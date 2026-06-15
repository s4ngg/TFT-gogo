package com.tftgogo.domain.patchnote.repository;

import com.tftgogo.domain.patchnote.entity.PatchChangeCategory;
import com.tftgogo.domain.patchnote.entity.PatchChange;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;
import com.tftgogo.domain.patchnote.entity.PatchChangeImpact;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatchChangeRepository extends JpaRepository<PatchChange, Long> {

    interface PatchChangeCount {
        Long getPatchNoteId();

        Long getChangeCount();
    }

    @Query("""
            SELECT c.patchNote.id AS patchNoteId, COUNT(c) AS changeCount
            FROM PatchChange c
            WHERE c.patchNote IN :patchNotes
              AND c.active = true
              AND c.deletedAt IS NULL
            GROUP BY c.patchNote.id
            """)
    List<PatchChangeCount> countByPatchNotes(@Param("patchNotes") List<PatchNote> patchNotes);

    List<PatchChange> findByPatchNoteAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc(PatchNote patchNote);

    List<PatchChange> findByPatchNoteAndDeletedAtIsNullOrderBySortOrderAscIdAsc(PatchNote patchNote);

    Optional<PatchChange> findByIdAndDeletedAtIsNull(Long id);

    Optional<PatchChange> findByPatchNoteAndSourceKey(PatchNote patchNote, String sourceKey);

    @Query("""
            SELECT c
            FROM PatchChange c
            WHERE c.patchNote = :patchNote
              AND c.active = true
              AND c.deletedAt IS NULL
              AND (:category IS NULL OR c.category = :category)
              AND (:changeType IS NULL OR c.changeType = :changeType)
              AND (:impact IS NULL OR c.impact = :impact)
              AND (
                    :query IS NULL
                    OR LOWER(c.targetKey) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
                    OR LOWER(c.targetName) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
                    OR LOWER(c.summary) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
              )
            ORDER BY c.sortOrder ASC, c.id ASC
            """)
    List<PatchChange> findFilteredChanges(
            @Param("patchNote") PatchNote patchNote,
            @Param("category") PatchChangeCategory category,
            @Param("changeType") PatchChangeType changeType,
            @Param("impact") PatchChangeImpact impact,
            @Param("query") String query
    );
}
