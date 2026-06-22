package net.ankan.budget.service.impl;

import lombok.RequiredArgsConstructor;
import net.ankan.budget.dto.BudgetDto;
import net.ankan.budget.dto.BudgetStatusDto;
import net.ankan.budget.entity.*;
import net.ankan.budget.exception.ResourceNotFoundException;
import net.ankan.budget.mapper.BudgetMapper;
import net.ankan.budget.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetServiceImpl {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BudgetMapper budgetMapper;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public List<BudgetDto> getAll(Integer month, Integer year) {
        User user = getCurrentUser();
        List<Budget> budgets = (month != null && year != null)
                ? budgetRepository.findByUserAndMonthAndYear(user, month, year)
                : budgetRepository.findByUser(user);
        return budgets.stream().map(budgetMapper::toDto).toList();
    }

    @Transactional
    public BudgetDto create(BudgetDto dto) {
        User user = getCurrentUser();
        if (budgetRepository.existsByUserAndCategoryIdAndMonthAndYear(
                user, dto.getCategoryId(), dto.getMonth(), dto.getYear())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A budget for this category and month already exists.");
        }
        Category category = categoryRepository.findByIdAndUser(dto.getCategoryId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (category.getType() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Budgets can only be set for EXPENSE categories.");
        }
        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .monthlyLimit(dto.getMonthlyLimit())
                .month(dto.getMonth())
                .year(dto.getYear())
                .build();
        return budgetMapper.toDto(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetDto update(Long id, BudgetDto dto) {
        User user = getCurrentUser();
        Budget budget = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
        budget.setMonthlyLimit(dto.getMonthlyLimit());
        return budgetMapper.toDto(budgetRepository.save(budget));
    }

    @Transactional
    public void delete(Long id) {
        User user = getCurrentUser();
        Budget budget = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
        budgetRepository.delete(budget);
    }

    public List<BudgetStatusDto> getStatus(int month, int year) {
        User user = getCurrentUser();
        return budgetRepository.findByUserAndMonthAndYear(user, month, year).stream()
                .map(budget -> {
                    // Access category within transaction — no lazy load issue
                    String categoryName = budget.getCategory().getName();
                    Long categoryId    = budget.getCategory().getId();

                    BigDecimal spent = transactionRepository
                            .sumByUserAndCategoryAndMonthAndYear(user, budget.getCategory(), month, year);
                    if (spent == null) spent = BigDecimal.ZERO;

                    BigDecimal remaining = budget.getMonthlyLimit().subtract(spent);
                    boolean isOver = remaining.compareTo(BigDecimal.ZERO) < 0;

                    double percentage = budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) == 0 ? 0 :
                            spent.divide(budget.getMonthlyLimit(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();

                    return new BudgetStatusDto(
                            budget.getId(),
                            categoryId,
                            categoryName,
                            budget.getMonthlyLimit(),
                            spent,
                            remaining,
                            isOver,
                            percentage
                    );
                }).toList();
    }
}