package com.tftgogo.domain.deck.repository;

import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.domain.deck.entity.RankFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MetaDeckRepository extends JpaRepository<MetaDeck, Long> {

    // patchVersion에는 매핑 적용 전 원본 client version이 저장된다 (표시용 패치 번호는 조회 시점에 매핑을 적용해 계산).
    Optional<MetaDeck> findBySignatureAndRankFilterAndPatchVersion(
            String signature, RankFilter rankFilter, String patchVersion);

    List<MetaDeck> findAllByRankFilter(RankFilter rankFilter);

    List<MetaDeck> findAllByRankFilterAndPatchVersion(RankFilter rankFilter, String patchVersion);

    @Query("SELECT COUNT(DISTINCT d.rankFilter) FROM MetaDeck d WHERE d.dataStartDate = :dataStartDate")
    long countAggregatedRankFiltersByDataStartDate(@Param("dataStartDate") java.time.LocalDate dataStartDate);

    // #134: default_batch_fetch_size로 N+1 제어 (application.yml 설정)
    // 다중 @OneToMany List 동시 join fetch → MultipleBagFetchException + 카테시안 곱 위험으로 @EntityGraph 미사용
    // 표시용 패치 하나가 여러 원본 client version(patchVersion)에 매핑될 수 있어 IN 조건으로 조회한다.
    @Query("SELECT d FROM MetaDeck d WHERE d.rankFilter = :rankFilter AND d.patchVersion IN :patchVersions AND d.playRate >= :minPlayRate ORDER BY d.playRate DESC")
    List<MetaDeck> findMetaDecksByPickRateIn(
            @Param("rankFilter") RankFilter rankFilter,
            @Param("patchVersions") Collection<String> patchVersions,
            @Param("minPlayRate") double minPlayRate);

    // 관리자 페이지: 최신 패치의 전체 덱 목록 (표시용 패치 하나에 대응하는 원본 client version들 기준 조회)
    List<MetaDeck> findByRankFilterAndPatchVersionIn(RankFilter rankFilter, Collection<String> patchVersions);

    @Query("SELECT DISTINCT d.patchVersion FROM MetaDeck d WHERE d.rankFilter = :rankFilter")
    List<String> findDistinctPatchVersionsByRankFilter(@Param("rankFilter") RankFilter rankFilter);
}
