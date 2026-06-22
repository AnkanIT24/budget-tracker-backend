package net.ankan.budget.repository;

import net.ankan.budget.entity.Category;
import net.ankan.budget.entity.TransactionType;
import net.ankan.budget.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser(User user);
    List<Category> findByUserAndType(User user, TransactionType type);
    Optional<Category> findByIdAndUser(Long id, User user);
}
