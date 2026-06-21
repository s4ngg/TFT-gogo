package com.tftgogo.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    private List<MessageDto> messages;
    private ContextDto context;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageDto {
        private String role;
        private String content;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextDto {
        @JsonProperty("summoner_name")
        @JsonAlias("summonerName")
        private String summonerName;

        @JsonProperty("tag_line")
        @JsonAlias("tagLine")
        private String tagLine;

        @JsonProperty("stats_summary")
        @JsonAlias("statsSummary")
        private String statsSummary;

        @JsonProperty("good_traits")
        @JsonAlias("goodTraits")
        private List<String> goodTraits;

        @JsonProperty("bad_traits")
        @JsonAlias("badTraits")
        private List<String> badTraits;

        @JsonProperty("recent_matches")
        @JsonAlias("recentMatches")
        private String recentMatches;

        @JsonProperty("top_champions")
        @JsonAlias("topChampions")
        private List<String> topChampions;
    }
}
