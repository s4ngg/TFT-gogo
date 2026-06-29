package com.tftgogo.domain.deck.dto.response;

import com.tftgogo.domain.deck.entity.HeroAugmentDeck;
import lombok.Getter;

@Getter
public class HeroAugmentDeckResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final String champions;
    private final String traits;
    private final String boardPositions;
    private final String heroAugments;
    private final boolean recommended;
    private final int sortOrder;
    private final String grade;

    private HeroAugmentDeckResponse(HeroAugmentDeck deck) {
        this.id = deck.getId();
        this.name = deck.getName();
        this.description = deck.getDescription();
        this.champions = deck.getChampions();
        this.traits = deck.getTraits();
        this.boardPositions = deck.getBoardPositions();
        this.heroAugments = deck.getHeroAugments();
        this.recommended = deck.isRecommended();
        this.sortOrder = deck.getSortOrder();
        this.grade = deck.getGrade();
    }

    public static HeroAugmentDeckResponse from(HeroAugmentDeck deck) {
        return new HeroAugmentDeckResponse(deck);
    }
}
