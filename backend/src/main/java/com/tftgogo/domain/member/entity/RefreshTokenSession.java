package com.tftgogo.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "reuse_detected", nullable = false)
    private boolean reuseDetected;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public static RefreshTokenSession of(Member member, String tokenHash, LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        RefreshTokenSession session = new RefreshTokenSession();
        session.member = member;
        session.tokenHash = tokenHash;
        session.expiresAt = expiresAt;
        session.revoked = false;
        session.reuseDetected = false;
        session.createdAt = now;
        session.updatedAt = now;
        return session;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public void revoke(LocalDateTime now) {
        this.revoked = true;
        this.revokedAt = now;
        this.updatedAt = now;
    }

    public void markReuseDetected(LocalDateTime now) {
        this.reuseDetected = true;
        revoke(now);
    }
}
