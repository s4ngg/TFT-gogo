package com.tftgogo.domain.summoner.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cached_summoner",
        indexes = @Index(name = "idx_cached_summoner_name_tag", columnList = "gameName, tagLine"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CachedSummoner {

    @Id
    @Column(length = 100)
    private String puuid;

    @Column(nullable = false, length = 100)
    private String gameName;

    @Column(nullable = false, length = 50)
    private String tagLine;

    @Column(nullable = false)
    private int profileIconId;

    @Column(nullable = false)
    private long summonerLevel;

    @Column(nullable = false)
    private LocalDateTime cachedAt;

    @Builder
    public CachedSummoner(String puuid, String gameName, String tagLine,
                          int profileIconId, long summonerLevel, LocalDateTime cachedAt) {
        this.puuid = puuid;
        this.gameName = gameName;
        this.tagLine = tagLine;
        this.profileIconId = profileIconId;
        this.summonerLevel = summonerLevel;
        this.cachedAt = cachedAt;
    }
}
