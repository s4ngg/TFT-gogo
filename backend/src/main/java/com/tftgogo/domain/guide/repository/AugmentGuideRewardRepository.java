package com.tftgogo.domain.guide.repository;

import com.tftgogo.domain.guide.entity.AugmentGuideReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AugmentGuideRewardRepository extends JpaRepository<AugmentGuideReward, Long> {

    List<AugmentGuideReward> findByPatchVersionOrderByStageAscIdAsc(String patchVersion);

    @Query(value = """
            SELECT patch_version
            FROM augment_guide_rewards
            ORDER BY CAST(SUBSTRING_INDEX(patch_version, '.', 1) AS UNSIGNED) DESC,
                     CAST(SUBSTRING_INDEX(patch_version, '.', -1) AS UNSIGNED) DESC,
                     patch_version DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findLatestPatchVersion();
}
