package com.tftgogo.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_token_blocklist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessTokenBlocklist {

    @Id
    @Column(name = "token_id", nullable = false, length = 80)
    private String tokenId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static AccessTokenBlocklist of(String tokenId, Long userId, LocalDateTime expiresAt) {
        AccessTokenBlocklist blocklist = new AccessTokenBlocklist();
        blocklist.tokenId = tokenId;
        blocklist.userId = userId;
        blocklist.expiresAt = expiresAt;
        blocklist.createdAt = LocalDateTime.now();
        return blocklist;
    }
}
