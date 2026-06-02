package com.tftgogo.domain.patchnote.entity;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "patch_changes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatchChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patch_note_id", nullable = false)
    private PatchNote patchNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PatchCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private PatchChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PatchImpact impact;

    @Column(name = "target_key", nullable = false, length = 100)
    private String targetKey;

    @Column(name = "target_name", nullable = false, length = 100)
    private String targetName;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(name = "before_value", length = 300)
    private String beforeValue;

    @Column(name = "after_value", length = 300)
    private String afterValue;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "tags_json", columnDefinition = "JSON")
    private String tagsJson;

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
    public PatchChange(PatchNote patchNote, PatchCategory category, PatchChangeType changeType,
                       PatchImpact impact, String targetKey, String targetName, String summary,
                       String beforeValue, String afterValue, String imageUrl, String tagsJson,
                       int sortOrder, boolean active) {
        this.patchNote = patchNote;
        this.category = category;
        this.changeType = changeType;
        this.impact = impact;
        this.targetKey = targetKey;
        this.targetName = targetName;
        this.summary = summary;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.imageUrl = imageUrl;
        this.tagsJson = tagsJson;
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
