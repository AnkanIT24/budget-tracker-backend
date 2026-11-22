
package net.ankan.budget.ai.service;

import net.ankan.budget.ai.dto.GoalAnalysisRequestDto;
import net.ankan.budget.ai.dto.GoalFeasibilityDto;

public interface GoalAnalysisService {

    GoalFeasibilityDto analyzeGoal(GoalAnalysisRequestDto request);
}
