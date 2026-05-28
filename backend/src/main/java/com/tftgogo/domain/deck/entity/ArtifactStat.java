package com.tftgogo.domain.deck.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "artifact_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtifactStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_decks_id", nullable = false)
    private MetaDeck metaDeck;

    @Column(name = "patch_version", nullable = false, length = 20)
    private String patchVersion;

    @Column(name = "item_id", nullable = false, length = 100)
    private String itemId;              // CDragon Item ID

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "play_rate", nullable = false)
    private double playRate;            // 빈도수

    @Column(name = "win_rate", nullable = false)
    private double winRate;

    @Column(name = "top4_rate", nullable = false)
    private double top4Rate;

    @Column(name = "avg_placement", nullable = false)
    private double avgPlacement;

    @Column(name = "placement_delta", nullable = false)
    private double placementDelta;      // 등수 변화

    @Column(name = "sample_size", nullable = false)
    private int sampleSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public ArtifactStat(MetaDeck metaDeck, String patchVersion, String itemId, String itemName,
                        double playRate, double winRate, double top4Rate,
                        double avgPlacement, double placementDelta, int sampleSize) {
        this.metaDeck = metaDeck;
        this.patchVersion = patchVersion;
        this.itemId = itemId;
        this.itemName = itemName;
        this.playRate = playRate;
        this.winRate = winRate;
        this.top4Rate = top4Rate;
        this.avgPlacement = avgPlacement;
        this.placementDelta = placementDelta;
        this.sampleSize = sampleSize;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
