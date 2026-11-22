
package net.ankan.budget.ai.service.impl;

import lombok.RequiredArgsConstructor;
import net.ankan.budget.ai.dto.FinancialContextDto;
import net.ankan.budget.ai.dto.GoalAnalysisRequestDto;
import net.ankan.budget.ai.dto.GoalFeasibilityDto;
import net.ankan.budget.dto.MonthlyFinancialSummaryDto;
import net.ankan.budget.ai.service.FinancialContextService;
import net.ankan.budget.ai.service.GoalAnalysisService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalAnalysisServiceImpl implements GoalAnalysisService {

    private final FinancialContextService financialContextService;

    @Override
    public GoalFeasibilityDto analyzeGoal(GoalAnalysisRequestDto request) {

        // 1. Fetch authenticated user's financial context
        FinancialContextDto context =
                financialContextService.getCurrentMonthContext();

        LocalDate today = LocalDate.now();

        // 2. Read goal information
        BigDecimal targetAmount = request.getTargetAmount();

        BigDecimal currentSavedAmount =
                request.getCurrentSavedAmount() != null
                        ? request.getCurrentSavedAmount()
                        : BigDecimal.ZERO;

        // 3. Calculate remaining amount
        BigDecimal remainingAmount = targetAmount
                .subtract(currentSavedAmount)
                .max(BigDecimal.ZERO);

        // 4. Calculate remaining days
        long remainingDays = ChronoUnit.DAYS.between(
                today,
                request.getTargetDate()
        );

        // 5. Calculate average monthly surplus from historical data
        List<MonthlyFinancialSummaryDto> history =
                context.getMonthlyFinancialHistory();

        long monthsWithRecordedActivity = history.stream()
                .filter(month ->
                        month.getTotalIncome().compareTo(BigDecimal.ZERO) > 0
                                || month.getTotalExpense().compareTo(BigDecimal.ZERO) > 0)
                .count();

        BigDecimal averageMonthlySurplus = null;

        // Require all three historical months to contain recorded activity
        if (history.size() == 3 && monthsWithRecordedActivity == 3) {

            BigDecimal totalSurplus = history.stream()
                    .map(month ->
                            month.getTotalIncome()
                                    .subtract(month.getTotalExpense()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            averageMonthlySurplus = totalSurplus.divide(
                    BigDecimal.valueOf(3),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        // 6. Handle already completed goal
        if (remainingAmount.compareTo(BigDecimal.ZERO) == 0) {

            return createResult(
                    request,
                    currentSavedAmount,
                    remainingAmount,
                    remainingDays,
                    averageMonthlySurplus,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    "ACHIEVABLE",
                    "The target amount has already been saved."
            );
        }

        // 7. Handle insufficient historical data
        if (averageMonthlySurplus == null) {

            return createResult(
                    request,
                    currentSavedAmount,
                    remainingAmount,
                    remainingDays,
                    null,
                    BigDecimal.ZERO,
                    null,
                    "INSUFFICIENT_DATA",
                    "Three months of recorded financial activity are required to assess goal affordability reliably."
            );
        }

        // 8. Calculate required monthly savings using calendar months
        long fullMonths = ChronoUnit.MONTHS.between(
                today,
                request.getTargetDate()
        );

        LocalDate nextMonthDate = today.plusMonths(fullMonths);

        BigDecimal remainingMonths = BigDecimal.valueOf(fullMonths);

        if (nextMonthDate.isBefore(request.getTargetDate())) {
            remainingMonths = remainingMonths.add(BigDecimal.ONE);
        }

        // Prevent division by zero
        if (remainingMonths.compareTo(BigDecimal.ZERO) <= 0) {
            remainingMonths = BigDecimal.ONE;
        }

        BigDecimal requiredMonthlySavings = remainingAmount.divide(
                remainingMonths,
                2,
                RoundingMode.HALF_UP
        );

        // 9. Calculate affordability gap
        BigDecimal monthlyAffordabilityGap =
                requiredMonthlySavings.subtract(averageMonthlySurplus);

        // 10. Determine goal feasibility
        String feasibilityStatus;
        String explanation;

        if (averageMonthlySurplus.compareTo(BigDecimal.ZERO) <= 0) {

            feasibilityStatus = "NOT_ACHIEVABLE";

            explanation =
                    "The historical financial records do not show a positive average monthly surplus.";

        } else if (requiredMonthlySavings.compareTo(
                averageMonthlySurplus) <= 0) {

            feasibilityStatus = "ACHIEVABLE";

            explanation =
                    "The required monthly savings are within the average monthly surplus recorded over the last three completed months.";

        } else {

            feasibilityStatus = "NOT_ACHIEVABLE";

            explanation =
                    "The required monthly savings exceed the average monthly surplus recorded over the last three completed months.";
        }

        // 11. Return calculated goal analysis
        return createResult(
                request,
                currentSavedAmount,
                remainingAmount,
                remainingDays,
                averageMonthlySurplus,
                requiredMonthlySavings,
                monthlyAffordabilityGap,
                feasibilityStatus,
                explanation
        );
    }

    // Create the goal feasibility response
    private GoalFeasibilityDto createResult(
            GoalAnalysisRequestDto request,
            BigDecimal currentSavedAmount,
            BigDecimal remainingAmount,
            long remainingDays,
            BigDecimal recordedMonthlySurplus,
            BigDecimal requiredMonthlySavings,
            BigDecimal monthlyAffordabilityGap,
            String feasibilityStatus,
            String feasibilityExplanation) {

        return new GoalFeasibilityDto(
                request.getGoalDescription(),
                request.getTargetAmount(),
                currentSavedAmount,
                remainingAmount,
                request.getTargetDate(),
                remainingDays,
                requiredMonthlySavings,
                recordedMonthlySurplus,
                monthlyAffordabilityGap,
                feasibilityStatus,
                feasibilityExplanation,
                null
        );
    }
}
