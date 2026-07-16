package com.tftgogo.domain.search.entity;

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
	@Column(name = "puuid", length = 100, nullable = false)
	private String puuid;

	@Column(name = "game_name", length = 100, nullable = false)
	private String gameName;

	@Column(name = "tag_line",length = 50, nullable = false)
	private String tagLine;

	@Column(name = "profile_icon_id", nullable = false)
	private int profileIconId;

	@Column(name = "summoner_level", nullable = false)
	private long summonerLevel;

	@Column(name = "cached_at", nullable = false)
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
