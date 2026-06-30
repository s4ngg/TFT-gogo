package com.tftgogo.domain.admin.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 45)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 255)
    private String target;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AdminAuditLog(Long adminId, String username, String ip,
                         String userAgent, String action, String target) {
        this.adminId = adminId;
        this.username = username;
        this.ip = ip;
        this.userAgent = userAgent;
        this.action = action;
        this.target = target;
        this.createdAt = LocalDateTime.now();
    }
}
