package com.tftgogo.domain.deck.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meta_decks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetaDeck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 조합 식별 키 - 집계 upsert에 사용 (ERD 외 내부 필드)
    @Column(unique = true)
    private String signature;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "patch_version", nullable = false, length = 20)
    private String patchVersion;

    // ENUM('S','A+','A','B','C','D')
    @Column(nullable = false, length = 5)
    private String tier;

    @Column(name = "play_rate", nullable = false)
    private double playRate;

    @Column(name = "win_rate", nullable = false)
    private double winRate;

    @Column(name = "top4_rate", nullable = false)
    private double top4Rate;

    @Column(name = "avg_placement", nullable = false)
    private double avgPlacement;

    @Column(name = "sample_size", nullable = false)
    private int sampleSize;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "metaDeck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeckUnit> units = new ArrayList<>();

    @OneToMany(mappedBy = "metaDeck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeckTrait> traits = new ArrayList<>();

    @OneToMany(mappedBy = "metaDeck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HeroAugment> heroAugments = new ArrayList<>();

    @OneToMany(mappedBy = "metaDeck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtifactStat> artifactStats = new ArrayList<>();

    @Builder
    public MetaDeck(String signature, String name, String patchVersion, String tier,
                    double playRate, double winRate, double top4Rate,
                    double avgPlacement, int sampleSize) {
        this.signature = signature;
        this.name = name;
        this.patchVersion = patchVersion;
        this.tier = tier;
        this.playRate = playRate;
        this.winRate = winRate;
        this.top4Rate = top4Rate;
        this.avgPlacement = avgPlacement;
        this.sampleSize = sampleSize;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String tier, double playRate, double winRate,
                       double top4Rate, double avgPlacement, int sampleSize, String patchVersion) {
        this.tier = tier;
        this.playRate = playRate;
        this.winRate = winRate;
        this.top4Rate = top4Rate;
        this.avgPlacement = avgPlacement;
        this.sampleSize = sampleSize;
        this.patchVersion = patchVersion;
        this.updatedAt = LocalDateTime.now();
    }
}
