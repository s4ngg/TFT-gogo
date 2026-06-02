package com.tftgogo.domain.deck.repository;

import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.domain.deck.entity.RankFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MetaDeckRepository extends JpaRepository<MetaDeck, Long> {

    Optional<MetaDeck> findBySignatureAndRankFilterAndPatchVersion(
            String signature, RankFilter rankFilter, String patchVersion);

    List<MetaDeck> findAllByRankFilter(RankFilter rankFilter);

    List<MetaDeck> findAllByRankFilterAndPatchVersion(RankFilter rankFilter, String patchVersion);

    @Query("SELECT COUNT(DISTINCT d.rankFilter) FROM MetaDeck d WHERE d.dataStartDate = :dataStartDate")
    long countAggregatedRankFiltersByDataStartDate(@Param("dataStartDate") java.time.LocalDate dataStartDate);

    // 선택률 기준 내림차순 정렬 + 최소 선택률 필터 (DB 레벨)
    @Query("SELECT d FROM MetaDeck d WHERE d.rankFilter = :rankFilter AND d.patchVersion = :patchVersion AND d.playRate >= :minPlayRate ORDER BY d.playRate DESC")
    List<MetaDeck> findMetaDecksByPickRate(
            @Param("rankFilter") RankFilter rankFilter,
            @Param("patchVersion") String patchVersion,
            @Param("minPlayRate") double minPlayRate);

    // 관리자 페이지: 최신 패치의 전체 덱 목록
    List<MetaDeck> findByRankFilterAndPatchVersion(RankFilter rankFilter, String patchVersion);

    @Query("SELECT MAX(d.patchVersion) FROM MetaDeck d WHERE d.rankFilter = :rankFilter")
    Optional<String> findLatestPatchVersion(@Param("rankFilter") RankFilter rankFilter);

    @Query("SELECT DISTINCT d.patchVersion FROM MetaDeck d WHERE d.rankFilter = :rankFilter")
    List<String> findDistinctPatchVersionsByRankFilter(@Param("rankFilter") RankFilter rankFilter);
}
