package net.ankan.budget.repository;

import net.ankan.budget.entity.Budget;
import net.ankan.budget.entity.Category;
import net.ankan.budget.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUser(User user);
    List<Budget> findByUserAndMonthAndYear(User user, int month, int year);
    Optional<Budget> findByIdAndUser(Long id, User user);
    boolean existsByUserAndCategoryIdAndMonthAndYear(User user, Long categoryId, int month, int year);
    boolean existsByCategory(Category category);
}