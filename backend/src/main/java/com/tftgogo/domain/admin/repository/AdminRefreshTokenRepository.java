package com.tftgogo.domain.admin.repository;

import com.tftgogo.domain.admin.entity.AdminRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, Long> {
    Optional<AdminRefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM AdminRefreshToken t WHERE t.adminAccountId = :adminAccountId")
    void deleteAllByAdminAccountId(Long adminAccountId);

    @Modifying
    @Query("DELETE FROM AdminRefreshToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);
}
