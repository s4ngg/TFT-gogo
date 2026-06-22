package com.tftgogo.domain.guide.repository;

import com.tftgogo.domain.guide.entity.GuideItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GuideItemRepository extends JpaRepository<GuideItem, Long> {

    Optional<GuideItem> findByItemKeyAndPatchVersion(String itemKey, String patchVersion);

    List<GuideItem> findByPatchVersionOrderByNameAscIdAsc(String patchVersion);

    @Query(value = """
            SELECT patch_version
            FROM tft_guide_items
            ORDER BY CAST(SUBSTRING_INDEX(patch_version, '.', 1) AS UNSIGNED) DESC,
                     CAST(SUBSTRING_INDEX(patch_version, '.', -1) AS UNSIGNED) DESC,
                     patch_version DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findLatestPatchVersion();
}
