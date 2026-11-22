
package net.ankan.budget.ai.service;

import lombok.RequiredArgsConstructor;
import net.ankan.budget.ai.dto.BudgetRiskDto;
import net.ankan.budget.ai.dto.CategorySpendingDto;
import net.ankan.budget.ai.dto.FinancialContextDto;
import net.ankan.budget.ai.dto.FinancialMetricsDto;
import net.ankan.budget.ai.dto.SpendingPatternDto;
import net.ankan.budget.dto.BudgetStatusDto;
import net.ankan.budget.dto.CategoryDto;
import net.ankan.budget.dto.MonthlyFinancialSummaryDto;
import net.ankan.budget.dto.TransactionDto;
import net.ankan.budget.dto.TransactionSummaryDto;
import net.ankan.budget.entity.User;
import net.ankan.budget.repository.TransactionRepository;
import net.ankan.budget.repository.UserRepository;
import net.ankan.budget.service.impl.BudgetServiceImpl;
import net.ankan.budget.service.impl.CategoryServiceImpl;
import net.ankan.budget.service.impl.TransactionServiceImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinancialContextService {

    private final TransactionServiceImpl transactionService;
    private final BudgetServiceImpl budgetService;
    private final CategoryServiceImpl categoryService;

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public FinancialContextDto getCurrentMonthContext() {

        LocalDate today = LocalDate.now();

        int month = today.getMonthValue();
        int year = today.getYear();

        // 1. Fetch monthly financial summary
        TransactionSummaryDto summary =
                transactionService.getSummary(month, year);

        // 2. Fetch monthly budget status
        List<BudgetStatusDto> budgetStatuses =
                budgetService.getStatus(month, year);

        // 3. Fetch categories belonging to the authenticated user
        List<CategoryDto> categories =
                categoryService.getAll(null);

        // 4. Fetch this month's transactions
        List<TransactionDto> transactions =
                transactionService.getAll(month, year, null, null);

        // 5. Fetch category-wise monthly spending
        List<CategorySpendingDto> categorySpending =
                getCategoryWiseSpending(month, year);

        // 6. Calculate financial metrics
        FinancialMetricsDto financialMetrics =
                calculateFinancialMetrics(summary, budgetStatuses);

        // 7. Calculate spending pattern
        SpendingPatternDto spendingPattern =
                calculateSpendingPattern(categorySpending);

        // 8. Calculate budget risks
        List<BudgetRiskDto> budgetRisks =
                calculateBudgetRisks(budgetStatuses);

        // 9. Fetch historical financial data
        List<MonthlyFinancialSummaryDto> monthlyFinancialHistory =
                getMonthlyFinancialHistory(today);

        // 10. Combine everything into one financial context
        return new FinancialContextDto(
                month,
                year,
                today,
                "INR",
                summary,
                categories,
                budgetStatuses,
                transactions,
                categorySpending,
                financialMetrics,
                spendingPattern,
                budgetRisks,
                monthlyFinancialHistory
        );
    }

    // Fetch the last three completed months of financial history
    public List<MonthlyFinancialSummaryDto> getMonthlyFinancialHistory(
            LocalDate today) {

        // Get authenticated user's email
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        // Retrieve authenticated user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Authenticated user not found"
                ));

        // Calculate the date range
        YearMonth currentMonth = YearMonth.from(today);

        YearMonth startMonth = currentMonth.minusMonths(3);

        LocalDate startDate = startMonth.atDay(1);
        LocalDate endDate = currentMonth.atDay(1);

        // Fetch historical financial records
        List<Object[]> results =
                transactionRepository.findMonthlyFinancialHistory(
                        user,
                        startDate,
                        endDate
                );

        // Convert query results into a map
        Map<YearMonth, MonthlyFinancialSummaryDto> historyMap =
                new HashMap<>();

        for (Object[] row : results) {

            int month = ((Number) row[0]).intValue();
            int year = ((Number) row[1]).intValue();

            BigDecimal income = row[2] != null
                    ? (BigDecimal) row[2]
                    : BigDecimal.ZERO;

            BigDecimal expenses = row[3] != null
                    ? (BigDecimal) row[3]
                    : BigDecimal.ZERO;

            YearMonth yearMonth = YearMonth.of(year, month);

            historyMap.put(
                    yearMonth,
                    new MonthlyFinancialSummaryDto(
                            month,
                            year,
                            income,
                            expenses
                    )
            );
        }

        // Ensure all three months are represented
        List<MonthlyFinancialSummaryDto> monthlyHistory =
                new ArrayList<>();

        for (int i = 0; i < 3; i++) {

            YearMonth yearMonth = startMonth.plusMonths(i);

            MonthlyFinancialSummaryDto summary =
                    historyMap.get(yearMonth);

            // If no transactions exist, use zero values
            if (summary == null) {

                summary = new MonthlyFinancialSummaryDto(
                        yearMonth.getMonthValue(),
                        yearMonth.getYear(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );
            }

            monthlyHistory.add(summary);
        }

        return monthlyHistory;
    }

    // Category-wise monthly expense analysis
    public List<CategorySpendingDto> getCategoryWiseSpending(
            int month, int year) {

        List<Object[]> results =
                transactionService.getCategoryWiseSpending(month, year);

        BigDecimal totalExpenses = results.stream()
                .map(row -> (BigDecimal) row[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategorySpendingDto> spendingList = new ArrayList<>();

        for (Object[] row : results) {

            Long categoryId = ((Number) row[0]).longValue();
            String categoryName = (String) row[1];
            BigDecimal totalSpent = (BigDecimal) row[2];

            double percentage = 0.0;

            if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                percentage = totalSpent
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalExpenses, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            spendingList.add(new CategorySpendingDto(
                    categoryId,
                    categoryName,
                    totalSpent,
                    percentage
            ));
        }

        return spendingList;
    }

    // Calculate financial health metrics
    private FinancialMetricsDto calculateFinancialMetrics(
            TransactionSummaryDto summary,
            List<BudgetStatusDto> budgetStatuses) {

        BigDecimal income = summary.getTotalIncome();
        BigDecimal expenses = summary.getTotalExpense();
        BigDecimal recordedSurplus = summary.getNetBalance();

        // Recorded surplus rate = surplus / income * 100
        BigDecimal recordedSurplusRate = BigDecimal.ZERO;

        if (income.compareTo(BigDecimal.ZERO) > 0) {
            recordedSurplusRate = recordedSurplus
                    .multiply(BigDecimal.valueOf(100))
                    .divide(income, 2, RoundingMode.HALF_UP);
        }

        // Expense-to-income ratio = expenses / income * 100
        BigDecimal expenseToIncomeRatio = BigDecimal.ZERO;

        if (income.compareTo(BigDecimal.ZERO) > 0) {
            expenseToIncomeRatio = expenses
                    .multiply(BigDecimal.valueOf(100))
                    .divide(income, 2, RoundingMode.HALF_UP);
        }

        // Income-to-expense ratio
        // Null means this ratio is not applicable.
        BigDecimal incomeToExpenseRatio = null;

        if (expenses.compareTo(BigDecimal.ZERO) > 0) {
            incomeToExpenseRatio = income.divide(
                    expenses, 2, RoundingMode.HALF_UP);
        }

        // Budget utilization
        // Null means no budgets are configured.
        BigDecimal budgetUtilization = null;

        if (!budgetStatuses.isEmpty()) {

            BigDecimal totalBudgetLimit = budgetStatuses.stream()
                    .map(BudgetStatusDto::getMonthlyLimit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalBudgetSpent = budgetStatuses.stream()
                    .map(BudgetStatusDto::getSpent)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalBudgetLimit.compareTo(BigDecimal.ZERO) > 0) {
                budgetUtilization = totalBudgetSpent
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalBudgetLimit, 2, RoundingMode.HALF_UP);
            }
        }

        return new FinancialMetricsDto(
                recordedSurplus,
                recordedSurplusRate,
                expenseToIncomeRatio,
                incomeToExpenseRatio,
                budgetUtilization
        );
    }

    // Detect monthly spending patterns
    public SpendingPatternDto calculateSpendingPattern(
            List<CategorySpendingDto> categorySpending) {

        // Calculate total recorded expenses
        BigDecimal totalExpenses = categorySpending.stream()
                .map(CategorySpendingDto::getTotalSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Identify categories with actual spending
        List<CategorySpendingDto> categoriesWithSpending =
                categorySpending.stream()
                        .filter(category ->
                                category.getTotalSpent()
                                        .compareTo(BigDecimal.ZERO) > 0)
                        .toList();

        // Handle the case where no expenses exist
        if (totalExpenses.compareTo(BigDecimal.ZERO) == 0) {
            return new SpendingPatternDto(
                    BigDecimal.ZERO,
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0,
                    "NO_SPENDING"
            );
        }

        // Find the highest-spending category
        CategorySpendingDto highestCategory =
                categoriesWithSpending.get(0);

        BigDecimal highestCategoryAmount =
                highestCategory.getTotalSpent();

        // Calculate the highest category's percentage
        BigDecimal highestCategoryPercentage =
                highestCategoryAmount
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalExpenses, 2, RoundingMode.HALF_UP);

        int categoryCount = categoriesWithSpending.size();

        // Classify the spending pattern
        String spendingPattern;

        if (categoryCount == 1) {
            spendingPattern = "SINGLE_CATEGORY";
        } else if (highestCategoryPercentage.compareTo(
                BigDecimal.valueOf(70)) >= 0) {
            spendingPattern = "HIGHLY_CONCENTRATED";
        } else if (highestCategoryPercentage.compareTo(
                BigDecimal.valueOf(40)) >= 0) {
            spendingPattern = "MODERATELY_CONCENTRATED";
        } else {
            spendingPattern = "DISTRIBUTED";
        }

        return new SpendingPatternDto(
                totalExpenses,
                highestCategory.getCategoryName(),
                highestCategoryAmount,
                highestCategoryPercentage,
                categoryCount,
                spendingPattern
        );
    }

    // Analyze monthly budget risks
    public List<BudgetRiskDto> calculateBudgetRisks(
            List<BudgetStatusDto> budgetStatuses) {

        return budgetStatuses.stream()
                .map(budget -> {

                    double percentageUsed = budget.getPercentageUsed();

                    String riskLevel;

                    if (percentageUsed > 100) {
                        riskLevel = "OVER_BUDGET";
                    } else if (percentageUsed >= 80) {
                        riskLevel = "HIGH";
                    } else if (percentageUsed >= 50) {
                        riskLevel = "MODERATE";
                    } else {
                        riskLevel = "LOW";
                    }

                    return new BudgetRiskDto(
                            budget.getCategoryId(),
                            budget.getCategoryName(),
                            budget.getMonthlyLimit(),
                            budget.getSpent(),
                            budget.getRemaining(),
                            percentageUsed,
                            riskLevel
                    );
                })
                .toList();
    }
}
