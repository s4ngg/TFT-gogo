package com.tftgogo.domain.guide.repository;

import com.tftgogo.domain.guide.entity.Guide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuideRepository extends JpaRepository<Guide, Long> {

    List<Guide> findByPatchVersionAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc(String patchVersion);

    @Query(value = """
            SELECT g.patch_version
            FROM guides g
            WHERE g.is_active = true
              AND g.deleted_at IS NULL
            ORDER BY CAST(SUBSTRING_INDEX(g.patch_version, '.', 1) AS UNSIGNED) DESC,
                     CAST(SUBSTRING_INDEX(g.patch_version, '.', -1) AS UNSIGNED) DESC,
                     g.patch_version DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findLatestPatchVersion();

    @Query(value = """
            SELECT *
            FROM guides g
            WHERE g.guide_type = :guideType
              AND g.is_active = true
              AND g.deleted_at IS NULL
              AND (:patchVersion IS NULL OR g.patch_version = :patchVersion)
              AND (
                    :query IS NULL
                    OR LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\\\'
                    OR LOWER(g.summary) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\\\'
                    OR LOWER(g.target_key) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\\\'
              )
              AND (
                    :cost IS NULL
                    OR :guideType <> 'CHAMPION'
                    OR CAST(JSON_UNQUOTE(JSON_EXTRACT(g.data_json, '$.cost')) AS UNSIGNED) = :cost
              )
            ORDER BY g.sort_order ASC, g.id ASC
            """, nativeQuery = true)
    List<Guide> findFilteredGuides(
            @Param("guideType") String guideType,
            @Param("patchVersion") String patchVersion,
            @Param("query") String query,
            @Param("cost") Integer cost
    );
}
