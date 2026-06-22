package net.ankan.budget.mapper;

import net.ankan.budget.dto.BudgetDto;
import net.ankan.budget.entity.Budget;
import org.springframework.stereotype.Component;

@Component
public class BudgetMapper {

    public BudgetDto toDto(Budget budget) {
        BudgetDto dto = new BudgetDto();
        dto.setId(budget.getId());
        dto.setCategoryId(budget.getCategory().getId());
        dto.setCategoryName(budget.getCategory().getName());
        dto.setMonthlyLimit(budget.getMonthlyLimit());
        dto.setMonth(budget.getMonth());
        dto.setYear(budget.getYear());
        return dto;
    }
}
