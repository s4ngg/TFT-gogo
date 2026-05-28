package com.tftgogo.domain.deck.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hero_augments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HeroAugment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_decks_id", nullable = false)
    private MetaDeck metaDeck;

    @Column(name = "character_id", nullable = false, length = 60)
    private String characterId;

    @Column(name = "augment_id", nullable = false, length = 100)
    private String augmentId;

    @Column(name = "augment_name", nullable = false, length = 200)
    private String augmentName;

    @Column(name = "is_recommended", nullable = false)
    private boolean isRecommended = true;

    @Column(name = "win_rate", nullable = false)
    private double winRate;

    @Column(name = "top4_rate", nullable = false)
    private double top4Rate;

    @Column(name = "avg_placement", nullable = false)
    private double avgPlacement;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    public HeroAugment(MetaDeck metaDeck, String characterId, String augmentId,
                       String augmentName, boolean isRecommended, double winRate,
                       double top4Rate, double avgPlacement, int sortOrder) {
        this.metaDeck = metaDeck;
        this.characterId = characterId;
        this.augmentId = augmentId;
        this.augmentName = augmentName;
        this.isRecommended = isRecommended;
        this.winRate = winRate;
        this.top4Rate = top4Rate;
        this.avgPlacement = avgPlacement;
        this.sortOrder = sortOrder;
    }
}
