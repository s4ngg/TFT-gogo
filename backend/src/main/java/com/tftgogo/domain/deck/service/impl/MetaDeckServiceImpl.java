package com.tftgogo.domain.deck.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.deck.dto.response.MetaDeckListResponse;
import com.tftgogo.domain.deck.dto.response.MetaDeckResponse;
import com.tftgogo.domain.deck.entity.ArtifactStat;
import com.tftgogo.domain.deck.entity.DeckTrait;
import com.tftgogo.domain.deck.entity.DeckUnit;
import com.tftgogo.domain.deck.entity.HeroAugment;
import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.repository.MetaDeckRepository;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.global.riot.RiotApiClient;
import com.tftgogo.global.riot.dto.LeagueEntryDto;
import com.tftgogo.global.riot.dto.LeagueListDto;
import com.tftgogo.global.riot.dto.MatchDto;
import com.tftgogo.global.riot.dto.MatchDto.ParticipantDto;
import com.tftgogo.global.riot.dto.MatchDto.TraitDto;
import com.tftgogo.global.riot.dto.MatchDto.UnitDto;
import com.tftgogo.global.riot.util.TftAssetUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetaDeckServiceImpl implements MetaDeckService {

    private static final Logger logger = LogManager.getLogger(MetaDeckServiceImpl.class);

    private static final int MATCHES_PER_SUMMONER = 20;
    private static final int MIN_SAMPLE = 3;
    private static final int MIN_DETAIL_SAMPLE = 2;
    private static final int SIGNATURE_TRAIT_COUNT = 3;
    private static final long RATE_LIMIT_DELAY_MS = 1200L;
    private static final String UNKNOWN_PATCH_VERSION = "UNKNOWN";
    private static final String GLOBAL_AUGMENT_CHARACTER_ID = "GLOBAL";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern PATCH_VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+[a-zA-Z]?)");

    private final MetaDeckRepository metaDeckRepository;
    private final RiotApiClient riotApiClient;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional(readOnly = true)
    public MetaDeckListResponse getMetaDecks(RankFilter rankFilter) {
        String latestPatchVersion = findLatestPatchVersion(rankFilter);
        if (latestPatchVersion == null) {
            return MetaDeckListResponse.builder()
                    .patchVersion(null)
                    .rankFilter(rankFilter)
                    .decks(List.of())
                    .build();
        }

        List<MetaDeck> decks = metaDeckRepository
                .findAllByRankFilterAndPatchVersionOrderByWinRateDesc(
                        rankFilter, latestPatchVersion);
        AtomicInteger rank = new AtomicInteger(1);
        List<MetaDeckResponse> responses = decks.stream()
                .map(deck -> MetaDeckResponse.from(deck, rank.getAndIncrement()))
                .toList();

        return MetaDeckListResponse.builder()
                .patchVersion(latestPatchVersion)
                .rankFilter(rankFilter)
                .decks(responses)
                .build();
    }

    @Override
    public void aggregateAndSave() {
        logger.info("전체 랭크 구간 메타 덱 집계 시작");
        for (RankFilter rankFilter : RankFilter.values()) {
            logger.info("[{}] 집계 시작", rankFilter);
            aggregateForTier(rankFilter);
        }
        logger.info("전체 랭크 구간 집계 완료");
    }

    private void aggregateForTier(RankFilter rankFilter) {
        List<String> puuids = collectPuuidsForTier(rankFilter);
        if (puuids.isEmpty()) {
            logger.warn("[{}] 수집 가능한 PUUID 없음 - 집계 중단", rankFilter);
            return;
        }

        Map<String, PatchDeckStats> patchStatsMap = new HashMap<>();
        Set<String> processedMatchIds = new HashSet<>();

        for (String puuid : puuids) {
            try {
                List<String> matchIds = riotApiClient.getMatchIds(puuid, MATCHES_PER_SUMMONER);
                for (String matchId : matchIds) {
                    if (!processedMatchIds.add(matchId)) {
                        continue;
                    }

                    MatchDto match = riotApiClient.getMatch(matchId);
                    if (match == null || match.getInfo() == null) {
                        continue;
                    }

                    String patchVersion = normalizePatchVersion(match.getInfo().getGame_version());
                    PatchDeckStats patchStats = patchStatsMap.computeIfAbsent(
                            patchVersion, ignored -> new PatchDeckStats());
                    patchStats.totalParticipants += processMatch(match, patchStats.deckStatMap);

                    Thread.sleep(RATE_LIMIT_DELAY_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("[{}] 집계 중단 - 인터럽트 발생", rankFilter, e);
                return;
            } catch (Exception e) {
                logger.warn("[{}] puuid={} 매치 수집 실패 - 건너뜀", rankFilter, puuid, e);
            }
        }

        logger.info("[{}] uniqueMatches={} patchBuckets={}",
                rankFilter, processedMatchIds.size(), patchStatsMap.size());

        patchStatsMap.forEach((patchVersion, patchStats) -> {
            logger.info("[{}][{}] participants={}, deckCombinations={}",
                    rankFilter, patchVersion, patchStats.totalParticipants, patchStats.deckStatMap.size());
            persistDeckStats(patchStats.deckStatMap, patchStats.totalParticipants, rankFilter, patchVersion);
        });
    }

    public void persistDeckStats(
            Map<String, DeckStat> deckStatMap,
            int totalParticipants,
            RankFilter rankFilter,
            String patchVersion
    ) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(
                status -> saveDeckStats(deckStatMap, totalParticipants, rankFilter, patchVersion));
    }

    private List<String> collectPuuidsForTier(RankFilter rankFilter) {
        List<String> puuids = new ArrayList<>();
        try {
            switch (rankFilter) {
                // 각 티어당 소환사 50명 × 20게임 = 1000게임 기준
                case MASTER_PLUS -> {
                    addPuuids(puuids, riotApiClient.getChallenger(), 20);
                    addPuuids(puuids, riotApiClient.getGrandmaster(), 20);
                    addPuuids(puuids, riotApiClient.getMaster(), 10);
                }
                case DIAMOND_PLUS -> {
                    addPuuids(puuids, riotApiClient.getChallenger(), 5);
                    addPuuids(puuids, riotApiClient.getGrandmaster(), 5);
                    addPuuids(puuids, riotApiClient.getMaster(), 5);
                    addLeaguePuuids(puuids, "DIAMOND", "I", 15);
                    addLeaguePuuids(puuids, "DIAMOND", "II", 10);
                    addLeaguePuuids(puuids, "DIAMOND", "III", 5);
                    addLeaguePuuids(puuids, "DIAMOND", "IV", 5);
                }
                case EMERALD_PLUS -> {
                    addPuuids(puuids, riotApiClient.getChallenger(), 3);
                    addPuuids(puuids, riotApiClient.getGrandmaster(), 3);
                    addPuuids(puuids, riotApiClient.getMaster(), 4);
                    addLeaguePuuids(puuids, "DIAMOND", "I", 5);
                    addLeaguePuuids(puuids, "DIAMOND", "II", 5);
                    addLeaguePuuids(puuids, "DIAMOND", "III", 5);
                    addLeaguePuuids(puuids, "DIAMOND", "IV", 5);
                    addLeaguePuuids(puuids, "EMERALD", "I", 10);
                    addLeaguePuuids(puuids, "EMERALD", "II", 5);
                    addLeaguePuuids(puuids, "EMERALD", "III", 5);
                    addLeaguePuuids(puuids, "EMERALD", "IV", 5);
                }
            }
        } catch (Exception e) {
            logger.error("[{}] 소환사 목록 조회 실패", rankFilter, e);
        }
        return puuids;
    }

    private void addPuuids(List<String> puuids, LeagueListDto league, int limit) {
        if (league == null || league.getEntries() == null) {
            return;
        }
        league.getEntries().stream()
                .filter(entry -> entry.getPuuid() != null)
                .limit(limit)
                .forEach(entry -> puuids.add(entry.getPuuid()));
    }

    private void addLeaguePuuids(List<String> puuids, String tier, String division, int limit) {
        List<LeagueEntryDto> entries = riotApiClient.getLeagueEntries(tier, division, 1);
        entries.stream()
                .filter(entry -> entry.getPuuid() != null)
                .limit(limit)
                .forEach(entry -> puuids.add(entry.getPuuid()));
    }

    private int processMatch(MatchDto match, Map<String, DeckStat> deckStatMap) {
        if (match.getInfo().getParticipants() == null) {
            return 0;
        }

        int count = 0;
        for (ParticipantDto participant : match.getInfo().getParticipants()) {
            if (participant.getTraits() == null || participant.getUnits() == null) {
                continue;
            }

            List<TraitDto> activeTraits = participant.getTraits().stream()
                    .filter(trait -> trait.getStyle() > 0)
                    .sorted(Comparator.comparingInt(TraitDto::getNum_units).reversed()
                            .thenComparing(TraitDto::getName))
                    .toList();

            if (activeTraits.isEmpty()) {
                continue;
            }

            String signature = buildSignature(activeTraits);
            deckStatMap.computeIfAbsent(signature, ignored -> new DeckStat(activeTraits))
                    .record(participant);
            count++;
        }
        return count;
    }

    private String buildSignature(List<TraitDto> activeTraits) {
        return activeTraits.stream()
                .limit(SIGNATURE_TRAIT_COUNT)
                .map(trait -> trait.getName() + "-" + trait.getNum_units())
                .collect(Collectors.joining("_"));
    }

    private void saveDeckStats(
            Map<String, DeckStat> deckStatMap,
            int totalParticipants,
            RankFilter rankFilter,
            String patchVersion
    ) {
        if (totalParticipants == 0) {
            return;
        }

        List<Map.Entry<String, DeckStat>> ranked = deckStatMap.entrySet().stream()
                .filter(entry -> entry.getValue().count >= MIN_SAMPLE)
                .sorted((a, b) -> Double.compare(b.getValue().winRate(), a.getValue().winRate()))
                .toList();

        for (Map.Entry<String, DeckStat> entry : ranked) {
            DeckStat stat = entry.getValue();
            double playRate = (double) stat.count / totalParticipants * 100;
            String tier = assignTier(stat.winRate(), stat.top4Rate());
            String name = buildDeckName(stat.traits);

            MetaDeck deck = metaDeckRepository
                    .findBySignatureAndRankFilterAndPatchVersion(entry.getKey(), rankFilter, patchVersion)
                    .orElseGet(() -> MetaDeck.builder()
                            .signature(entry.getKey())
                            .rankFilter(rankFilter)
                            .name(name)
                            .patchVersion(patchVersion)
                            .tier(tier)
                            .playRate(playRate)
                            .winRate(stat.winRate())
                            .top4Rate(stat.top4Rate())
                            .avgPlacement(stat.avgPlacement())
                            .sampleSize(stat.count)
                            .build());

            if (deck.getId() != null) {
                deck.update(tier, playRate, stat.winRate(), stat.top4Rate(),
                        stat.avgPlacement(), stat.count, patchVersion);
                deck.getUnits().clear();
                deck.getTraits().clear();
                deck.getArtifactStats().clear();
                deck.getHeroAugments().clear();
            }

            metaDeckRepository.save(deck);
            saveDeckDetails(deck, stat, patchVersion);

            logger.info("[{}][{}] saved name={} tier={} winRate={}% sample={}",
                    rankFilter, patchVersion, name, tier, String.format("%.1f", stat.winRate()), stat.count);
        }

        logger.info("[{}][{}] aggregate complete: {} decks saved", rankFilter, patchVersion, ranked.size());
    }

    private void saveDeckDetails(MetaDeck deck, DeckStat stat, String patchVersion) {
        for (TraitDto trait : stat.traits) {
            String tone = switch (trait.getStyle()) {
                case 1 -> "bronze";
                case 2 -> "silver";
                case 3 -> "gold";
                case 4 -> "prismatic";
                default -> "bronze";
            };
            DeckTrait deckTrait = DeckTrait.builder()
                    .metaDeck(deck)
                    .traitId(trait.getName())
                    .traitName(extractName(trait.getName()))
                    .numUnits(trait.getNum_units())
                    .tone(tone)
                    .iconUrl(buildTraitIconUrl(trait.getName()))
                    .build();
            deck.getTraits().add(deckTrait);
        }

        // 집계는 이미 메모리에서 끝났고, 덱 상세 표시/저장에 필요한 상위 유닛만 남긴다.
        List<UnitStat> unitStats = stat.unitStats.values().stream()
                .sorted(Comparator.comparingInt(UnitStat::getCount).reversed())
                .limit(10)
                .toList();
        for (UnitStat unitStat : unitStats) {
            DeckUnit unit = DeckUnit.builder()
                    .metaDeck(deck)
                    .characterId(unitStat.characterId)
                    .championName(extractName(unitStat.characterId))
                    .cost(raritytoCost(unitStat.rarity))
                    .isCarry(unitStat.maxTier >= 2 && !unitStat.topItems().isEmpty())
                    .recommendedItems(toJson(unitStat.topItems()))
                    .starLevel(unitStat.maxTier)
                    .build();
            deck.getUnits().add(unit);
        }

        List<ItemStat> itemStats = stat.itemStats.values().stream()
                .filter(itemStat -> itemStat.count >= MIN_DETAIL_SAMPLE)
                .sorted(Comparator.comparingDouble(ItemStat::winRate).reversed())
                .toList();
        for (ItemStat itemStat : itemStats) {
            ArtifactStat artifactStat = ArtifactStat.builder()
                    .metaDeck(deck)
                    .patchVersion(patchVersion)
                    .itemId(itemStat.itemId)
                    .itemName(extractName(itemStat.itemId))
                    .playRate((double) itemStat.count / stat.count * 100)
                    .winRate(itemStat.winRate())
                    .top4Rate(itemStat.top4Rate())
                    .avgPlacement(itemStat.avgPlacement())
                    .placementDelta(stat.avgPlacement() - itemStat.avgPlacement())
                    .sampleSize(itemStat.count)
                    .build();
            deck.getArtifactStats().add(artifactStat);
        }

        AtomicInteger sortOrder = new AtomicInteger(1);
        List<AugmentStat> augmentStats = stat.augmentStats.values().stream()
                .filter(augmentStat -> augmentStat.count >= MIN_DETAIL_SAMPLE)
                .sorted(Comparator.comparingDouble(AugmentStat::winRate).reversed())
                .toList();
        for (AugmentStat augmentStat : augmentStats) {
            HeroAugment heroAugment = HeroAugment.builder()
                    .metaDeck(deck)
                    .characterId(GLOBAL_AUGMENT_CHARACTER_ID)
                    .augmentId(augmentStat.augmentId)
                    .augmentName(extractName(augmentStat.augmentId))
                    .isRecommended(sortOrder.get() <= 3)
                    .winRate(augmentStat.winRate())
                    .top4Rate(augmentStat.top4Rate())
                    .avgPlacement(augmentStat.avgPlacement())
                    .sortOrder(sortOrder.getAndIncrement())
                    .build();
            deck.getHeroAugments().add(heroAugment);
        }
    }

    private String extractName(String id) {
        int idx = id.lastIndexOf('_');
        return idx >= 0 ? id.substring(idx + 1) : id;
    }

    private String buildDeckName(List<TraitDto> traits) {
        return traits.stream()
                .limit(2)
                .map(trait -> extractName(trait.getName()))
                .collect(Collectors.joining(" "));
    }

    private String assignTier(double winRate, double top4Rate) {
        if (winRate >= 20) return "S";
        if (winRate >= 16) return "A+";
        if (winRate >= 13) return "A";
        if (top4Rate >= 55) return "B";
        if (top4Rate >= 45) return "C";
        return "D";
    }

    private int raritytoCost(int rarity) {
        return switch (rarity) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            case 4 -> 4;
            case 6 -> 5;
            default -> {
                logger.warn("알 수 없는 rarity 값 {} - 1코스트로 처리", rarity);
                yield 1;
            }
        };
    }

    private String buildTraitIconUrl(String traitId) {
        return TftAssetUrlBuilder.buildTraitIconUrl(traitId);
    }

    private String normalizePatchVersion(String gameVersion) {
        if (gameVersion == null || gameVersion.isBlank()) {
            return UNKNOWN_PATCH_VERSION;
        }

        Matcher matcher = PATCH_VERSION_PATTERN.matcher(gameVersion);
        return matcher.find() ? matcher.group(1) : UNKNOWN_PATCH_VERSION;
    }

    private String findLatestPatchVersion(RankFilter rankFilter) {
        return metaDeckRepository.findAllByRankFilter(rankFilter).stream()
                .map(MetaDeck::getPatchVersion)
                .filter(patchVersion -> !UNKNOWN_PATCH_VERSION.equals(patchVersion))
                .max(this::comparePatchVersions)
                .orElse(null);
    }

    private int comparePatchVersions(String left, String right) {
        String[] leftParts = left.split("\\.", 2);
        String[] rightParts = right.split("\\.", 2);
        int majorCompare = Integer.compare(parseLeadingNumber(leftParts[0]), parseLeadingNumber(rightParts[0]));
        if (majorCompare != 0) {
            return majorCompare;
        }

        String leftMinor = leftParts.length > 1 ? leftParts[1] : "0";
        String rightMinor = rightParts.length > 1 ? rightParts[1] : "0";
        int minorCompare = Integer.compare(parseLeadingNumber(leftMinor), parseLeadingNumber(rightMinor));
        if (minorCompare != 0) {
            return minorCompare;
        }

        return leftMinor.compareTo(rightMinor);
    }

    private int parseLeadingNumber(String value) {
        Matcher matcher = Pattern.compile("\\d+").matcher(value);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            logger.warn("recommendedItems JSON 직렬화 실패", e);
            return "[]";
        }
    }

    private static class PatchDeckStats {
        final Map<String, DeckStat> deckStatMap = new HashMap<>();
        int totalParticipants = 0;
    }

    private static class DeckStat {
        final List<TraitDto> traits;
        final Map<String, UnitStat> unitStats = new HashMap<>();
        final Map<String, ItemStat> itemStats = new HashMap<>();
        final Map<String, AugmentStat> augmentStats = new HashMap<>();
        int count = 0, wins = 0, top4 = 0;
        double totalPlace = 0;

        DeckStat(List<TraitDto> traits) {
            this.traits = traits;
        }

        void record(ParticipantDto participant) {
            int placement = participant.getPlacement();
            count++;
            totalPlace += placement;
            if (placement == 1) wins++;
            if (placement <= 4) top4++;

            participant.getUnits().forEach(unit -> {
                UnitStat unitStat = unitStats.computeIfAbsent(unit.getCharacter_id(), UnitStat::new);
                unitStat.record(unit);

                if (unit.getItemNames() != null) {
                    unit.getItemNames().stream()
                            .filter(itemName -> itemName != null && !itemName.isBlank())
                            .forEach(itemName -> {
                                unitStat.recordItem(itemName);
                                itemStats.computeIfAbsent(itemName, ItemStat::new).record(placement);
                            });
                }
            });

            if (participant.getAugments() != null) {
                participant.getAugments().stream()
                        .filter(augment -> augment != null && !augment.isBlank())
                        .forEach(augment -> augmentStats.computeIfAbsent(augment, AugmentStat::new)
                                .record(placement));
            }
        }

        double winRate() { return count == 0 ? 0 : (double) wins / count * 100; }
        double top4Rate() { return count == 0 ? 0 : (double) top4 / count * 100; }
        double avgPlacement() { return count == 0 ? 0 : totalPlace / count; }
    }

    private static class UnitStat {
        final String characterId;
        final Map<String, Integer> itemCounts = new HashMap<>();
        int count = 0;
        int rarity = 0;
        int maxTier = 1;

        UnitStat(String characterId) {
            this.characterId = characterId;
        }

        void record(UnitDto unit) {
            count++;
            rarity = unit.getRarity();
            maxTier = Math.max(maxTier, unit.getTier());
        }

        void recordItem(String itemName) {
            itemCounts.merge(itemName, 1, Integer::sum);
        }

        int getCount() {
            return count;
        }

        List<String> topItems() {
            return itemCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .toList();
        }
    }

    private static class PlacementStat {
        int count = 0, wins = 0, top4 = 0;
        double totalPlace = 0;

        void record(int placement) {
            count++;
            totalPlace += placement;
            if (placement == 1) wins++;
            if (placement <= 4) top4++;
        }

        double winRate() { return count == 0 ? 0 : (double) wins / count * 100; }
        double top4Rate() { return count == 0 ? 0 : (double) top4 / count * 100; }
        double avgPlacement() { return count == 0 ? 0 : totalPlace / count; }
    }

    private static class ItemStat extends PlacementStat {
        final String itemId;

        ItemStat(String itemId) {
            this.itemId = itemId;
        }
    }

    private static class AugmentStat extends PlacementStat {
        final String augmentId;

        AugmentStat(String augmentId) {
            this.augmentId = augmentId;
        }
    }
}
