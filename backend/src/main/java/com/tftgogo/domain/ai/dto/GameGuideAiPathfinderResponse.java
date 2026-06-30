package com.tftgogo.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameGuideAiPathfinderResponse {

    private String title;
    private String summary;
    @JsonAlias("core_concepts")
    private List<String> coreConcepts;
    @JsonAlias("phase_plan")
    private List<PhasePlanDto> phasePlan;
    @JsonAlias("recommended_refs")
    private List<RecommendedRefDto> recommendedRefs;
    @JsonAlias("avoid_mistakes")
    private List<String> avoidMistakes;
    @JsonAlias("source_refs")
    private List<GuideRefDto> sourceRefs;
    private List<String> limitations;
    @JsonAlias("is_fallback")
    @JsonProperty("isFallback")
    private boolean fallback;

    public static GameGuideAiPathfinderResponse of(
            String title,
            String summary,
            List<String> coreConcepts,
            List<PhasePlanDto> phasePlan,
            List<RecommendedRefDto> recommendedRefs,
            List<String> avoidMistakes,
            List<GuideRefDto> sourceRefs,
            List<String> limitations,
            boolean isFallback
    ) {
        return new GameGuideAiPathfinderResponse(
                title,
                summary,
                coreConcepts,
                phasePlan,
                recommendedRefs,
                avoidMistakes,
                sourceRefs,
                limitations,
                isFallback
        );
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhasePlanDto {
        private String phase;
        private String title;
        private String description;
        @JsonAlias("guide_refs")
        private List<GuideRefDto> guideRefs;

        public static PhasePlanDto of(String phase, String title, String description, List<GuideRefDto> guideRefs) {
            return new PhasePlanDto(phase, title, description, guideRefs);
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GuideRefDto {
        @JsonAlias("guide_type")
        private String guideType;
        @JsonAlias("target_key")
        private String targetKey;
        private String name;

        public static GuideRefDto of(String guideType, String targetKey, String name) {
            return new GuideRefDto(guideType, targetKey, name);
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecommendedRefDto {
        @JsonAlias("guide_type")
        private String guideType;
        @JsonAlias("target_key")
        private String targetKey;
        private String name;
        private String reason;

        public static RecommendedRefDto of(String guideType, String targetKey, String name, String reason) {
            return new RecommendedRefDto(guideType, targetKey, name, reason);
        }
    }
}
