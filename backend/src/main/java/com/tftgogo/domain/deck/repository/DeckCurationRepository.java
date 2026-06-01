package com.tftgogo.domain.deck.repository;

import com.tftgogo.domain.deck.entity.DeckCuration;
import com.tftgogo.domain.deck.entity.RankFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeckCurationRepository extends JpaRepository<DeckCuration, Long> {

    List<DeckCuration> findByRankFilter(RankFilter rankFilter);

    Optional<DeckCuration> findBySignatureAndRankFilter(String signature, RankFilter rankFilter);
}
