package com.tftgogo.domain.deck.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meta_deck_trait")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetaDeckTrait {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_deck_id", nullable = false)
    private MetaDeck metaDeck;

    @Column(nullable = false)
    private String traitId;         // Set13_Challenger 형식

    @Column(nullable = false)
    private String traitName;       // Challenger

    private int unitCount;          // 활성 유닛 수

    private String tone;            // gold / silver / bronze / prismatic

    private String iconUrl;

    @Builder
    public MetaDeckTrait(MetaDeck metaDeck, String traitId, String traitName,
                          int unitCount, String tone, String iconUrl) {
        this.metaDeck = metaDeck;
        this.traitId = traitId;
        this.traitName = traitName;
        this.unitCount = unitCount;
        this.tone = tone;
        this.iconUrl = iconUrl;
    }
}
