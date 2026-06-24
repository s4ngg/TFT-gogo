package com.tftgogo.domain.deck.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deck_traits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckTrait {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_decks_id", nullable = false)
    private MetaDeck metaDeck;

    @Column(name = "trait_id", nullable = false, length = 60)
    private String traitId;             // Set13_Challenger 형식

    @Column(name = "trait_name", nullable = false, length = 100)
    private String traitName;

    @Column(name = "num_units", nullable = false)
    private int numUnits;

    // ERD 외 - 프론트 렌더링에 필요
    @Column(length = 30)
    private String tone;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Builder
    public DeckTrait(MetaDeck metaDeck, String traitId, String traitName,
                     int numUnits, String tone, String iconUrl) {
        this.metaDeck = metaDeck;
        this.traitId = traitId;
        this.traitName = traitName;
        this.numUnits = numUnits;
        this.tone = tone;
        this.iconUrl = iconUrl;
    }
}
