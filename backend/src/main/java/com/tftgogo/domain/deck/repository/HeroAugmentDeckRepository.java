package com.tftgogo.domain.deck.repository;

import com.tftgogo.domain.deck.entity.HeroAugmentDeck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeroAugmentDeckRepository extends JpaRepository<HeroAugmentDeck, Long> {
    List<HeroAugmentDeck> findAllByOrderBySortOrderAscIdAsc();
}
