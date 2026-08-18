package net.ankan.budget.repository;

import net.ankan.budget.entity.PasswordResetToken;
import net.ankan.budget.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Invalidates every outstanding token for a user. Called when a new reset is
     * requested and again after a successful reset, so an old email in an inbox
     * can't be replayed later.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :now " +
           "WHERE t.user = :user AND t.usedAt IS NULL")
    void invalidateAllForUser(@Param("user") User user, @Param("now") LocalDateTime now);
}
