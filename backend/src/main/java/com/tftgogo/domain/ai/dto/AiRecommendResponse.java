package com.tftgogo.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * AI 서버(FastAPI) 응답을 그대로 매핑하는 DTO.
 * 필드명은 AI 서버의 snake_case를 @JsonProperty로 매핑.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiRecommendResponse {

    private RecentStats stats;

    @JsonProperty("good_traits")
    private List<TraitStat> goodTraits;

    @JsonProperty("bad_traits")
    private List<TraitStat> badTraits;

    private List<Object> augments;

    @JsonProperty("deck_reasons")
    private List<DeckReason> deckReasons;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecentStats {
        @JsonProperty("recent_games")
        private int recentGames;

        @JsonProperty("avg_place")
        private String avgPlace;

        @JsonProperty("top4_rate")
        private String top4Rate;

        @JsonProperty("win_rate")
        private String winRate;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TraitStat {
        private String name;

        @JsonProperty("icon_url")
        private String iconUrl;

        private String tone;
        private int count;
        private int games;

        @JsonProperty("avg_place")
        private String avgPlace;

        @JsonProperty("top4_rate")
        private String top4Rate;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeckReason {
        @JsonProperty("deck_rank")
        private int deckRank;

        @JsonProperty("is_patch_trend")
        private boolean isPatchTrend;

        private String reason;
    }
}
