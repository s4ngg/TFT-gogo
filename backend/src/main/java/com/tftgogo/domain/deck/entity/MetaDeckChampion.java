package com.tftgogo.domain.deck.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meta_deck_champion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetaDeckChampion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_deck_id", nullable = false)
    private MetaDeck metaDeck;

    @Column(nullable = false)
    private String championId;      // TFT13_Jinx 형식

    @Column(nullable = false)
    private String championName;    // Jinx

    private String imageUrl;

    private int stars;              // 평균 별 등급 (2 or 3)

    private double frequency;       // 해당 덱에서 등장 빈도

    @Builder
    public MetaDeckChampion(MetaDeck metaDeck, String championId, String championName,
                             String imageUrl, int stars, double frequency) {
        this.metaDeck = metaDeck;
        this.championId = championId;
        this.championName = championName;
        this.imageUrl = imageUrl;
        this.stars = stars;
        this.frequency = frequency;
    }
}
