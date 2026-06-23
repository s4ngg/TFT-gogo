package com.tftgogo.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    @NotEmpty
    @Size(max = 20)
    @Valid
    private List<MessageDto> messages;

    @Valid
    private ContextDto context;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageDto {
        @NotBlank
        @Size(max = 10)
        private String role;

        @NotBlank
        @Size(max = 2000)
        private String content;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextDto {
        @JsonProperty("summoner_name")
        @JsonAlias("summonerName")
        @Size(max = 100)
        private String summonerName;

        @JsonProperty("tag_line")
        @JsonAlias("tagLine")
        @Size(max = 10)
        private String tagLine;

        @JsonProperty("stats_summary")
        @JsonAlias("statsSummary")
        @Size(max = 500)
        private String statsSummary;

        @JsonProperty("good_traits")
        @JsonAlias("goodTraits")
        @Size(max = 20)
        private List<@Size(max = 100) String> goodTraits;

        @JsonProperty("bad_traits")
        @JsonAlias("badTraits")
        @Size(max = 20)
        private List<@Size(max = 100) String> badTraits;

        @JsonProperty("recent_matches")
        @JsonAlias("recentMatches")
        @Size(max = 5000)
        private String recentMatches;

        @JsonProperty("top_champions")
        @JsonAlias("topChampions")
        @Size(max = 20)
        private List<@Size(max = 100) String> topChampions;
    }
}
