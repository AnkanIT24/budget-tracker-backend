package net.ankan.budget.service.impl;

import lombok.RequiredArgsConstructor;
import net.ankan.budget.dto.CategoryDto;
import net.ankan.budget.entity.Category;
import net.ankan.budget.entity.TransactionType;
import net.ankan.budget.entity.User;
import net.ankan.budget.exception.ResourceNotFoundException;
import net.ankan.budget.mapper.CategoryMapper;
import net.ankan.budget.repository.BudgetRepository;
import net.ankan.budget.repository.CategoryRepository;
import net.ankan.budget.repository.TransactionRepository;
import net.ankan.budget.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public List<CategoryDto> getAll(TransactionType type) {
        User user = getCurrentUser();
        List<Category> categories = (type != null)
                ? categoryRepository.findByUserAndType(user, type)
                : categoryRepository.findByUser(user);
        return categories.stream().map(categoryMapper::toDto).toList();
    }

    public CategoryDto create(CategoryDto dto) {
        User user = getCurrentUser();
        Category category = categoryMapper.toEntity(dto);
        category.setUser(user);
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    public CategoryDto update(Long id, CategoryDto dto) {
        User user = getCurrentUser();
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        category.setName(dto.getName());
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        User user = getCurrentUser();
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Check transactions
        if (transactionRepository.existsByCategory(category)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete \"" + category.getName() + "\" — it has existing transactions. Delete those first.");
        }

        // Check budgets — this was the missing check causing the FK violation
        if (budgetRepository.existsByCategory(category)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete \"" + category.getName() + "\" — it has a budget linked to it. Remove the budget first.");
        }

        categoryRepository.delete(category);
    }
}