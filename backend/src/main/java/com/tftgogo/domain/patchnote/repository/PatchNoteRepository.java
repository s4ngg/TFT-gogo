package com.tftgogo.domain.patchnote.repository;

import com.tftgogo.domain.patchnote.entity.PatchNote;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PatchNoteRepository extends JpaRepository<PatchNote, Long> {

    List<PatchNote> findByDeletedAtIsNullOrderByCurrentDescPublishedAtDescIdDesc();

    List<PatchNote> findByDeletedAtIsNullAndPublishedAtGreaterThanEqualOrderByPublishedAtDescIdDesc(
            LocalDateTime publishedAt
    );

    Optional<PatchNote> findFirstByDeletedAtIsNullOrderByCurrentDescPublishedAtDescIdDesc();

    Optional<PatchNote> findByVersionAndDeletedAtIsNull(String version);

    Optional<PatchNote> findByIdAndDeletedAtIsNull(Long id);

    Optional<PatchNote> findByVersion(String version);

    Optional<PatchNote> findBySourceKey(String sourceKey);

    Optional<PatchNote> findBySourceUrl(String sourceUrl);

    Optional<PatchNote> findFirstByCurrentTrueAndDeletedAtIsNullOrderByPublishedAtDescIdDesc();

    Optional<PatchNote> findFirstByDeletedAtIsNullOrderByPublishedAtDescIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    List<PatchNote> findByCurrentTrueAndDeletedAtIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    List<PatchNote> findByCurrentTrueAndDeletedAtIsNullAndIdNot(Long id);
}
