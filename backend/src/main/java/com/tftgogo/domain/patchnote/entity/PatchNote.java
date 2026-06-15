package com.tftgogo.domain.patchnote.entity;

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

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_locale", length = 20)
    private String sourceLocale;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_source", length = 30)
    private PatchNoteImportSource importSource;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @Column(name = "manually_edited", nullable = false)
    private boolean manuallyEdited;

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
                     String focus, String imageUrl, String sourceUrl, String sourceLocale,
                     PatchNoteImportSource importSource, LocalDateTime importedAt,
                     boolean manuallyEdited, LocalDateTime publishedAt, boolean current,
                     String highlightsJson, boolean active) {
        this.version = version;
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.focus = focus;
        this.imageUrl = imageUrl;
        this.sourceUrl = sourceUrl;
        this.sourceLocale = sourceLocale;
        this.importSource = importSource;
        this.importedAt = importedAt;
        this.manuallyEdited = manuallyEdited;
        this.publishedAt = publishedAt;
        this.current = current;
        this.highlightsJson = highlightsJson;
        this.active = active;
    }

    public void update(String version, String title, String summary, String description,
                       String focus, String imageUrl, LocalDateTime publishedAt,
                       boolean current, String highlightsJson) {
        this.version = version;
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.focus = focus;
        this.imageUrl = imageUrl;
        this.publishedAt = publishedAt;
        this.current = current;
        this.highlightsJson = highlightsJson;
    }

    public void markNotCurrent() {
        this.current = false;
    }

    public void markManuallyEditedIfImported() {
        if (isImported()) {
            this.manuallyEdited = true;
        }
    }

    public boolean isImported() {
        return importSource != null;
    }

    public void softDelete() {
        LocalDateTime now = LocalDateTime.now();
        this.active = false;
        this.current = false;
        this.deletedAt = now;
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
