package com.tftgogo.domain.patchnote.entity;

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
        name = "patch_notes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"version"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatchNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 200)
    private String focus;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "highlights_json", columnDefinition = "JSON")
    private String highlightsJson;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public PatchNote(String version, String title, String summary, String description,
                     String focus, String imageUrl, LocalDateTime publishedAt,
                     boolean current, String highlightsJson, boolean active) {
        this.version = version;
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.focus = focus;
        this.imageUrl = imageUrl;
        this.publishedAt = publishedAt;
        this.current = current;
        this.highlightsJson = highlightsJson;
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
