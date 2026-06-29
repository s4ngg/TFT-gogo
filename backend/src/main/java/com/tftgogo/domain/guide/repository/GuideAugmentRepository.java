package com.tftgogo.domain.guide.repository;

import com.tftgogo.domain.guide.entity.GuideAugment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuideAugmentRepository extends JpaRepository<GuideAugment, Long> {

    Optional<GuideAugment> findByAugmentKeyAndPatchVersion(String augmentKey, String patchVersion);

    List<GuideAugment> findByPatchVersionOrderByNameAscIdAsc(String patchVersion);

    @Query(
            value = """
                    SELECT *
                    FROM tft_guide_augments
                    WHERE patch_version = :patchVersion
                      AND (
                            :query IS NULL
                            OR LOWER(name) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(description) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(augment_key) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(tags_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                    ORDER BY name ASC, id ASC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM tft_guide_augments
                    WHERE patch_version = :patchVersion
                      AND (
                            :query IS NULL
                            OR LOWER(name) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(description) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(augment_key) LIKE LOWER(CONCAT('%', :query, '%'))
                            OR LOWER(CAST(tags_json AS CHAR)) LIKE LOWER(CONCAT('%', :query, '%'))
                      )
                    """,
            nativeQuery = true
    )
    Page<GuideAugment> searchPage(
            @Param("patchVersion") String patchVersion,
            @Param("query") String query,
            Pageable pageable
    );

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
