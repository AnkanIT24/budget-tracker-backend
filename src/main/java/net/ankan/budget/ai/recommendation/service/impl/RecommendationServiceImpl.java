
package net.ankan.budget.ai.recommendation.service.impl;

import lombok.RequiredArgsConstructor;
import net.ankan.budget.ai.dto.BudgetRiskDto;
import net.ankan.budget.ai.dto.FinancialContextDto;
import net.ankan.budget.ai.recommendation.dto.RecommendationDto;
import net.ankan.budget.ai.recommendation.dto.RecommendationResponseDto;
import net.ankan.budget.ai.recommendation.enums.RecommendationPriority;
import net.ankan.budget.ai.recommendation.service.RecommendationService;
import net.ankan.budget.ai.service.FinancialContextService;
import net.ankan.budget.ai.dto.BudgetRiskDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final FinancialContextService financialContextService;

    @Override
    public RecommendationResponseDto generateRecommendations() {

        // 1. Fetch authenticated user's financial context
        FinancialContextDto context =
                financialContextService.getCurrentMonthContext();

        List<RecommendationDto> recommendations = new ArrayList<>();

        // 2. Analyze budget risks
        generateBudgetRecommendations(
                context.getBudgetRisks(),
                recommendations
        );

        // 3. Count recommendations by priority
        int highPriorityCount = (int) recommendations.stream()
                .filter(r -> r.getPriority() == RecommendationPriority.HIGH)
                .count();

        int mediumPriorityCount = (int) recommendations.stream()
                .filter(r -> r.getPriority() == RecommendationPriority.MEDIUM)
                .count();

        int lowPriorityCount = (int) recommendations.stream()
                .filter(r -> r.getPriority() == RecommendationPriority.LOW)
                .count();

        // 4. Generate overall summary
        String overallSummary;

        if (recommendations.isEmpty()) {
            overallSummary =
                    "No immediate budget risks were detected in the recorded financial data.";
        } else {
            overallSummary =
                    "Found " + recommendations.size()
                            + " financial recommendations, including "
                            + highPriorityCount + " high-priority and "
                            + mediumPriorityCount + " medium-priority items.";
        }

        // 5. Return structured recommendations
        return new RecommendationResponseDto(
                recommendations,
                recommendations.size(),
                highPriorityCount,
                mediumPriorityCount,
                lowPriorityCount,
                overallSummary,
                null
        );
    }

    // Generate recommendations from budget risk data
    private void generateBudgetRecommendations(
            List<BudgetRiskDto> budgetRisks,
            List<RecommendationDto> recommendations) {

        for (BudgetRiskDto budget : budgetRisks) {

            BigDecimal spent = budget.getSpent();
            BigDecimal monthlyLimit = budget.getMonthlyLimit();
            BigDecimal remaining = budget.getRemaining();

            String riskLevel = budget.getRiskLevel();

            // HIGH: Category has exceeded its budget
            if ("OVER_BUDGET".equals(riskLevel)) {

                BigDecimal overspending = spent.subtract(monthlyLimit);

                recommendations.add(new RecommendationDto(
                        "Budget exceeded: " + budget.getCategoryName(),

                        "Your " + budget.getCategoryName()
                                + " spending has exceeded its monthly budget by ₹"
                                + overspending
                                + ". Reduce spending in this category to return within the allocated limit.",

                        budget.getCategoryName(),

                        RecommendationPriority.HIGH,

                        spent,

                        monthlyLimit,

                        overspending
                ));
            }

            // MEDIUM: Category has used at least 80% of its budget
            else if ("HIGH".equals(riskLevel)) {

                recommendations.add(new RecommendationDto(
                        "Budget warning: " + budget.getCategoryName(),

                        "You have used "
                                + budget.getPercentageUsed()
                                + "% of your "
                                + budget.getCategoryName()
                                + " budget. Only ₹"
                                + remaining
                                + " remains for this month.",

                        budget.getCategoryName(),

                        RecommendationPriority.MEDIUM,

                        spent,

                        remaining,

                        BigDecimal.ZERO
                ));
            }

            // LOW: Category has used at least 50% of its budget
            else if ("MODERATE".equals(riskLevel)) {

                recommendations.add(new RecommendationDto(
                        "Monitor spending: " + budget.getCategoryName(),

                        "Your " + budget.getCategoryName()
                                + " budget is "
                                + budget.getPercentageUsed()
                                + "% utilized. Monitor upcoming expenses to avoid overspending.",

                        budget.getCategoryName(),

                        RecommendationPriority.LOW,

                        spent,

                        remaining,

                        BigDecimal.ZERO
                ));
            }
        }
    }
}
