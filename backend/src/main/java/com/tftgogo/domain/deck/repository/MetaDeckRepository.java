package com.tftgogo.domain.deck.repository;

import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.domain.deck.entity.RankFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetaDeckRepository extends JpaRepository<MetaDeck, Long> {

    Optional<MetaDeck> findBySignatureAndRankFilterAndPatchVersion(
            String signature, RankFilter rankFilter, String patchVersion);

    List<MetaDeck> findAllByRankFilter(RankFilter rankFilter);

    // win_rate 내림차순 정렬 (DB 레벨)
    List<MetaDeck> findAllByRankFilterAndPatchVersionOrderByWinRateDesc(
            RankFilter rankFilter, String patchVersion);
}
