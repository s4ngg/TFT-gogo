package com.tftgogo.domain.match.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "league_points", nullable = false)
    private int leaguePoints;

    @Column(name = "wins", nullable = false)
    private int wins;

    @Column(name = "losses", nullable = false)
    private int losses;

    @Column(name = "cached_at", nullable = false)
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
