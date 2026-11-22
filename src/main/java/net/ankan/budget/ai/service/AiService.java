
package net.ankan.budget.ai.service;

import net.ankan.budget.ai.dto.AiChatRequest;
import net.ankan.budget.ai.dto.AiChatResponse;
import net.ankan.budget.ai.dto.GoalAnalysisRequestDto;
import net.ankan.budget.ai.dto.GoalFeasibilityDto;
import net.ankan.budget.ai.recommendation.dto.RecommendationResponseDto;
import net.ankan.budget.ai.anomaly.dto.AnomalyResponseDto;

public interface AiService {

    // Existing financial chat functionality
    AiChatResponse chat(AiChatRequest request);

    // Financial goal analysis
    GoalFeasibilityDto analyzeGoal(GoalAnalysisRequestDto request);

    // Gemini-powered financial recommendation personalization
    RecommendationResponseDto personalizeRecommendations(
            RecommendationResponseDto recommendationResponse
    );

    // Gemini-powered anomaly analysis
    AnomalyResponseDto personalizeAnomalies(
            AnomalyResponseDto anomalyResponse
    );
}
