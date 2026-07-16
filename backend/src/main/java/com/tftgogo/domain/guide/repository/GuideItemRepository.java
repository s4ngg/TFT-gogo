package com.tftgogo.domain.guide.repository;

import com.tftgogo.domain.guide.entity.GuideItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GuideItemRepository extends JpaRepository<GuideItem, Long> {

    Optional<GuideItem> findByItemKeyAndPatchVersion(String itemKey, String patchVersion);

    List<GuideItem> findByPatchVersionOrderByNameAscIdAsc(String patchVersion);

    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM GuideItem item
            WHERE item.patchVersion = :patchVersion
              AND item.itemKey NOT IN (:retainedKeys)
            """)
    int deleteStaleByPatchVersion(
            @Param("patchVersion") String patchVersion,
            @Param("retainedKeys") Set<String> retainedKeys
    );

    @Query(
            value = """
                    SELECT *
                    FROM tft_guide_items
                    WHERE patch_version = :patchVersion
                      AND (
                            :query IS NULL
                            OR LOWER(name) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(category) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(description) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(item_key) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(best_users_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(combinations_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                    ORDER BY name ASC, id ASC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM tft_guide_items
                    WHERE patch_version = :patchVersion
                      AND (
                            :query IS NULL
                            OR LOWER(name) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(category) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(description) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(item_key) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(best_users_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(combinations_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                    """,
            nativeQuery = true
    )
    Page<GuideItem> searchPage(
            @Param("patchVersion") String patchVersion,
            @Param("query") String query,
            Pageable pageable
    );

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
