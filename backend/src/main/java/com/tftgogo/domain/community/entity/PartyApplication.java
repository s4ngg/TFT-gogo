package com.tftgogo.domain.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "party_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_post_id", nullable = false)
    private PartyPost partyPost;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartyApplicationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public static PartyApplication accepted(PartyPost partyPost, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        PartyApplication application = new PartyApplication();
        application.partyPost = partyPost;
        application.userId = userId;
        application.status = PartyApplicationStatus.ACCEPTED;
        application.createdAt = now;
        application.respondedAt = now;
        return application;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
