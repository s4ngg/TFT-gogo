package com.tftgogo.domain.guide.repository;

import com.tftgogo.domain.guide.entity.GuideSnapshot;
import com.tftgogo.domain.guide.entity.GuideSnapshotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GuideSnapshotRepository extends JpaRepository<GuideSnapshot, Long> {

    Optional<GuideSnapshot> findByPatchVersion(String patchVersion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT snapshot FROM GuideSnapshot snapshot WHERE snapshot.patchVersion = :patchVersion")
    Optional<GuideSnapshot> findByPatchVersionForUpdate(@Param("patchVersion") String patchVersion);

    Optional<GuideSnapshot> findFirstByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GuideSnapshot> findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus status);
}
