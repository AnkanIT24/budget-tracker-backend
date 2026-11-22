
package net.ankan.budget.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorySpendingDto {

    private Long categoryId;

    private String categoryName;

    private BigDecimal totalSpent;

    private double percentageOfTotal;
}
