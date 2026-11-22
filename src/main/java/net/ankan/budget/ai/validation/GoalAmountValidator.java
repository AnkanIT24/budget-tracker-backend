
package net.ankan.budget.ai.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.ankan.budget.ai.dto.GoalAnalysisRequestDto;

import java.math.BigDecimal;

public class GoalAmountValidator
        implements ConstraintValidator<ValidGoalAmount, GoalAnalysisRequestDto> {

    @Override
    public boolean isValid(
            GoalAnalysisRequestDto request,
            ConstraintValidatorContext context) {

        if (request == null) {
            return true;
        }

        BigDecimal targetAmount = request.getTargetAmount();
        BigDecimal currentSavedAmount = request.getCurrentSavedAmount();

        // Let the existing @NotNull validations handle missing values.
        if (targetAmount == null || currentSavedAmount == null) {
            return true;
        }

        return currentSavedAmount.compareTo(targetAmount) <= 0;
    }
}
