package com.tftgogo.domain.guide.repository;

import com.tftgogo.domain.guide.entity.GuideChampion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuideChampionRepository extends JpaRepository<GuideChampion, Long> {

    Optional<GuideChampion> findByChampionKeyAndPatchVersion(String championKey, String patchVersion);

    List<GuideChampion> findByPatchVersionOrderByNameAscIdAsc(String patchVersion);

    @Query(
            value = """
                    SELECT *
                    FROM tft_guide_champions
                    WHERE patch_version = :patchVersion
                      AND (:cost IS NULL OR cost = :cost)
                      AND (
                            :query IS NULL
                            OR LOWER(name) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(champion_key) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(role) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(position) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(traits_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(best_items_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                      AND JSON_TYPE(traits_json) = 'ARRAY'
                      AND JSON_LENGTH(traits_json) > 0
                    ORDER BY name ASC, id ASC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM tft_guide_champions
                    WHERE patch_version = :patchVersion
                      AND (:cost IS NULL OR cost = :cost)
                      AND (
                            :query IS NULL
                            OR LOWER(name) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(champion_key) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(role) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(position) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(traits_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(best_items_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                      AND JSON_TYPE(traits_json) = 'ARRAY'
                      AND JSON_LENGTH(traits_json) > 0
                    """,
            nativeQuery = true
    )
    Page<GuideChampion> searchPage(
            @Param("patchVersion") String patchVersion,
            @Param("query") String query,
            @Param("cost") Integer cost,
            Pageable pageable
    );

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
