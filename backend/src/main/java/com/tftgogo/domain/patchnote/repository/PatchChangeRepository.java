package com.tftgogo.domain.patchnote.repository;

import com.tftgogo.domain.patchnote.entity.PatchCategory;
import com.tftgogo.domain.patchnote.entity.PatchChange;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;
import com.tftgogo.domain.patchnote.entity.PatchImpact;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatchChangeRepository extends JpaRepository<PatchChange, Long> {

    long countByPatchNoteAndActiveTrueAndDeletedAtIsNull(PatchNote patchNote);

    List<PatchChange> findByPatchNoteAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc(PatchNote patchNote);

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
                    OR LOWER(c.targetKey) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(c.targetName) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(c.summary) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY c.sortOrder ASC, c.id ASC
            """)
    List<PatchChange> findFilteredChanges(
            @Param("patchNote") PatchNote patchNote,
            @Param("category") PatchCategory category,
            @Param("changeType") PatchChangeType changeType,
            @Param("impact") PatchImpact impact,
            @Param("query") String query
    );
}
