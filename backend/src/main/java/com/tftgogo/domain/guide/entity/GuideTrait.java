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
        name = "tft_guide_traits",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"trait_key", "patch_version"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideTrait {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trait_key", nullable = false, length = 100)
    private String traitKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "icon_url", nullable = false, length = 500)
    private String iconUrl;

    @Column(nullable = false, length = 30)
    private String tone;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "levels_json", nullable = false, columnDefinition = "JSON")
    private String levelsJson;

    @Column(name = "tier_effects_json", nullable = false, columnDefinition = "JSON")
    private String tierEffectsJson;

    @Column(name = "champions_json", nullable = false, columnDefinition = "JSON")
    private String championsJson;

    @Column(name = "tips_json", nullable = false, columnDefinition = "JSON")
    private String tipsJson;

    @Column(name = "patch_version", nullable = false, length = 20)
    private String patchVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public GuideTrait(
            String traitKey,
            String name,
            String type,
            String iconUrl,
            String tone,
            String summary,
            String levelsJson,
            String tierEffectsJson,
            String championsJson,
            String tipsJson,
            String patchVersion
    ) {
        this.traitKey = traitKey;
        this.name = name;
        this.type = type;
        this.iconUrl = iconUrl;
        this.tone = tone;
        this.summary = summary;
        this.levelsJson = levelsJson;
        this.tierEffectsJson = tierEffectsJson;
        this.championsJson = championsJson;
        this.tipsJson = tipsJson;
        this.patchVersion = patchVersion;
    }

    public void update(
            String name,
            String type,
            String iconUrl,
            String tone,
            String summary,
            String levelsJson,
            String tierEffectsJson,
            String championsJson,
            String tipsJson
    ) {
        this.name = name;
        this.type = type;
        this.iconUrl = iconUrl;
        this.tone = tone;
        this.summary = summary;
        this.levelsJson = levelsJson;
        this.tierEffectsJson = tierEffectsJson;
        this.championsJson = championsJson;
        this.tipsJson = tipsJson;
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
