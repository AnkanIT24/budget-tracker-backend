
package net.ankan.budget.ai.anomaly.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyResponseDto {

    private List<AnomalyDto> anomalies;

    private int totalAnomalies;

    private int highSeverityCount;

    private int mediumSeverityCount;

    private int lowSeverityCount;

    private String overallSummary;

    private String personalizedAnalysis;
}
