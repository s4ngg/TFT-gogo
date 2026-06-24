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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tft_guide_augments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"augment_key", "patch_version"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideAugment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "augment_key", nullable = false, length = 100)
    private String augmentKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "tags_json", nullable = false, columnDefinition = "JSON")
    private String tagsJson;

    @Column(name = "stats_json", nullable = false, columnDefinition = "JSON")
    private String statsJson;

    @Column(name = "patch_version", nullable = false, length = 20)
    private String patchVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public GuideAugment(
            String augmentKey,
            String name,
            String description,
            String iconUrl,
            String tagsJson,
            String statsJson,
            String patchVersion
    ) {
        this.augmentKey = augmentKey;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.tagsJson = tagsJson;
        this.statsJson = statsJson;
        this.patchVersion = patchVersion;
    }

    public void update(
            String name,
            String description,
            String iconUrl,
            String tagsJson,
            String statsJson
    ) {
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.tagsJson = tagsJson;
        this.statsJson = statsJson;
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
