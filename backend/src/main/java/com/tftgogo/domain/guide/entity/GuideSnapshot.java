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
        name = "tft_guide_snapshots",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"patch_version"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patch_version", nullable = false, length = 20)
    private String patchVersion;

    @Column(name = "source_set_number")
    private Integer sourceSetNumber;

    @Column(name = "source_mutator", length = 100)
    private String sourceMutator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuideSnapshotStatus status;

    @Column(name = "champion_count", nullable = false)
    private int championCount;

    @Column(name = "trait_count", nullable = false)
    private int traitCount;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    @Column(name = "augment_count", nullable = false)
    private int augmentCount;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public GuideSnapshot(
            String patchVersion,
            Integer sourceSetNumber,
            String sourceMutator,
            GuideSnapshotStatus status,
            int championCount,
            int traitCount,
            int itemCount,
            int augmentCount,
            LocalDateTime validatedAt,
            LocalDateTime activatedAt
    ) {
        this.patchVersion = patchVersion;
        this.sourceSetNumber = sourceSetNumber;
        this.sourceMutator = sourceMutator;
        this.status = status;
        this.championCount = championCount;
        this.traitCount = traitCount;
        this.itemCount = itemCount;
        this.augmentCount = augmentCount;
        this.validatedAt = validatedAt;
        this.activatedAt = activatedAt;
    }

    public void updateValidation(
            int championCount,
            int traitCount,
            int itemCount,
            int augmentCount,
            LocalDateTime validatedAt
    ) {
        updateCounts(championCount, traitCount, itemCount, augmentCount);
        this.validatedAt = validatedAt;
    }

    public void updateCounts(
            int championCount,
            int traitCount,
            int itemCount,
            int augmentCount
    ) {
        this.championCount = championCount;
        this.traitCount = traitCount;
        this.itemCount = itemCount;
        this.augmentCount = augmentCount;
    }

    public boolean hasSource() {
        return sourceSetNumber != null && sourceMutator != null;
    }

    public boolean matchesSource(int setNumber, String mutator) {
        return hasSource()
                && sourceSetNumber == setNumber
                && sourceMutator.equals(mutator);
    }

    public void recordSource(int setNumber, String mutator) {
        if (setNumber < 1 || mutator == null || mutator.trim().isEmpty()) {
            throw new IllegalArgumentException("Guide snapshot source must be explicit");
        }
        if (hasSource() && !matchesSource(setNumber, mutator)) {
            throw new IllegalStateException("Guide snapshot source cannot be changed");
        }
        this.sourceSetNumber = setNumber;
        this.sourceMutator = mutator;
    }

    public void activate(LocalDateTime activatedAt) {
        if (validatedAt == null) {
            throw new IllegalStateException("A guide snapshot must be validated before activation");
        }
        this.status = GuideSnapshotStatus.ACTIVE;
        this.activatedAt = activatedAt;
    }

    public void deactivate() {
        this.status = GuideSnapshotStatus.INACTIVE;
    }

    public boolean isPubliclyReadable() {
        return validatedAt != null
                && (status == GuideSnapshotStatus.ACTIVE || status == GuideSnapshotStatus.INACTIVE);
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
