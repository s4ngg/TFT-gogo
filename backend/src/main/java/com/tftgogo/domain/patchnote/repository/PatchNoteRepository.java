package com.tftgogo.domain.patchnote.repository;

import com.tftgogo.domain.patchnote.entity.PatchNote;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PatchNoteRepository extends JpaRepository<PatchNote, Long> {

    List<PatchNote> findByDeletedAtIsNullOrderByCurrentDescPublishedAtDescIdDesc();

    @Query("SELECT p FROM PatchNote p "
            + "WHERE p.deletedAt IS NULL "
            + "AND (p.current = true OR p.publishedAt >= :cutoff) "
            + "ORDER BY CASE WHEN p.current = true THEN 0 ELSE 1 END, p.publishedAt DESC, p.id DESC")
    List<PatchNote> findPublicHistorySinceIncludingCurrent(@Param("cutoff") LocalDateTime cutoff);

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
