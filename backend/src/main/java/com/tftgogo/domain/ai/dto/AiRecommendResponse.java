package com.tftgogo.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
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

    @JsonProperty(value = "good_traits", access = Access.WRITE_ONLY)
    private List<TraitStat> goodTraits;

    @JsonProperty(value = "bad_traits", access = Access.WRITE_ONLY)
    private List<TraitStat> badTraits;

    private List<Object> augments;

    @JsonProperty(value = "deck_reasons", access = Access.WRITE_ONLY)
    private List<DeckReason> deckReasons;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecentStats {
        @JsonProperty(value = "recent_games", access = Access.WRITE_ONLY)
        private int recentGames;

        @JsonProperty(value = "avg_place", access = Access.WRITE_ONLY)
        private String avgPlace;

        @JsonProperty(value = "top4_rate", access = Access.WRITE_ONLY)
        private String top4Rate;

        @JsonProperty(value = "win_rate", access = Access.WRITE_ONLY)
        private String winRate;

        @JsonProperty(value = "recent_placements", access = Access.WRITE_ONLY)
        private List<Integer> recentPlacements;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TraitStat {
        private String name;

        @JsonProperty(value = "icon_url", access = Access.WRITE_ONLY)
        private String iconUrl;

        private String tone;
        private int count;
        private int games;

        @JsonProperty(value = "avg_place", access = Access.WRITE_ONLY)
        private String avgPlace;

        @JsonProperty(value = "top4_rate", access = Access.WRITE_ONLY)
        private String top4Rate;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeckReason {
        @JsonProperty(value = "deck_rank", access = Access.WRITE_ONLY)
        private int deckRank;

        @JsonProperty(value = "is_patch_trend", access = Access.WRITE_ONLY)
        private boolean isPatchTrend;

        private String reason;
    }
}
