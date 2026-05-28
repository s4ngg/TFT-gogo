package com.tftgogo.domain.deck.dto.response;

import com.tftgogo.domain.deck.entity.DeckTrait;
import com.tftgogo.domain.deck.entity.DeckUnit;
import com.tftgogo.domain.deck.entity.MetaDeck;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MetaDeckResponse {

    private int rank;           // DB 미저장 - 정렬 순위로 동적 계산
    private String grade;       // tier 필드 매핑
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
        private int cost;
    }

    public static MetaDeckResponse from(MetaDeck deck, int rank) {
        List<TraitSummary> traits = deck.getTraits().stream()
                .sorted((a, b) -> Integer.compare(b.getNumUnits(), a.getNumUnits()))
                .map(t -> TraitSummary.builder()
                        .name(t.getTraitName())
                        .count(t.getNumUnits())
                        .iconUrl(t.getIconUrl())
                        .tone(t.getTone())
                        .build())
                .toList();

        List<ChampionSummary> champions = deck.getUnits().stream()
                .sorted((a, b) -> Boolean.compare(b.isCarry(), a.isCarry()))
                .map(u -> ChampionSummary.builder()
                        .name(u.getChampionName())
                        .imageUrl(buildChampionImageUrl(u.getCharacterId()))
                        .stars(u.getStarLevel())
                        .cost(u.getCost())
                        .build())
                .toList();

        return MetaDeckResponse.builder()
                .rank(rank)
                .grade(deck.getTier())
                .name(deck.getName())
                .winRate(String.format("%.1f%%", deck.getWinRate()))
                .top4(String.format("%.1f%%", deck.getTop4Rate()))
                .avgPlace(String.format("%.2f", deck.getAvgPlacement()))
                .pickRate(String.format("%.1f%%", deck.getPlayRate()))
                .traits(traits)
                .champions(champions)
                .build();
    }

    private static String buildChampionImageUrl(String characterId) {
        if (characterId == null || characterId.isBlank()) {
            throw new IllegalArgumentException("characterId가 비어 있습니다.");
        }
        String id = characterId.toLowerCase();
        return "https://raw.communitydragon.org/latest/game/assets/characters/"
                + id + "/hud/" + id + "_square.tft.png";
    }
}
