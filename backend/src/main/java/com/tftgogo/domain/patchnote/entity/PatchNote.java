package com.tftgogo.domain.patchnote.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String description;

    @Column(length = 200)
    private String focus;

    @Column(name = "representative_image_url", length = 500)
    private String imageUrl;

    @Transient
    private String sourceUrl;

    @Transient
    private String sourceLocale;

    @Transient
    private PatchNoteImportSource importSource;

    @Transient
    private LocalDateTime importedAt;

    @Transient
    private boolean manuallyEdited;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "highlights_json", columnDefinition = "JSON")
    private String highlightsJson;

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
                     String highlightsJson) {
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
