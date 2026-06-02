package com.tftgogo.domain.guide.repository;

import com.tftgogo.domain.guide.entity.Guide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GuideRepository extends JpaRepository<Guide, Long> {

    List<Guide> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc();

    @Query(value = """
            SELECT *
            FROM guides g
            WHERE g.guide_type = :guideType
              AND g.is_active = true
              AND g.deleted_at IS NULL
              AND (:patchVersion IS NULL OR g.patch_version = :patchVersion)
              AND (
                    :query IS NULL
                    OR LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(g.summary) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(g.target_key) LIKE LOWER(CONCAT('%', :query, '%'))
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
