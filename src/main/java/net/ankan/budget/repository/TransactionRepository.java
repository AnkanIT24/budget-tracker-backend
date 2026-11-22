
package net.ankan.budget.repository;

import net.ankan.budget.entity.Category;
import net.ankan.budget.entity.Transaction;
import net.ankan.budget.entity.TransactionType;
import net.ankan.budget.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserOrderByDateDesc(User user);

    Optional<Transaction> findByIdAndUser(Long id, User user);

    boolean existsByCategory(Category category);

    // For summary endpoint
    @Query("SELECT SUM(t.amount) FROM Transaction t " +
            "WHERE t.user = :user AND t.type = :type " +
            "AND MONTH(t.date) = :month AND YEAR(t.date) = :year")
    BigDecimal sumByUserAndTypeAndMonthAndYear(
            @Param("user") User user,
            @Param("type") TransactionType type,
            @Param("month") int month,
            @Param("year") int year
    );

    // For budget status
    @Query("SELECT SUM(t.amount) FROM Transaction t " +
            "WHERE t.user = :user AND t.category = :category " +
            "AND t.type = 'EXPENSE' " +
            "AND MONTH(t.date) = :month AND YEAR(t.date) = :year")
    BigDecimal sumByUserAndCategoryAndMonthAndYear(
            @Param("user") User user,
            @Param("category") Category category,
            @Param("month") int month,
            @Param("year") int year
    );

    // Filtered queries
    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
            "AND (:month IS NULL OR MONTH(t.date) = :month) " +
            "AND (:year IS NULL OR YEAR(t.date) = :year) " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "ORDER BY t.date DESC, t.createdAt DESC")
    List<Transaction> findFiltered(
            @Param("user") User user,
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("type") TransactionType type,
            @Param("category") Category category
    );

    // Category-wise monthly expense analysis
    @Query("""
            SELECT t.category.id,
                   t.category.name,
                   SUM(t.amount)
            FROM Transaction t
            WHERE t.user = :user
              AND t.type = :type
              AND MONTH(t.date) = :month
              AND YEAR(t.date) = :year
            GROUP BY t.category.id, t.category.name
            ORDER BY SUM(t.amount) DESC
            """)
    List<Object[]> findCategoryWiseSpending(
            @Param("user") User user,
            @Param("type") TransactionType type,
            @Param("month") int month,
            @Param("year") int year
    );

    // Historical monthly income and expense analysis
    @Query("""
            SELECT
                MONTH(t.date),
                YEAR(t.date),
                SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END),
                SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END)
            FROM Transaction t
            WHERE t.user = :user
              AND t.date >= :startDate
              AND t.date < :endDate
            GROUP BY YEAR(t.date), MONTH(t.date)
            ORDER BY YEAR(t.date), MONTH(t.date)
            """)
    List<Object[]> findMonthlyFinancialHistory(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
