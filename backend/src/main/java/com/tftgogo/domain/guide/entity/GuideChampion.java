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
        name = "tft_guide_champions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"champion_key", "patch_version"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideChampion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "champion_key", nullable = false, length = 100)
    private String championKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int cost;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(nullable = false, length = 50)
    private String position;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "stats_json", nullable = false, columnDefinition = "JSON")
    private String statsJson;

    @Column(name = "traits_json", nullable = false, columnDefinition = "JSON")
    private String traitsJson;

    @Column(name = "best_items_json", nullable = false, columnDefinition = "JSON")
    private String bestItemsJson;

    @Column(name = "patch_version", nullable = false, length = 20)
    private String patchVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public GuideChampion(
            String championKey,
            String name,
            int cost,
            String role,
            String position,
            String imageUrl,
            String statsJson,
            String traitsJson,
            String bestItemsJson,
            String patchVersion
    ) {
        this.championKey = championKey;
        this.name = name;
        this.cost = cost;
        this.role = role;
        this.position = position;
        this.imageUrl = imageUrl;
        this.statsJson = statsJson;
        this.traitsJson = traitsJson;
        this.bestItemsJson = bestItemsJson;
        this.patchVersion = patchVersion;
    }

    public void update(
            String name,
            int cost,
            String role,
            String position,
            String imageUrl,
            String statsJson,
            String traitsJson,
            String bestItemsJson
    ) {
        this.name = name;
        this.cost = cost;
        this.role = role;
        this.position = position;
        this.imageUrl = imageUrl;
        this.statsJson = statsJson;
        this.traitsJson = traitsJson;
        this.bestItemsJson = bestItemsJson;
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
