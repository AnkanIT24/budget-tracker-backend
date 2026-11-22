
package net.ankan.budget.ai.anomaly.controller;

import lombok.RequiredArgsConstructor;
import net.ankan.budget.ai.anomaly.dto.AnomalyResponseDto;
import net.ankan.budget.ai.anomaly.service.AnomalyDetectionService;
import net.ankan.budget.ai.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyDetectionService anomalyDetectionService;
    private final AiService aiService;

    // Detect unusual financial activity and generate personalized AI analysis
    @GetMapping
    public ResponseEntity<AnomalyResponseDto> detectAnomalies() {

        // Step 1: Detect anomalies using Java
        AnomalyResponseDto response =
                anomalyDetectionService.detectAnomalies();

        // Step 2: Generate personalized explanations using Gemini
        response = aiService.personalizeAnomalies(response);

        return ResponseEntity.ok(response);
    }
}
