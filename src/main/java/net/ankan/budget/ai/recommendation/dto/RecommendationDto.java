
package net.ankan.budget.ai.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.ankan.budget.ai.recommendation.enums.RecommendationPriority;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDto {

    private String title;

    private String description;

    private String categoryName;

    private RecommendationPriority priority;

    private BigDecimal currentAmount;

    private BigDecimal suggestedAmount;

    private BigDecimal potentialSavings;
}
