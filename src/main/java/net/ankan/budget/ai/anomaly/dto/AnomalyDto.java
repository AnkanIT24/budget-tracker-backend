
package net.ankan.budget.ai.anomaly.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.ankan.budget.ai.anomaly.enums.AnomalySeverity;
import net.ankan.budget.ai.anomaly.enums.AnomalyType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDto {

    private Long transactionId;

    private String categoryName;

    private BigDecimal amount;

    private LocalDate transactionDate;

    private AnomalyType anomalyType;

    private AnomalySeverity severity;

    private String description;

    private String suggestedAction;
}
