package com.tftgogo.domain.deck.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meta_deck")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetaDeck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String signature;       // 조합 식별 키 (예: Challenger-6_Blaster-4)

    @Column(nullable = false)
    private String name;            // 덱 이름

    @Column(nullable = false)
    private String grade;           // S / A+ / A / B / C / D

    private int rank;               // 메타 순위

    private double winRate;         // 1등 비율
    private double top4Rate;        // TOP4 비율
    private double avgPlace;        // 평균 등수
    private double pickRate;        // 전체 대비 픽률

    private int sampleCount;        // 집계된 게임 수

    @OneToMany(mappedBy = "metaDeck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MetaDeckChampion> champions = new ArrayList<>();

    @OneToMany(mappedBy = "metaDeck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MetaDeckTrait> traits = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public MetaDeck(String signature, String name, String grade, int rank,
                    double winRate, double top4Rate, double avgPlace,
                    double pickRate, int sampleCount) {
        this.signature = signature;
        this.name = name;
        this.grade = grade;
        this.rank = rank;
        this.winRate = winRate;
        this.top4Rate = top4Rate;
        this.avgPlace = avgPlace;
        this.pickRate = pickRate;
        this.sampleCount = sampleCount;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String grade, int rank, double winRate, double top4Rate,
                       double avgPlace, double pickRate, int sampleCount) {
        this.grade = grade;
        this.rank = rank;
        this.winRate = winRate;
        this.top4Rate = top4Rate;
        this.avgPlace = avgPlace;
        this.pickRate = pickRate;
        this.sampleCount = sampleCount;
        this.updatedAt = LocalDateTime.now();
    }
}
