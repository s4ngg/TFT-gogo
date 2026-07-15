package com.tftgogo.domain.patchnote.repository;

import com.tftgogo.domain.patchnote.entity.PatchChangeTombstone;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface PatchChangeTombstoneRepository extends JpaRepository<PatchChangeTombstone, Long> {

    boolean existsByPatchNoteAndSourceKey(PatchNote patchNote, String sourceKey);

    @Query("SELECT tombstone.sourceKey FROM PatchChangeTombstone tombstone WHERE tombstone.patchNote = :patchNote")
    Set<String> findSourceKeysByPatchNote(@Param("patchNote") PatchNote patchNote);
}
