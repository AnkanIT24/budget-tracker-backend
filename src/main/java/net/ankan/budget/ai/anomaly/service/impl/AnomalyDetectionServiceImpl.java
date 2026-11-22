
package net.ankan.budget.ai.anomaly.service.impl;

import lombok.RequiredArgsConstructor;
import net.ankan.budget.ai.anomaly.dto.AnomalyDto;
import net.ankan.budget.ai.anomaly.dto.AnomalyResponseDto;
import net.ankan.budget.ai.anomaly.enums.AnomalySeverity;
import net.ankan.budget.ai.anomaly.enums.AnomalyType;
import net.ankan.budget.ai.anomaly.service.AnomalyDetectionService;
import net.ankan.budget.entity.Transaction;
import net.ankan.budget.entity.TransactionType;
import net.ankan.budget.entity.User;
import net.ankan.budget.repository.TransactionRepository;
import net.ankan.budget.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private static final BigDecimal LARGE_EXPENSE_MULTIPLIER =
            new BigDecimal("3");

    private static final BigDecimal CATEGORY_SPIKE_MULTIPLIER =
            new BigDecimal("2");

    private static final int MINIMUM_HISTORICAL_TRANSACTIONS = 5;

    // Separate minimum history for unusual new-category detection
    private static final int MINIMUM_HISTORY_FOR_NEW_CATEGORY = 3;

    @Override
    @Transactional(readOnly = true)
    public AnomalyResponseDto detectAnomalies() {

        // 1. Identify the authenticated user
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Authenticated user not found"));

        // 2. Fetch only this user's transactions
        List<Transaction> transactions =
                transactionRepository.findByUserOrderByDateDesc(user);

        List<AnomalyDto> anomalies = new ArrayList<>();

        // 3. Consider expense transactions only
        List<Transaction> expenses = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .toList();

        // 4. Detect unusual activity in previously unused categories first.
        // Keep track of these transactions so they are not incorrectly
        // classified as generic large expenses.
        Set<Long> unusualCategoryTransactionIds =
                detectUnusualCategoryActivity(expenses, anomalies);

        // 5. Run the remaining anomaly detection rules
        detectUnusuallyLargeExpenses(
                expenses,
                anomalies,
                unusualCategoryTransactionIds
        );

        detectCategorySpendingSpikes(expenses, anomalies);

        detectPossibleDuplicateTransactions(expenses, anomalies);

        // 6. Sort anomalies by severity and transaction date
        anomalies.sort(
                Comparator.comparing(AnomalyDto::getSeverity)
                        .reversed()
                        .thenComparing(
                                AnomalyDto::getTransactionDate,
                                Comparator.reverseOrder()
                        )
        );

        // 7. Calculate severity counts
        int highCount = countBySeverity(
                anomalies, AnomalySeverity.HIGH);

        int mediumCount = countBySeverity(
                anomalies, AnomalySeverity.MEDIUM);

        int lowCount = countBySeverity(
                anomalies, AnomalySeverity.LOW);

        // 8. Generate summary
        String summary;

        if (anomalies.isEmpty()) {
            summary =
                    "No unusual expense patterns were detected "
                            + "in the available transaction history.";
        } else {
            summary =
                    "Detected " + anomalies.size()
                            + " potential financial anomalies, including "
                            + highCount + " high-severity, "
                            + mediumCount + " medium-severity and "
                            + lowCount + " low-severity findings.";
        }

        return new AnomalyResponseDto(
                anomalies,
                anomalies.size(),
                highCount,
                mediumCount,
                lowCount,
                summary,
                null
        );
    }

    // RULE 1: Detect unusual activity in previously unused categories
    private Set<Long> detectUnusualCategoryActivity(
            List<Transaction> expenses,
            List<AnomalyDto> anomalies) {

        Set<Long> unusualCategoryTransactionIds = new HashSet<>();

        LocalDate currentMonthStart =
                LocalDate.now().withDayOfMonth(1);

        // Historical expenses before the current month
        List<Transaction> previousExpenses = expenses.stream()
                .filter(t -> t.getDate().isBefore(currentMonthStart))
                .toList();

        // Require at least 3 historical transactions for this rule
        if (previousExpenses.size() < MINIMUM_HISTORY_FOR_NEW_CATEGORY) {
            return unusualCategoryTransactionIds;
        }

        List<BigDecimal> historicalAmounts = previousExpenses.stream()
                .map(Transaction::getAmount)
                .sorted()
                .toList();

        BigDecimal median = calculateMedian(historicalAmounts);

        if (median.compareTo(BigDecimal.ZERO) <= 0) {
            return unusualCategoryTransactionIds;
        }

        BigDecimal threshold =
                median.multiply(LARGE_EXPENSE_MULTIPLIER);

        // Check current-month expenses
        for (Transaction transaction : expenses) {

            if (transaction.getDate().isBefore(currentMonthStart)) {
                continue;
            }

            Long categoryId = transaction.getCategory().getId();

            boolean categoryPreviouslyUsed =
                    previousExpenses.stream()
                            .anyMatch(t ->
                                    t.getCategory().getId().equals(categoryId));

            // Skip categories that already have historical activity
            if (categoryPreviouslyUsed) {
                continue;
            }

            // Only classify significant expenses in new categories
            if (transaction.getAmount().compareTo(threshold) >= 0) {

                unusualCategoryTransactionIds.add(transaction.getId());

                anomalies.add(new AnomalyDto(
                        transaction.getId(),
                        transaction.getCategory().getName(),
                        transaction.getAmount(),
                        transaction.getDate(),
                        AnomalyType.UNUSUAL_CATEGORY_ACTIVITY,
                        AnomalySeverity.MEDIUM,
                        "A significant expense has been recorded in a category "
                                + "with no previous recorded expense activity. "
                                + "The amount is substantially higher than "
                                + "your historical median expense.",
                        "Verify the transaction category and check whether "
                                + "this is a new or unexpected expense."
                ));
            }
        }

        return unusualCategoryTransactionIds;
    }

    // RULE 2: Detect unusually large expenses
    private void detectUnusuallyLargeExpenses(
            List<Transaction> expenses,
            List<AnomalyDto> anomalies,
            Set<Long> unusualCategoryTransactionIds) {

        for (Transaction transaction : expenses) {

            // Do not classify a new-category transaction again
            // as a generic large expense.
            if (unusualCategoryTransactionIds.contains(transaction.getId())) {
                continue;
            }

            Long categoryId = transaction.getCategory().getId();

            // Compare only against earlier expenses in the same category
            List<BigDecimal> historicalAmounts = expenses.stream()
                    .filter(t -> t.getCategory().getId().equals(categoryId))
                    .filter(t -> t.getDate().isBefore(transaction.getDate()))
                    .map(Transaction::getAmount)
                    .sorted()
                    .toList();

            if (historicalAmounts.size()
                    < MINIMUM_HISTORICAL_TRANSACTIONS) {
                continue;
            }

            BigDecimal median = calculateMedian(historicalAmounts);

            if (median.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal threshold =
                    median.multiply(LARGE_EXPENSE_MULTIPLIER);

            if (transaction.getAmount().compareTo(threshold) >= 0) {

                BigDecimal ratio = transaction.getAmount()
                        .divide(median, 2, RoundingMode.HALF_UP);

                anomalies.add(new AnomalyDto(
                        transaction.getId(),
                        transaction.getCategory().getName(),
                        transaction.getAmount(),
                        transaction.getDate(),
                        AnomalyType.UNUSUALLY_LARGE_EXPENSE,
                        AnomalySeverity.HIGH,
                        "This expense is approximately "
                                + ratio
                                + " times the median of your earlier "
                                + "recorded expenses in this category.",
                        "Review this transaction and verify that the amount "
                                + "and category are correct."
                ));
            }
        }
    }

    // RULE 3: Detect category spending spikes
    private void detectCategorySpendingSpikes(
            List<Transaction> expenses,
            List<AnomalyDto> anomalies) {

        LocalDate currentDate = LocalDate.now();

        LocalDate currentMonthStart =
                currentDate.withDayOfMonth(1);

        LocalDate previousPeriodStart =
                currentMonthStart.minusMonths(3);

        Map<Long, BigDecimal> currentMonthSpending =
                calculateCategorySpending(
                        expenses,
                        currentMonthStart,
                        currentDate.plusDays(1)
                );

        Map<Long, Map<LocalDate, BigDecimal>> historicalMonthlySpending =
                new HashMap<>();

        for (Transaction transaction : expenses) {

            LocalDate transactionDate = transaction.getDate();

            if (transactionDate.isBefore(previousPeriodStart)
                    || !transactionDate.isBefore(currentMonthStart)) {
                continue;
            }

            Long categoryId = transaction.getCategory().getId();

            LocalDate month =
                    transactionDate.withDayOfMonth(1);

            historicalMonthlySpending
                    .computeIfAbsent(categoryId, key -> new HashMap<>())
                    .merge(
                            month,
                            transaction.getAmount(),
                            BigDecimal::add
                    );
        }

        for (Map.Entry<Long, BigDecimal> entry
                : currentMonthSpending.entrySet()) {

            Long categoryId = entry.getKey();
            BigDecimal currentSpending = entry.getValue();

            Map<LocalDate, BigDecimal> monthlyHistory =
                    historicalMonthlySpending.get(categoryId);

            if (monthlyHistory == null || monthlyHistory.size() < 2) {
                continue;
            }

            BigDecimal historicalTotal = monthlyHistory.values()
                    .stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal averageMonthlySpending =
                    historicalTotal.divide(
                            BigDecimal.valueOf(monthlyHistory.size()),
                            2,
                            RoundingMode.HALF_UP
                    );

            if (averageMonthlySpending.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal threshold =
                    averageMonthlySpending.multiply(
                            CATEGORY_SPIKE_MULTIPLIER
                    );

            if (currentSpending.compareTo(threshold) >= 0) {

                Optional<Transaction> latestTransaction =
                        expenses.stream()
                                .filter(t ->
                                        t.getCategory().getId().equals(categoryId))
                                .filter(t ->
                                        !t.getDate().isBefore(currentMonthStart))
                                .max(Comparator.comparing(Transaction::getDate));

                latestTransaction.ifPresent(transaction ->
                        anomalies.add(new AnomalyDto(
                                transaction.getId(),
                                transaction.getCategory().getName(),
                                currentSpending,
                                transaction.getDate(),
                                AnomalyType.CATEGORY_SPENDING_SPIKE,
                                AnomalySeverity.HIGH,
                                "Recorded spending in this category this month "
                                        + "has reached at least twice its average "
                                        + "monthly spending across the available "
                                        + "historical months.",
                                "Review recent expenses in this category "
                                        + "and check whether the increased "
                                        + "spending is expected."
                        ))
                );
            }
        }
    }

    // RULE 4: Detect possible duplicate transactions
    private void detectPossibleDuplicateTransactions(
            List<Transaction> expenses,
            List<AnomalyDto> anomalies) {

        Map<String, List<Transaction>> groupedTransactions =
                expenses.stream()
                        .collect(Collectors.groupingBy(t ->
                                t.getCategory().getId()
                                        + "|"
                                        + t.getAmount()
                                        + "|"
                                        + t.getDate()
                        ));

        for (List<Transaction> group : groupedTransactions.values()) {

            if (group.size() < 2) {
                continue;
            }

            // Keep the first transaction as the original.
            // Flag subsequent matching transactions for review.
            group.sort(Comparator.comparing(Transaction::getId));

            for (int i = 1; i < group.size(); i++) {

                Transaction duplicate = group.get(i);

                anomalies.add(new AnomalyDto(
                        duplicate.getId(),
                        duplicate.getCategory().getName(),
                        duplicate.getAmount(),
                        duplicate.getDate(),
                        AnomalyType.POSSIBLE_DUPLICATE_TRANSACTION,
                        AnomalySeverity.MEDIUM,
                        "Another expense with the same amount, category "
                                + "and transaction date is recorded.",
                        "Review both transactions to confirm whether "
                                + "the expense was entered more than once."
                ));
            }
        }
    }

    // Calculate category spending within a date range
    private Map<Long, BigDecimal> calculateCategorySpending(
            List<Transaction> expenses,
            LocalDate startDate,
            LocalDate endDate) {

        return expenses.stream()
                .filter(t -> !t.getDate().isBefore(startDate))
                .filter(t -> t.getDate().isBefore(endDate))
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getId(),
                        Collectors.mapping(
                                Transaction::getAmount,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        BigDecimal::add
                                )
                        )
                ));
    }

    // Calculate median of historical expense amounts
    private BigDecimal calculateMedian(List<BigDecimal> sortedAmounts) {

        int size = sortedAmounts.size();

        if (size == 0) {
            return BigDecimal.ZERO;
        }

        if (size % 2 == 1) {
            return sortedAmounts.get(size / 2);
        }

        BigDecimal firstMiddle = sortedAmounts.get(size / 2 - 1);
        BigDecimal secondMiddle = sortedAmounts.get(size / 2);

        return firstMiddle.add(secondMiddle)
                .divide(
                        BigDecimal.valueOf(2),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    // Count anomalies by severity
    private int countBySeverity(
            List<AnomalyDto> anomalies,
            AnomalySeverity severity) {

        return (int) anomalies.stream()
                .filter(a -> a.getSeverity() == severity)
                .count();
    }
}
