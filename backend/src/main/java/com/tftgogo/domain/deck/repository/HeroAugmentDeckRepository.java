package com.tftgogo.domain.deck.repository;

import com.tftgogo.domain.deck.entity.HeroAugmentDeck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeroAugmentDeckRepository extends JpaRepository<HeroAugmentDeck, Long> {
}
