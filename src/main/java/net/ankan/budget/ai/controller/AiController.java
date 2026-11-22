
package net.ankan.budget.ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.ankan.budget.ai.dto.AiChatRequest;
import net.ankan.budget.ai.dto.AiChatResponse;
import net.ankan.budget.ai.dto.FinancialContextDto;
import net.ankan.budget.ai.dto.GoalAnalysisRequestDto;
import net.ankan.budget.ai.dto.GoalFeasibilityDto;
import net.ankan.budget.ai.recommendation.dto.RecommendationResponseDto;
import net.ankan.budget.ai.recommendation.service.RecommendationService;
import net.ankan.budget.ai.service.AiService;
import net.ankan.budget.ai.service.FinancialContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final FinancialContextService financialContextService;
    private final RecommendationService recommendationService;

    // Existing AI chat endpoint
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @RequestBody AiChatRequest request) {

        AiChatResponse response = aiService.chat(request);

        return ResponseEntity.ok(response);
    }

    // Personalized financial goal analysis
    @PostMapping("/analyze-goal")
    public ResponseEntity<GoalFeasibilityDto> analyzeGoal(
            @Valid @RequestBody GoalAnalysisRequestDto request) {

        GoalFeasibilityDto response =
                aiService.analyzeGoal(request);

        return ResponseEntity.ok(response);
    }

    // Get complete financial context for the authenticated user
    @GetMapping("/context")
    public ResponseEntity<FinancialContextDto> getFinancialContext() {

        FinancialContextDto context =
                financialContextService.getCurrentMonthContext();

        return ResponseEntity.ok(context);
    }

    // Generate and personalize financial recommendations
    @GetMapping("/recommendations")
    public ResponseEntity<RecommendationResponseDto> getRecommendations() {

        // Step 1: Generate recommendations using Java business rules
        RecommendationResponseDto response =
                recommendationService.generateRecommendations();

        // Step 2: Use Gemini to personalize the recommendations
        response = aiService.personalizeRecommendations(response);

        return ResponseEntity.ok(response);
    }
}
