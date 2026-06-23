package com.tftgogo.domain.guide.repository;

import com.tftgogo.domain.guide.entity.AugmentGuidePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AugmentGuidePlanRepository extends JpaRepository<AugmentGuidePlan, Long> {

    List<AugmentGuidePlan> findByPatchVersionOrderByPlanKeyAscIdAsc(String patchVersion);

    @Query(value = """
            SELECT patch_version
            FROM augment_guide_plans
            ORDER BY CAST(SUBSTRING_INDEX(patch_version, '.', 1) AS UNSIGNED) DESC,
                     CAST(SUBSTRING_INDEX(patch_version, '.', -1) AS UNSIGNED) DESC,
                     patch_version DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findLatestPatchVersion();
}
