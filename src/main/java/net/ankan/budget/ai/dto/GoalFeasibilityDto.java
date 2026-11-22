
package net.ankan.budget.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalFeasibilityDto {

    private String goalDescription;

    private BigDecimal targetAmount;

    private BigDecimal currentSavedAmount;

    private BigDecimal remainingAmount;

    private LocalDate targetDate;

    private long remainingDays;

    private BigDecimal requiredMonthlySavings;

    private BigDecimal recordedMonthlySurplus;

    private BigDecimal monthlyAffordabilityGap;

    private String feasibilityStatus;

    private String feasibilityExplanation;

    private String personalizedAnalysis;
}
