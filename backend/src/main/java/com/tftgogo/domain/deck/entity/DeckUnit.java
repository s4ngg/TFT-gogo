package com.tftgogo.domain.deck.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deck_units")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_decks_id", nullable = false)
    private MetaDeck metaDeck;

    @Column(name = "character_id", nullable = false, length = 60)
    private String characterId;         // TFT13_Jinx 형식

    @Column(name = "champion_name", nullable = false, length = 100)
    private String championName;

    @Column(nullable = false)
    private int cost;

    @Column(name = "is_carry", nullable = false)
    private boolean isCarry;

    // 추천 아이템 ID 목록 (JSON 배열로 저장)
    @Column(name = "recommended_items", columnDefinition = "JSON")
    private String recommendedItems;

    @Column(name = "star_level", nullable = false)
    private int starLevel;

    @Builder
    public DeckUnit(MetaDeck metaDeck, String characterId, String championName,
                    int cost, boolean isCarry, String recommendedItems, int starLevel) {
        this.metaDeck = metaDeck;
        this.characterId = characterId;
        this.championName = championName;
        this.cost = cost;
        this.isCarry = isCarry;
        this.recommendedItems = recommendedItems;
        this.starLevel = starLevel;
    }
}
