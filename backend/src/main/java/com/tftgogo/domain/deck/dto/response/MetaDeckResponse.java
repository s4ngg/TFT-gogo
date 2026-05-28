package com.tftgogo.domain.deck.dto.response;

import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.domain.deck.entity.MetaDeckChampion;
import com.tftgogo.domain.deck.entity.MetaDeckTrait;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MetaDeckResponse {

    private int rank;
    private String grade;
    private String name;
    private String winRate;
    private String top4;
    private String avgPlace;
    private String pickRate;
    private List<TraitSummary> traits;
    private List<ChampionSummary> champions;

    @Getter
    @Builder
    public static class TraitSummary {
        private String name;
        private int count;
        private String iconUrl;
        private String tone;
    }

    @Getter
    @Builder
    public static class ChampionSummary {
        private String name;
        private String imageUrl;
        private int stars;
    }

    public static MetaDeckResponse from(MetaDeck deck) {
        List<TraitSummary> traits = deck.getTraits().stream()
                .sorted((a, b) -> Integer.compare(b.getUnitCount(), a.getUnitCount()))
                .map(t -> TraitSummary.builder()
                        .name(t.getTraitName())
                        .count(t.getUnitCount())
                        .iconUrl(t.getIconUrl())
                        .tone(t.getTone())
                        .build())
                .toList();

        List<ChampionSummary> champions = deck.getChampions().stream()
                .sorted((a, b) -> Double.compare(b.getFrequency(), a.getFrequency()))
                .map(c -> ChampionSummary.builder()
                        .name(c.getChampionName())
                        .imageUrl(c.getImageUrl())
                        .stars(c.getStars())
                        .build())
                .toList();

        return MetaDeckResponse.builder()
                .rank(deck.getRank())
                .grade(deck.getGrade())
                .name(deck.getName())
                .winRate(String.format("%.1f%%", deck.getWinRate()))
                .top4(String.format("%.1f%%", deck.getTop4Rate()))
                .avgPlace(String.format("%.2f", deck.getAvgPlace()))
                .pickRate(String.format("%.1f%%", deck.getPickRate()))
                .traits(traits)
                .champions(champions)
                .build();
    }
}
