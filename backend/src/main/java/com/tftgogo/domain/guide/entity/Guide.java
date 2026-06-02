package com.tftgogo.domain.guide.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "guides",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"guide_type", "target_key", "patch_version"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "guide_type", nullable = false, length = 20)
    private GuideType guideType;

    @Column(name = "target_key", nullable = false, length = 100)
    private String targetKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "data_json", nullable = false, columnDefinition = "JSON")
    private String dataJson;

    @Column(name = "patch_version", nullable = false, length = 20)
    private String patchVersion;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Guide(GuideType guideType, String targetKey, String name, String summary,
                 String imageUrl, String dataJson, String patchVersion, int sortOrder,
                 boolean active) {
        this.guideType = guideType;
        this.targetKey = targetKey;
        this.name = name;
        this.summary = summary;
        this.imageUrl = imageUrl;
        this.dataJson = dataJson;
        this.patchVersion = patchVersion;
        this.sortOrder = sortOrder;
        this.active = active;
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
