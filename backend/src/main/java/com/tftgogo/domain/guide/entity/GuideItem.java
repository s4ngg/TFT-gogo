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
        name = "tft_guide_items",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"item_key", "patch_version"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_key", nullable = false, length = 100)
    private String itemKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "best_users_json", nullable = false, columnDefinition = "JSON")
    private String bestUsersJson;

    @Column(name = "combinations_json", nullable = false, columnDefinition = "JSON")
    private String combinationsJson;

    @Column(name = "patch_version", nullable = false, length = 20)
    private String patchVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public GuideItem(
            String itemKey,
            String name,
            String category,
            String imageUrl,
            String description,
            String bestUsersJson,
            String combinationsJson,
            String patchVersion
    ) {
        this.itemKey = itemKey;
        this.name = name;
        this.category = category;
        this.imageUrl = imageUrl;
        this.description = description;
        this.bestUsersJson = bestUsersJson;
        this.combinationsJson = combinationsJson;
        this.patchVersion = patchVersion;
    }

    public void update(
            String name,
            String category,
            String imageUrl,
            String description,
            String bestUsersJson,
            String combinationsJson
    ) {
        this.name = name;
        this.category = category;
        this.imageUrl = imageUrl;
        this.description = description;
        this.bestUsersJson = bestUsersJson;
        this.combinationsJson = combinationsJson;
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
