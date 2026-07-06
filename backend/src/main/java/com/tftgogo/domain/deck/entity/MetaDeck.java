package com.tftgogo.domain.deck.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "meta_decks",
    uniqueConstraints = { @UniqueConstraint(columnNames = {"signature", "rank_filter", "patch_version"}) }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetaDeck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 조합 식별 키 - (signature, rank_filter) 복합 유니크
    @Column(nullable = false, length = 255)
    private String signature;

    @Enumerated(EnumType.STRING)
    @Column(name = "rank_filter", nullable = false, length = 20)
    private RankFilter rankFilter;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "patch_version", nullable = false, length = 20)
    private String patchVersion;

    // ENUM('S','A','B','C','D')
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

    // 집계에 사용된 데이터 수집 시작 날짜 (최근 N일 기준)
    @Column(name = "data_start_date")
    private LocalDate dataStartDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // N+1 방지: 다중 @OneToMany join fetch는 MultipleBagFetchException/카테시안 곱
    // 위험이 있어 대신 @BatchSize로 부모 엔티티 여러 건의 컬렉션을 IN절 배치 조회한다.
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "metaDeck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeckUnit> units = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "metaDeck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeckTrait> traits = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "metaDeck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HeroAugment> heroAugments = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "metaDeck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtifactStat> artifactStats = new ArrayList<>();

    @Builder
    public MetaDeck(String signature, RankFilter rankFilter, String name, String patchVersion,
                    String tier, double playRate, double winRate, double top4Rate,
                    double avgPlacement, int sampleSize, LocalDate dataStartDate) {
        this.signature = signature;
        this.rankFilter = rankFilter;
        this.name = name;
        this.patchVersion = patchVersion;
        this.tier = tier;
        this.playRate = playRate;
        this.winRate = winRate;
        this.top4Rate = top4Rate;
        this.avgPlacement = avgPlacement;
        this.sampleSize = sampleSize;
        this.dataStartDate = dataStartDate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String tier, double playRate, double winRate,
                       double top4Rate, double avgPlacement, int sampleSize,
                       String patchVersion, LocalDate dataStartDate) {
        this.tier = tier;
        this.playRate = playRate;
        this.winRate = winRate;
        this.top4Rate = top4Rate;
        this.avgPlacement = avgPlacement;
        this.sampleSize = sampleSize;
        this.patchVersion = patchVersion;
        this.dataStartDate = dataStartDate;
        this.updatedAt = LocalDateTime.now();
    }
}
