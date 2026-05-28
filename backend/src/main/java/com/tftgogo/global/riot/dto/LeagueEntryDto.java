package com.tftgogo.global.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeagueEntryDto {

    private String summonerId;
    private String puuid;
    private String summonerName;
    private int leaguePoints;
    private int wins;
    private int losses;
}
