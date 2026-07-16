package com.tftgogo.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;

import java.util.List;

/**
 * AI 서버(FastAPI) 응답을 매핑하는 DTO.
 * @JsonAlias로 snake_case 역직렬화를 허용하고, Java 필드명(camelCase)으로 직렬화된다.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiRecommendResponse {

    private RecentStats stats;

    @JsonAlias("good_traits")
    private List<TraitStat> goodTraits;

    @JsonAlias("bad_traits")
    private List<TraitStat> badTraits;

    private List<Object> augments;

    @JsonAlias("deck_reasons")
    private List<DeckReason> deckReasons;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecentStats {
        @JsonAlias("recent_games")
        private int recentGames;

        @JsonAlias("avg_place")
        private String avgPlace;

        @JsonAlias("top4_rate")
        private String top4Rate;

        @JsonAlias("win_rate")
        private String winRate;

        @JsonAlias("recent_placements")
        private List<Integer> recentPlacements;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TraitStat {
        // ai-server가 CDragon trait key에서 뽑은 영문 suffix(예: "psyops")로 내려주는데,
        // AiRecommendService가 GuideTrait 기준 한글 이름으로 치환할 수 있도록 setter를 연다.
        @Setter
        private String name;

        @JsonAlias("icon_url")
        private String iconUrl;

        private String tone;
        private int count;
        private int games;

        @JsonAlias("avg_place")
        private String avgPlace;

        @JsonAlias("top4_rate")
        private String top4Rate;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeckReason {
        @JsonAlias("deck_rank")
        private int deckRank;

        // Lombok이 생성하는 isPatchTrend() getter는 Jackson이 patchTrend로 인식하므로 억제.
        // @JsonGetter로 출력 키를 isPatchTrend로 고정한다.
        @JsonAlias("is_patch_trend")
        @Getter(AccessLevel.NONE)
        private boolean isPatchTrend;

        private String reason;

        @JsonGetter("isPatchTrend")
        public boolean isPatchTrend() { return isPatchTrend; }
    }
}
