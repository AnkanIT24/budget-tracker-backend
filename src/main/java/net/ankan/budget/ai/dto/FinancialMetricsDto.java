
package net.ankan.budget.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialMetricsDto {

    private BigDecimal recordedSurplus;
    private BigDecimal recordedSurplusRate;
    private BigDecimal expenseToIncomeRatio;
    private BigDecimal incomeToExpenseRatio;
    private BigDecimal budgetUtilization;
}
