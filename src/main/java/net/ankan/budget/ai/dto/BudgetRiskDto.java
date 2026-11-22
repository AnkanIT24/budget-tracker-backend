
package net.ankan.budget.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.ankan.budget.ai.dto.BudgetRiskDto;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetRiskDto {

    private Long categoryId;

    private String categoryName;

    private BigDecimal monthlyLimit;

    private BigDecimal spent;

    private BigDecimal remaining;

    private double percentageUsed;

    private String riskLevel;
}
