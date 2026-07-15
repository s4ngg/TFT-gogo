package com.tftgogo.domain.patchnote.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "patch_note_change_tombstones",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_patch_note_change_tombstones_source",
                        columnNames = {"patch_note_id", "source_key"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatchChangeTombstone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patch_note_id", nullable = false)
    private PatchNote patchNote;

    @Column(name = "source_key", nullable = false, length = 150)
    private String sourceKey;

    @Column(name = "deleted_at", nullable = false, updatable = false)
    private LocalDateTime deletedAt;

    @Builder
    public PatchChangeTombstone(PatchNote patchNote, String sourceKey, LocalDateTime deletedAt) {
        this.patchNote = patchNote;
        this.sourceKey = sourceKey;
        this.deletedAt = deletedAt == null ? LocalDateTime.now() : deletedAt;
    }
}
