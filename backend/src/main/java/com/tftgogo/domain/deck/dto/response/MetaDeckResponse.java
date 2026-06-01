package com.tftgogo.domain.deck.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.deck.entity.ArtifactStat;
import com.tftgogo.domain.deck.entity.HeroAugment;
import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.global.riot.util.TftAssetUrlBuilder;
import com.tftgogo.global.riot.util.TftShopUnitFilter;
import lombok.Builder;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;

@Getter
@Builder
public class MetaDeckResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private int rank;
    private String grade;
    private String name;
    private String winRate;
    private String top4;
    private String avgPlace;
    private String pickRate;
    private int sampleSize;
    private List<TraitSummary> traits;
    private List<ChampionSummary> champions;
    private List<AugmentSummary> topAugments;
    private List<ItemSummary> topItems;

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
        private List<String> recommendedItems;
    }

    @Getter
    @Builder
    public static class AugmentSummary {
        private String augmentId;
        private String augmentName;
        private String winRate;
        @JsonProperty("isRecommended")   // Boolean + @JsonProperty로 JSON 키를 명시적으로 고정
        private Boolean isRecommended;
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
        return from(deck, rank, null);
    }

    public static MetaDeckResponse from(MetaDeck deck, int rank,
            com.tftgogo.domain.deck.entity.DeckCuration curation) {
        // 관리자가 이름 지정한 경우 우선 사용
        String displayName = (curation != null
                && curation.getCustomName() != null
                && !curation.getCustomName().isBlank())
                ? curation.getCustomName()
                : deck.getName();
        List<TraitSummary> traits = deck.getTraits().stream()
                .sorted((a, b) -> Integer.compare(b.getNumUnits(), a.getNumUnits()))
                .map(trait -> TraitSummary.builder()
                        .name(trait.getTraitName())
                        .count(trait.getNumUnits())
                        .iconUrl(trait.getIconUrl())
                        .tone(trait.getTone())
                        .build())
                .toList();

        List<com.tftgogo.domain.deck.entity.DeckUnit> responseUnits = deck.getUnits().stream()
                .filter(unit -> TftShopUnitFilter.isShopUnit(unit.getCharacterId()))
                .sorted(Comparator.comparingInt(com.tftgogo.domain.deck.entity.DeckUnit::getCost)) // 1→2→3→4→5 코스트 오름차순
                .limit(12)
                .toList();
        List<String> carryUnitIds = responseUnits.stream()
                .filter(unit -> !parseRecommendedItems(unit.getRecommendedItems()).isEmpty())
                .sorted(Comparator.comparingInt(com.tftgogo.domain.deck.entity.DeckUnit::getCost).reversed())
                .limit(3)
                .map(com.tftgogo.domain.deck.entity.DeckUnit::getCharacterId)
                .toList();

        List<ChampionSummary> champions = responseUnits.stream()
                .map(unit -> ChampionSummary.builder()
                        .name(unit.getChampionName())
                        .imageUrl(TftAssetUrlBuilder.buildChampionImageUrl(unit.getCharacterId()))
                        .stars(displayStarLevel(unit.getCost(), unit.getStarLevel()))
                        .cost(unit.getCost())
                        .recommendedItems(carryUnitIds.contains(unit.getCharacterId())
                                ? parseRecommendedItems(unit.getRecommendedItems())
                                : List.of())
                        .build())
                .toList();

        List<AugmentSummary> topAugments = deck.getHeroAugments().stream()
                .sorted(Comparator.comparingInt(HeroAugment::getSortOrder))
                .limit(5)
                .map(augment -> AugmentSummary.builder()
                        .augmentId(augment.getAugmentId())
                        .augmentName(augment.getAugmentName())
                        .winRate(String.format("%.1f%%", augment.getWinRate()))
                        .isRecommended(augment.isRecommended())
                        .build())
                .toList();

        List<ItemSummary> topItems = deck.getArtifactStats().stream()
                .sorted(Comparator.comparingDouble(ArtifactStat::getWinRate).reversed())
                .limit(20)
                .map(stat -> ItemSummary.builder()
                        .itemId(stat.getItemId())
                        .itemName(stat.getItemName())
                        .playRate(String.format("%.1f%%", stat.getPlayRate()))
                        .winRate(String.format("%.1f%%", stat.getWinRate()))
                        .placementDelta(String.format("%+.2f", stat.getPlacementDelta()))
                        .build())
                .toList();

        return MetaDeckResponse.builder()
                .rank(rank)
                .grade(deck.getTier())
                .name(displayName)
                .winRate(String.format("%.1f%%", deck.getWinRate()))
                .top4(String.format("%.1f%%", deck.getTop4Rate()))
                .avgPlace(String.format("%.2f", deck.getAvgPlacement()))
                .pickRate(String.format("%.1f%%", deck.getPlayRate()))
                .sampleSize(deck.getSampleSize())
                .traits(traits)
                .champions(champions)
                .topAugments(topAugments)
                .topItems(topItems)
                .build();
    }

    private static int displayStarLevel(int cost, int starLevel) {
        int capped = Math.min(starLevel, 3); // TFT 최대 3성
        if (cost >= 4) {
            return Math.min(capped, 2); // 4~5코스트 최대 2성
        }
        return capped;
    }

    private static List<String> parseRecommendedItems(String json) {
        return parseJsonList(json).stream()
                .filter(MetaDeckResponse::isRecommendedItem)
                .limit(3)
                .toList();
    }

    private static List<String> parseJsonList(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) {
            return List.of();
        }

        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private static boolean isRecommendedItem(String itemId) {
        String normalized = itemId.toLowerCase();
        if (!normalized.startsWith("tft_item_")) {
            return false;
        }

        return !normalized.contains("emptybag")
                && !normalized.contains("radiant")
                && !normalized.contains("artifact")
                && !normalized.contains("ornn")
                && !normalized.contains("support")
                && !normalized.contains("emblem")
                && !normalized.contains("trait")
                && !normalized.contains("consumable")
                && !normalized.contains("temporary")
                && !normalized.endsWith("bfsword")
                && !normalized.endsWith("recurvebow")
                && !normalized.endsWith("needlesslylargerod")
                && !normalized.endsWith("tearofthegoddess")
                && !normalized.endsWith("chainvest")
                && !normalized.endsWith("negatroncloak")
                && !normalized.endsWith("giantsbelt")
                && !normalized.endsWith("sparringgloves")
                && !normalized.endsWith("spatula")
                && !normalized.endsWith("fryingpan");
    }
}
