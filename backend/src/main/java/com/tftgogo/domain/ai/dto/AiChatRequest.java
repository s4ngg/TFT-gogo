package com.tftgogo.domain.ai.dto;

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
        private String summonerName;

        @JsonProperty("tag_line")
        private String tagLine;

        @JsonProperty("stats_summary")
        private String statsSummary;

        @JsonProperty("good_traits")
        private List<String> goodTraits;

        @JsonProperty("bad_traits")
        private List<String> badTraits;
    }
}
