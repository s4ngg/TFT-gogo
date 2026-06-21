package com.tftgogo.domain.guide.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "augment_guide_plans",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"plan_key", "patch_version"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AugmentGuidePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_key", nullable = false, length = 50)
    private String planKey;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "stages_json", nullable = false, columnDefinition = "JSON")
    private String stagesJson;

    @Column(name = "patch_version", nullable = false, length = 20)
    private String patchVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
