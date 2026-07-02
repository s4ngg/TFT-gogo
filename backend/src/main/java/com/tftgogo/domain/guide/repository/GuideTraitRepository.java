package com.tftgogo.domain.guide.repository;

import com.tftgogo.domain.guide.entity.GuideTrait;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuideTraitRepository extends JpaRepository<GuideTrait, Long> {

    Optional<GuideTrait> findByTraitKeyAndPatchVersion(String traitKey, String patchVersion);

    List<GuideTrait> findByPatchVersionOrderByNameAscIdAsc(String patchVersion);

    @Query(
            value = """
                    SELECT *
                    FROM tft_guide_traits
                    WHERE patch_version = :patchVersion
                      AND (
                            :hideBaseStargazer = false
                            OR trait_key NOT REGEXP '^TFT[0-9]+_Stargazer$'
                      )
                      AND (
                            :query IS NULL
                            OR LOWER(name) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(type) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(summary) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(trait_key) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(levels_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(tier_effects_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(champions_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(special_units_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(tips_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                      AND JSON_TYPE(champions_json) = 'ARRAY'
                      AND JSON_LENGTH(champions_json) > 0
                    ORDER BY name ASC, id ASC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM tft_guide_traits
                    WHERE patch_version = :patchVersion
                      AND (
                            :hideBaseStargazer = false
                            OR trait_key NOT REGEXP '^TFT[0-9]+_Stargazer$'
                      )
                      AND (
                            :query IS NULL
                            OR LOWER(name) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(type) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(summary) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(trait_key) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(levels_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(tier_effects_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(champions_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(special_units_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(tips_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                      AND JSON_TYPE(champions_json) = 'ARRAY'
                      AND JSON_LENGTH(champions_json) > 0
                    """,
            nativeQuery = true
    )
    Page<GuideTrait> searchPage(
            @Param("patchVersion") String patchVersion,
            @Param("query") String query,
            @Param("hideBaseStargazer") boolean hideBaseStargazer,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM tft_guide_traits
                    WHERE patch_version = :patchVersion
                      AND trait_key IN (
                          'TFT17_Stargazer_Wolf',
                          'TFT17_Stargazer_Medallion',
                          'TFT17_Stargazer_Huntress',
                          'TFT17_Stargazer_Serpent',
                          'TFT17_Stargazer_Shield',
                          'TFT17_Stargazer_Fountain',
                          'TFT17_Stargazer_Mountain'
                      )
                    """,
            nativeQuery = true
    )
    long countStargazerVariantsByPatchVersion(@Param("patchVersion") String patchVersion);

    @Query(value = """
            SELECT patch_version
            FROM tft_guide_traits
            ORDER BY CAST(SUBSTRING_INDEX(patch_version, '.', 1) AS UNSIGNED) DESC,
                     CAST(SUBSTRING_INDEX(patch_version, '.', -1) AS UNSIGNED) DESC,
                     patch_version DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findLatestPatchVersion();
}
