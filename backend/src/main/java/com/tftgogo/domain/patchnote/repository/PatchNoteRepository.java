package com.tftgogo.domain.patchnote.repository;

import com.tftgogo.domain.patchnote.entity.PatchNote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface PatchNoteRepository extends JpaRepository<PatchNote, Long> {

    List<PatchNote> findByDeletedAtIsNullOrderByCurrentDescPublishedAtDescIdDesc();

    Optional<PatchNote> findByVersionAndDeletedAtIsNull(String version);

    Optional<PatchNote> findByIdAndDeletedAtIsNull(Long id);

    Optional<PatchNote> findByVersion(String version);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PatchNote> findByCurrentTrueAndDeletedAtIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PatchNote> findByCurrentTrueAndDeletedAtIsNullAndIdNot(Long id);
}
