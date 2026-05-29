package com.tftgogo.global.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeagueListDto {

    private String leagueId;
    private String tier;
    private List<LeagueEntryDto> entries;
}
