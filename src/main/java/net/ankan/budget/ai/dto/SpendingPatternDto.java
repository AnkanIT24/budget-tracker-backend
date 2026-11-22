
package net.ankan.budget.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpendingPatternDto {

    private BigDecimal totalExpenses;

    private String highestSpendingCategory;

    private BigDecimal highestCategoryAmount;

    private BigDecimal highestCategoryPercentage;

    private int categoriesWithSpending;

    private String spendingPattern;
}
