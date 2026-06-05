package com.tftgogo.domain.patchnote.repository;

import com.tftgogo.domain.patchnote.entity.PatchNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatchNoteRepository extends JpaRepository<PatchNote, Long> {

    List<PatchNote> findByActiveTrueAndDeletedAtIsNullOrderByCurrentDescPublishedAtDescIdDesc();

    List<PatchNote> findByDeletedAtIsNullOrderByCurrentDescPublishedAtDescIdDesc();

    Optional<PatchNote> findByVersionAndActiveTrueAndDeletedAtIsNull(String version);

    Optional<PatchNote> findByIdAndDeletedAtIsNull(Long id);

    Optional<PatchNote> findByVersion(String version);

    List<PatchNote> findByCurrentTrueAndActiveTrueAndDeletedAtIsNull();

    List<PatchNote> findByCurrentTrueAndActiveTrueAndDeletedAtIsNullAndIdNot(Long id);
}
