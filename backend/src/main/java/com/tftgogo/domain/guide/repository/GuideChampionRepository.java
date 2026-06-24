package com.tftgogo.domain.guide.repository;

import com.tftgogo.domain.guide.entity.GuideChampion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GuideChampionRepository extends JpaRepository<GuideChampion, Long> {

    Optional<GuideChampion> findByChampionKeyAndPatchVersion(String championKey, String patchVersion);

    List<GuideChampion> findByPatchVersionOrderByNameAscIdAsc(String patchVersion);

    @Query(value = """
            SELECT patch_version
            FROM tft_guide_champions
            ORDER BY CAST(SUBSTRING_INDEX(patch_version, '.', 1) AS UNSIGNED) DESC,
                     CAST(SUBSTRING_INDEX(patch_version, '.', -1) AS UNSIGNED) DESC,
                     patch_version DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findLatestPatchVersion();
}
