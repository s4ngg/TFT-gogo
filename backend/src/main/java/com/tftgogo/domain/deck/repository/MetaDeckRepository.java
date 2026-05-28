package com.tftgogo.domain.deck.repository;

import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.domain.deck.entity.RankFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetaDeckRepository extends JpaRepository<MetaDeck, Long> {

    Optional<MetaDeck> findBySignatureAndRankFilter(String signature, RankFilter rankFilter);

    // win_rate 내림차순 정렬 (DB 레벨)
    List<MetaDeck> findAllByRankFilterOrderByWinRateDesc(RankFilter rankFilter);
}
