package net.ankan.budget.mapper;

import net.ankan.budget.dto.CategoryDto;
import net.ankan.budget.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryDto toDto(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getType());
    }

    public Category toEntity(CategoryDto dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setType(dto.getType());
        return category;
    }
}
