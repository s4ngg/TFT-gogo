package com.tftgogo.domain.guide.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "augment_guide_rewards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AugmentGuideReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String stage;

    @Column(name = "condition_text", nullable = false, length = 200)
    private String conditionText;

    @Column(name = "reward_text", nullable = false, length = 200)
    private String rewardText;

    @Column(name = "patch_version", nullable = false, length = 20)
    private String patchVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public AugmentGuideReward(String stage, String conditionText, String rewardText, String patchVersion) {
        this.stage = stage;
        this.conditionText = conditionText;
        this.rewardText = rewardText;
        this.patchVersion = patchVersion;
    }

    public void update(String conditionText, String rewardText) {
        this.conditionText = conditionText;
        this.rewardText = rewardText;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
