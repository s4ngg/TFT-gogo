package com.tftgogo.domain.search.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SummonerDetailResponse {
    private String puuid;
    private String gameName;
    private String tagLine;
    private int profileIconId;
    private long summonerLevel;
    private String tier;
    private String rank;
    private int leaguePoints;
    private int wins;
    private int losses;

    public static SummonerDetailResponse from(SummonerProfileResponse profile, RankInfoResponse rankInfo) {
        return SummonerDetailResponse.builder()
                .puuid(profile.getPuuid())
                .gameName(profile.getGameName())
                .tagLine(profile.getTagLine())
                .profileIconId(profile.getProfileIconId())
                .summonerLevel(profile.getSummonerLevel())
                .tier(rankInfo.isUnranked() ? null : rankInfo.getTier())
                .rank(rankInfo.isUnranked() ? null : rankInfo.getRank())
                .leaguePoints(rankInfo.isUnranked() ? 0 : rankInfo.getLeaguePoints())
                .wins(rankInfo.isUnranked() ? 0 : rankInfo.getWins())
                .losses(rankInfo.isUnranked() ? 0 : rankInfo.getLosses())
                .build();
    }
}
