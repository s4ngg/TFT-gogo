package com.tftgogo.domain.guide.repository;

import com.tftgogo.domain.guide.entity.GuideAugment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GuideAugmentRepository extends JpaRepository<GuideAugment, Long> {

    Optional<GuideAugment> findByAugmentKeyAndPatchVersion(String augmentKey, String patchVersion);

    List<GuideAugment> findByPatchVersionOrderByTierAscNameAscIdAsc(String patchVersion);

    @Query(value = """
            SELECT patch_version
            FROM tft_guide_augments
            ORDER BY CAST(SUBSTRING_INDEX(patch_version, '.', 1) AS UNSIGNED) DESC,
                     CAST(SUBSTRING_INDEX(patch_version, '.', -1) AS UNSIGNED) DESC,
                     patch_version DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findLatestPatchVersion();
}
