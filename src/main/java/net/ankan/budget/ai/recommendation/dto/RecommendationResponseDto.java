
package net.ankan.budget.ai.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDto {

    private List<RecommendationDto> recommendations;

    private int totalRecommendations;

    private int highPriorityCount;

    private int mediumPriorityCount;

    private int lowPriorityCount;

    private String overallSummary;

    private String personalizedAnalysis;
}
