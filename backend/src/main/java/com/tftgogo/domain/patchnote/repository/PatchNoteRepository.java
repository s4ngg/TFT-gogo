package com.tftgogo.domain.patchnote.repository;

import com.tftgogo.domain.patchnote.entity.PatchNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatchNoteRepository extends JpaRepository<PatchNote, Long> {

    List<PatchNote> findByActiveTrueAndDeletedAtIsNullOrderByCurrentDescPublishedAtDescIdDesc();

    Optional<PatchNote> findByVersionAndActiveTrueAndDeletedAtIsNull(String version);
}
