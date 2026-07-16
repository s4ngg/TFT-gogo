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

    interface GuideDataCounts {
        long getChampionCount();

        long getTraitCount();

        long getItemCount();

        long getAugmentCount();
    }

    Optional<GuideSnapshot> findByPatchVersion(String patchVersion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT snapshot FROM GuideSnapshot snapshot WHERE snapshot.patchVersion = :patchVersion")
    Optional<GuideSnapshot> findByPatchVersionForUpdate(@Param("patchVersion") String patchVersion);

    Optional<GuideSnapshot> findFirstByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus status);

    @Query(value = """
            SELECT
                (SELECT COUNT(*) FROM tft_guide_champions WHERE patch_version = :patchVersion) AS championCount,
                (SELECT COUNT(*) FROM tft_guide_traits WHERE patch_version = :patchVersion) AS traitCount,
                (SELECT COUNT(*) FROM tft_guide_items WHERE patch_version = :patchVersion) AS itemCount,
                (SELECT COUNT(*) FROM tft_guide_augments WHERE patch_version = :patchVersion) AS augmentCount
            """, nativeQuery = true)
    GuideDataCounts countGuideDataByPatchVersion(@Param("patchVersion") String patchVersion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GuideSnapshot> findFirstForUpdateByStatusOrderByActivatedAtDescIdDesc(GuideSnapshotStatus status);
}
