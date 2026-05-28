package com.tftgogo.domain.deck.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.deck.entity.ArtifactStat;
import com.tftgogo.domain.deck.entity.DeckTrait;
import com.tftgogo.domain.deck.entity.DeckUnit;
import com.tftgogo.domain.deck.entity.HeroAugment;
import com.tftgogo.domain.deck.entity.MetaDeck;
import lombok.Builder;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Builder
public class MetaDeckResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private int rank;           // DB 미저장 - 정렬 순위로 동적 계산
    private String grade;       // tier 필드 매핑
    private String name;
    private String winRate;
    private String top4;
    private String avgPlace;
    private String pickRate;
    private List<TraitSummary> traits;
    private List<ChampionSummary> champions;
    private List<AugmentSummary> topAugments;   // 덱에서 자주 선택된 상위 증강
    private List<ItemSummary> topItems;          // 덱에서 승률 높은 상위 아이템

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
        private List<String> recommendedItems;   // DeckUnit.recommendedItems JSON 파싱
    }

    @Getter
    @Builder
    public static class AugmentSummary {
        private String augmentId;
        private String augmentName;
        private String winRate;
        private boolean isRecommended;
    }

    @Getter
    @Builder
    public static class ItemSummary {
        private String itemId;
        private String itemName;
        private String playRate;
        private String winRate;
        private String placementDelta;
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
                .limit(8)
                .map(u -> ChampionSummary.builder()
                        .name(u.getChampionName())
                        .imageUrl(buildChampionImageUrl(u.getCharacterId()))
                        .stars(u.getStarLevel())
                        .cost(u.getCost())
                        .recommendedItems(parseJsonList(u.getRecommendedItems()))
                        .build())
                .toList();

        List<AugmentSummary> topAugments = deck.getHeroAugments().stream()
                .sorted(Comparator.comparingInt(HeroAugment::getSortOrder))
                .limit(5)
                .map(a -> AugmentSummary.builder()
                        .augmentId(a.getAugmentId())
                        .augmentName(a.getAugmentName())
                        .winRate(String.format("%.1f%%", a.getWinRate()))
                        .isRecommended(a.isRecommended())
                        .build())
                .toList();

        List<ItemSummary> topItems = deck.getArtifactStats().stream()
                .sorted(Comparator.comparingDouble(ArtifactStat::getWinRate).reversed())
                .limit(5)
                .map(s -> ItemSummary.builder()
                        .itemId(s.getItemId())
                        .itemName(s.getItemName())
                        .playRate(String.format("%.1f%%", s.getPlayRate()))
                        .winRate(String.format("%.1f%%", s.getWinRate()))
                        .placementDelta(String.format("%+.2f", s.getPlacementDelta()))
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
                .topAugments(topAugments)
                .topItems(topItems)
                .build();
    }

    private static List<String> parseJsonList(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) return List.of();
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private static final Pattern SET_NUM_PATTERN = Pattern.compile("tft(\\d+)_");

    private static String buildChampionImageUrl(String characterId) {
        if (characterId == null || characterId.isBlank()) {
            throw new IllegalArgumentException("characterId가 비어 있습니다.");
        }
        String id = characterId.toLowerCase();
        Matcher m = SET_NUM_PATTERN.matcher(id);
        String setTag = m.find() ? "tft_set" + m.group(1) : "tft_set17";
        return "https://raw.communitydragon.org/latest/game/assets/characters/"
                + id + "/hud/" + id + "_square." + setTag + ".png";
    }
}
