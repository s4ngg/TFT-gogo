package com.tftgogo.domain.match.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cached_rank")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CachedRank {

    @Id
    @Column(length = 100)
    private String puuid;

    @Column(length = 20)
    private String tier;

    @Column(name = "rank_value", length = 10)
    private String rank;

    private int leaguePoints;
    private int wins;
    private int losses;

    @Column(nullable = false)
    private LocalDateTime cachedAt;

    @Builder
    public CachedRank(String puuid, String tier, String rank,
                      int leaguePoints, int wins, int losses, LocalDateTime cachedAt) {
        this.puuid = puuid;
        this.tier = tier;
        this.rank = rank;
        this.leaguePoints = leaguePoints;
        this.wins = wins;
        this.losses = losses;
        this.cachedAt = cachedAt;
    }
}
