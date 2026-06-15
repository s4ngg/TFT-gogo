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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "patch_changes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"patch_note_id", "source_key"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatchChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patch_note_id", nullable = false)
    private PatchNote patchNote;

    @Column(name = "source_key", length = 64)
    private String sourceKey;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_heading_path", length = 500)
    private String sourceHeadingPath;

    @Column(name = "source_order")
    private Integer sourceOrder;

    @Column(name = "source_locale", length = 20)
    private String sourceLocale;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_source", length = 30)
    private PatchNoteImportSource importSource;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @Column(name = "manually_edited", nullable = false)
    private boolean manuallyEdited;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PatchChangeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private PatchChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PatchChangeImpact impact;

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
    public PatchChange(PatchNote patchNote, String sourceKey, String sourceUrl, String sourceHeadingPath,
                       Integer sourceOrder, String sourceLocale, PatchNoteImportSource importSource,
                       LocalDateTime importedAt, boolean manuallyEdited, PatchChangeCategory category,
                       PatchChangeType changeType, PatchChangeImpact impact, String targetKey,
                       String targetName, String summary, String beforeValue, String afterValue,
                       String imageUrl, String tagsJson, int sortOrder, boolean active) {
        this.patchNote = patchNote;
        this.sourceKey = sourceKey;
        this.sourceUrl = sourceUrl;
        this.sourceHeadingPath = sourceHeadingPath;
        this.sourceOrder = sourceOrder;
        this.sourceLocale = sourceLocale;
        this.importSource = importSource;
        this.importedAt = importedAt;
        this.manuallyEdited = manuallyEdited;
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

    public void update(PatchNote patchNote, PatchChangeCategory category, PatchChangeType changeType,
                       PatchChangeImpact impact, String targetKey, String targetName, String summary,
                       String beforeValue, String afterValue, String imageUrl, String tagsJson,
                       int sortOrder) {
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
    }

    public void updateImported(PatchNote patchNote, String sourceKey, String sourceUrl, String sourceHeadingPath,
                               Integer sourceOrder, String sourceLocale, PatchNoteImportSource importSource,
                               LocalDateTime importedAt, PatchChangeCategory category, PatchChangeType changeType,
                               PatchChangeImpact impact, String targetKey, String targetName, String summary,
                               String beforeValue, String afterValue, String imageUrl, String tagsJson,
                               int sortOrder, boolean active) {
        this.patchNote = patchNote;
        this.sourceKey = sourceKey;
        this.sourceUrl = sourceUrl;
        this.sourceHeadingPath = sourceHeadingPath;
        this.sourceOrder = sourceOrder;
        this.sourceLocale = sourceLocale;
        this.importSource = importSource;
        this.importedAt = importedAt;
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

    public void softDelete() {
        LocalDateTime now = LocalDateTime.now();
        this.active = false;
        this.deletedAt = now;
    }

    public void markManuallyEditedIfImported() {
        if (isImported()) {
            this.manuallyEdited = true;
        }
    }

    public boolean isImported() {
        return importSource != null;
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
