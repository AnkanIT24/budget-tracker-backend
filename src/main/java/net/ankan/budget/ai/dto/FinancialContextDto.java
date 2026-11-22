
package net.ankan.budget.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.ankan.budget.dto.BudgetStatusDto;
import net.ankan.budget.dto.CategoryDto;
import net.ankan.budget.dto.TransactionDto;
import net.ankan.budget.dto.TransactionSummaryDto;
import net.ankan.budget.dto.MonthlyFinancialSummaryDto;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialContextDto {

    private int month;
    private int year;
    private LocalDate generatedAt;
    private String currency;

    private TransactionSummaryDto summary;

    private List<CategoryDto> categories;
    private List<BudgetStatusDto> budgetStatuses;
    private List<TransactionDto> recentTransactions;

    private List<CategorySpendingDto> categorySpending;

    private FinancialMetricsDto financialMetrics;

    private SpendingPatternDto spendingPattern;

    // New: Budget risk analysis
    private List<BudgetRiskDto> budgetRisks;

    private List<MonthlyFinancialSummaryDto> monthlyFinancialHistory;
}
