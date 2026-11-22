
package net.ankan.budget.ai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.ankan.budget.ai.validation.ValidGoalAmount;

import java.math.BigDecimal;
import java.time.LocalDate;

@ValidGoalAmount
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalAnalysisRequestDto {

    @NotBlank(message = "Goal description is required")
    private String goalDescription;

    @NotNull(message = "Target amount is required")
    @DecimalMin(value = "0.01", message = "Target amount must be greater than zero")
    private BigDecimal targetAmount;

    @NotNull(message = "Target date is required")
    @Future(message = "Target date must be in the future")
    private LocalDate targetDate;

    @DecimalMin(value = "0.00", message = "Current saved amount cannot be negative")
    private BigDecimal currentSavedAmount;
}
