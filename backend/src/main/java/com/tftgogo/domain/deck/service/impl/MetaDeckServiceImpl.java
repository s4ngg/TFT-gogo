package com.tftgogo.domain.deck.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.global.config.CacheConfig;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.domain.deck.dto.response.MetaDeckListResponse;
import com.tftgogo.domain.deck.dto.response.MetaDeckResponse;
import com.tftgogo.domain.deck.entity.ArtifactStat;
import com.tftgogo.domain.deck.entity.ClientVersionPatchMapping;
import com.tftgogo.domain.deck.entity.DeckTrait;
import com.tftgogo.domain.deck.entity.DeckUnit;
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
import com.tftgogo.global.riot.util.TftShopUnitFilter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetaDeckServiceImpl implements MetaDeckService {

    private static final Logger logger = LogManager.getLogger(MetaDeckServiceImpl.class);

    /** 동시 집계 방지 lock */
    private final AtomicBoolean aggregating = new AtomicBoolean(false);

    private static final int MATCHES_PER_SUMMONER = 20;
    // 15 → 10: 데이터 부족 시 덱 종류가 너무 적어지는 문제 완화
    private static final int MIN_SAMPLE = 10;
    private static final int MIN_ITEM_SAMPLE = 1;
    // 덱 등장 횟수 대비 유닛 최소 출현 비율 — 이 미만이면 빌드업·필러 유닛으로 판단해 제외
    private static final double MIN_UNIT_FREQUENCY = 0.25;
    private static final int SIGNATURE_TRAIT_COUNT = 2;
    // 6유닛 미만 = 플레이어가 레벨 5 이하에서 탈락한 빌드업 조합 — 집계 대상 제외
    private static final int MIN_UNIT_COUNT = 6;
    // 트레잇 실버(2) 이상만 활성 시너지로 인정 / 2유닛 이상이어야 고유 특성(Unique) 제외
    private static final int MIN_TRAIT_STYLE = 2;
    private static final int MIN_TRAIT_UNITS = 2;
    private static final int CORE_UNIT_LIMIT = 7;
    private static final int MIN_CORE_UNIT_COUNT = 5;
    // 0.70/0.75 → 0.55/0.65: 같은 캐리에 필러·탱커 1~2개 차이인 변형 덱을 동일 아키타입으로 병합
    // (예: 정령족코르키+리븐 vs 정령족코르키+람머스 → 코어 5/7 공유 ≈ 0.56 → 병합 O)
    private static final double CORE_UNIT_SIMILARITY_THRESHOLD = 0.55;
    private static final double BOARD_UNIT_SIMILARITY_THRESHOLD = 0.65;
    // 동일 주요 캐리 + 보드 40% 이상 공유 → 같은 아키타입으로 병합 (트레이트 시그니처가 달라도)
    private static final double MIN_CARRY_BOARD_SIMILARITY = 0.40;
    private static final double S_TIER_RATIO = 0.10;
    private static final double A_TIER_RATIO = 0.20;
    private static final double B_TIER_RATIO = 0.30;
    private static final double C_TIER_RATIO = 0.25;
    private static final long RATE_LIMIT_DELAY_MS = 1200L;
    private static final int RECOMMENDED_CARRY_LIMIT = 3;
    private static final String UNKNOWN_PATCH_VERSION = "UNKNOWN";
    private static final ZoneId DATA_COLLECTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final Set<String> COMPONENT_ITEM_IDS = Set.of(
            "tft_item_bfsword",
            "tft_item_recurvebow",
            "tft_item_needlesslylargerod",
            "tft_item_tearofthegoddess",
            "tft_item_chainvest",
            "tft_item_negatroncloak",
            "tft_item_giantsbelt",
            "tft_item_sparringgloves",
            "tft_item_spatula",
            "tft_item_fryingpan"
    );
    // 선택률 최소 임계값(%) — 이 이상인 덱만 덱모음에 노출
    private static final double MIN_PLAY_RATE = 0.5;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern PATCH_VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+[a-zA-Z]?)");

    private final MetaDeckRepository metaDeckRepository;
    private final com.tftgogo.domain.deck.repository.DeckCurationRepository deckCurationRepository;
    private final com.tftgogo.domain.deck.repository.ClientVersionPatchMappingRepository clientVersionPatchMappingRepository;
    private final RiotApiClient riotApiClient;
    private final PlatformTransactionManager transactionManager;
    private final AsyncAggregationRunner asyncAggregationRunner;
    private final org.springframework.cache.CacheManager cacheManager;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.META_DECKS, key = "#rankFilter")
    public MetaDeckListResponse getMetaDecks(RankFilter rankFilter) {
        Optional<String> latestPatchOpt = findLatestPatchVersion(rankFilter);
        if (latestPatchOpt.isEmpty()) {
            return MetaDeckListResponse.builder()
                    .patchVersion(null)
                    .rankFilter(rankFilter)
                    .dataStartDate(null)
                    .decks(List.of())
                    .build();
        }
        String latestPatchVersion = latestPatchOpt.get();
        List<String> rawVersions = resolveRawVersionsForPatch(rankFilter, latestPatchVersion);

        // 선택률 기준 내림차순 정렬 + 최소 선택률 필터 적용
        List<MetaDeck> decks = metaDeckRepository
                .findMetaDecksByPickRateIn(rankFilter, rawVersions, MIN_PLAY_RATE);

        // 큐레이션 적용: customName 오버라이드, 숨김 처리, sortPriority 정렬
        Map<String, com.tftgogo.domain.deck.entity.DeckCuration> curationMap =
                deckCurationRepository.findByRankFilter(rankFilter).stream()
                        .collect(Collectors.toMap(
                                com.tftgogo.domain.deck.entity.DeckCuration::getSignature,
                                c -> c));

        AtomicInteger rank = new AtomicInteger(1);
        List<MetaDeckResponse> responses = decks.stream()
                .filter(deck -> {
                    var curation = curationMap.get(deck.getSignature());
                    return curation == null || !curation.isHidden();
                })
                .sorted((a, b) -> {
                    var ca = curationMap.get(a.getSignature());
                    var cb = curationMap.get(b.getSignature());
                    Integer pa = ca != null ? ca.getSortPriority() : null;
                    Integer pb = cb != null ? cb.getSortPriority() : null;
                    if (pa != null && pb != null) return Integer.compare(pa, pb);
                    if (pa != null) return -1;  // 우선순위 있는 쪽 먼저
                    if (pb != null) return 1;
                    return Double.compare(b.getPlayRate(), a.getPlayRate()); // 기본: pickRate 내림차순
                })
                .map(deck -> {
                    var curation = curationMap.get(deck.getSignature());
                    return MetaDeckResponse.from(deck, rank.getAndIncrement(), curation);
                })
                .toList();

        // 가장 오래된 dataStartDate를 응답에 포함 (수집 기간 표시용)
        LocalDate dataStartDate = decks.stream()
                .map(MetaDeck::getDataStartDate)
                .filter(d -> d != null)
                .min(LocalDate::compareTo)
                .orElse(null);

        return MetaDeckListResponse.builder()
                .patchVersion(latestPatchVersion)
                .rankFilter(rankFilter)
                .dataStartDate(dataStartDate)
                .decks(responses)
                .build();
    }

    @Override
    public void aggregateAndSave() {
        aggregateAndSave(LocalDate.now(DATA_COLLECTION_ZONE).minusDays(1));
    }

    @Override
    public void aggregateAndSave(LocalDate dataDate) {
        if (!aggregating.compareAndSet(false, true)) {
            logger.warn("집계 이미 실행 중 - 중복 요청 skip (date={})", dataDate);
            return;
        }
        try {
            logger.info("전체 랭크 구간 메타 덱 일일 집계 시작 - date={}", dataDate);
            for (RankFilter rankFilter : RankFilter.values()) {
                logger.info("[{}] 집계 시작 - date={}", rankFilter, dataDate);
                aggregateForTier(rankFilter, dataDate);
            }
            logger.info("전체 랭크 구간 메타 덱 일일 집계 완료 - date={}", dataDate);
        } finally {
            // 랭크별로 개별 TransactionTemplate 커밋을 사용하므로 일부 랭크 저장 후
            // 예외가 나도 이미 커밋된 변경이 반영되도록 finally에서 직접 clear한다.
            // (@CacheEvict는 기본 beforeInvocation=false라 예외 종료 시 무효화되지 않음)
            java.util.Optional.ofNullable(cacheManager.getCache(CacheConfig.META_DECKS))
                    .ifPresent(org.springframework.cache.Cache::clear);
            aggregating.set(false);
        }
    }

    @Override
    public CompletableFuture<Void> aggregateAndSaveAsync(LocalDate dataDate) {
        if (!aggregating.compareAndSet(false, true)) {
            logger.warn("집계 이미 실행 중 - 중복 요청 거부 (date={})", dataDate);
            throw new BusinessException(ErrorCode.AGGREGATION_ALREADY_RUNNING);
        }
        try {
            return asyncAggregationRunner.run(() -> {
                try {
                    logger.info("전체 랭크 구간 메타 덱 일일 집계 시작 - date={}", dataDate);
                    for (RankFilter rankFilter : RankFilter.values()) {
                        logger.info("[{}] 집계 시작 - date={}", rankFilter, dataDate);
                        aggregateForTier(rankFilter, dataDate);
                    }
                    logger.info("전체 랭크 구간 메타 덱 일일 집계 완료 - date={}", dataDate);
                } finally {
                    // 비동기 람다 내부라 @CacheEvict 프록시를 타지 않으므로 직접 무효화한다.
                    java.util.Optional.ofNullable(cacheManager.getCache(CacheConfig.META_DECKS))
                            .ifPresent(org.springframework.cache.Cache::clear);
                    aggregating.set(false);
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) { // AbortPolicy가 던지는 예외만 처리
            aggregating.set(false);
            logger.error("집계 작업 등록 실패 - executor 큐 가득참 (date={})", dataDate, e);
            throw new BusinessException(ErrorCode.AGGREGATION_QUEUE_FULL, e);
        }
    }

    private void aggregateForTier(RankFilter rankFilter, LocalDate dataDate) {
        List<String> puuids = collectPuuidsForTier(rankFilter);
        if (puuids.isEmpty()) {
            logger.warn("[{}] 수집 가능한 PUUID 없음 - 집계 중단", rankFilter);
            return;
        }

        long startTimeSeconds = dataDate.atStartOfDay(DATA_COLLECTION_ZONE).toEpochSecond();
        long endTimeSeconds = dataDate.plusDays(1).atStartOfDay(DATA_COLLECTION_ZONE).toEpochSecond();
        logger.info("[{}] 수집 기간: {} 00:00 ~ {} 00:00 ({})",
                rankFilter, dataDate, dataDate.plusDays(1), DATA_COLLECTION_ZONE);

        Map<String, PatchDeckStats> patchStatsMap = new HashMap<>();
        Set<String> processedMatchIds = new HashSet<>();

        int puuidIndex = 0;
        for (String puuid : puuids) {
            puuidIndex++;
            try {
                List<String> matchIds = riotApiClient.getMatchIds(
                        puuid, MATCHES_PER_SUMMONER, startTimeSeconds, endTimeSeconds);
                logger.info("[{}] puuid {}/{} matchIds={} uniqueTotal={}",
                        rankFilter, puuidIndex, puuids.size(), matchIds.size(), processedMatchIds.size());
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
                            patchVersion, ignored -> new PatchDeckStats(dataDate));
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
            persistDeckStats(patchStats.deckStatMap, patchStats.totalParticipants, rankFilter, patchVersion, patchStats.dataStartDate);
        });
    }

    public void persistDeckStats(
            Map<String, DeckStat> deckStatMap,
            int totalParticipants,
            RankFilter rankFilter,
            String patchVersion,
            LocalDate dataStartDate
    ) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(
                status -> saveDeckStats(deckStatMap, totalParticipants, rankFilter, patchVersion, dataStartDate));
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

            // MIN_UNIT_COUNT 미만 = 초반 빌드업(전환 중) 조합 — 품질 저하 원인이므로 제외
            if (participant.getUnits().size() < MIN_UNIT_COUNT) {
                continue;
            }

            // MIN_TRAIT_STYLE(실버+) 이상이고 MIN_TRAIT_UNITS(2개+)인 트레잇만 활성 시너지로 인정
            List<TraitDto> activeTraits = participant.getTraits().stream()
                    .filter(trait -> trait.getStyle() >= MIN_TRAIT_STYLE && trait.getNum_units() >= MIN_TRAIT_UNITS)
                    .sorted(Comparator.comparingInt(TraitDto::getNum_units).reversed()
                            .thenComparing(TraitDto::getName))
                    .toList();

            if (activeTraits.isEmpty()) {
                continue;
            }

            DeckProfile profile = buildDeckProfile(activeTraits, participant.getUnits());
            DeckStat deckStat = findSimilarDeckStat(deckStatMap, profile).orElseGet(() -> {
                String signature = buildSignature(profile);
                DeckStat newStat = new DeckStat(activeTraits, profile);
                deckStatMap.put(signature, newStat);
                return newStat;
            });
            deckStat.record(participant);
            count++;
        }
        return count;
    }

    private DeckProfile buildDeckProfile(List<TraitDto> activeTraits, List<UnitDto> units) {
        return new DeckProfile(
                buildTraitSignature(activeTraits),
                buildCoreUnitIds(units),
                buildBoardUnitIds(units),
                buildPrimaryCarryId(units));
    }

    /** 아이템 2개 이상 장착 유닛 중 아이템 수 최다인 유닛 = 주요 캐리 */
    private String buildPrimaryCarryId(List<UnitDto> units) {
        return units.stream()
                .filter(u -> u.getItemNames() != null && u.getItemNames().size() >= 2)
                .max(Comparator.comparingInt(u -> u.getItemNames().size()))
                .map(UnitDto::getCharacter_id)
                .map(id -> id.toLowerCase(Locale.ROOT))
                .orElse(null);
    }

    private String buildTraitSignature(List<TraitDto> activeTraits) {
        // 시그니처 = 상위 2개 트레잇 이름만 사용 (num_units 제외)
        // → 같은 아키타입이 유닛 수만 다를 때 다른 덱으로 분류되는 중복 문제 해결
        return activeTraits.stream()
                .limit(SIGNATURE_TRAIT_COUNT)
                .map(TraitDto::getName)
                .collect(Collectors.joining("_"));
    }

    private Set<String> buildCoreUnitIds(List<UnitDto> units) {
        List<UnitDto> sortedUnits = units.stream()
                .filter(unit -> unit.getCharacter_id() != null && !unit.getCharacter_id().isBlank())
                .sorted(Comparator.comparingInt(UnitDto::getRarity).reversed()
                        .thenComparing(Comparator.comparingInt(UnitDto::getTier).reversed())
                        .thenComparing(UnitDto::getCharacter_id))
                .toList();

        // #137: TreeSet으로 알파벳 정렬 보장 → 집계 순서와 무관하게 동일 signature 생성
        Set<String> coreUnitIds = sortedUnits.stream()
                .filter(this::isCoreUnit)
                .limit(CORE_UNIT_LIMIT)
                .map(UnitDto::getCharacter_id)
                .collect(Collectors.toCollection(TreeSet::new));

        if (coreUnitIds.size() < MIN_CORE_UNIT_COUNT) {
            sortedUnits.stream()
                    .limit(CORE_UNIT_LIMIT)
                    .map(UnitDto::getCharacter_id)
                    .forEach(coreUnitIds::add);
        }

        return coreUnitIds;
    }

    private Set<String> buildBoardUnitIds(List<UnitDto> units) {
        return units.stream()
                .map(UnitDto::getCharacter_id)
                .filter(characterId -> characterId != null && !characterId.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private boolean isCoreUnit(UnitDto unit) {
        boolean hasItems = unit.getItemNames() != null && unit.getItemNames().stream()
                .anyMatch(itemName -> itemName != null && !itemName.isBlank());
        return unit.getRarity() >= 2 || unit.getTier() >= 2 || hasItems;
    }

    private Optional<DeckStat> findSimilarDeckStat(Map<String, DeckStat> deckStatMap, DeckProfile profile) {
        return deckStatMap.values().stream()
                .filter(stat -> hasSimilarDeckProfile(stat.profile, profile))
                .findFirst();
    }

    private boolean hasSimilarDeckProfile(DeckProfile left, DeckProfile right) {
        // 1. 트레이트 시그니처 동일 + 코어 유닛 유사 → 동일 아키타입
        if (left.traitSignature.equals(right.traitSignature)
                && hasSimilarCoreUnits(left.coreUnitIds, right.coreUnitIds)) {
            return true;
        }

        // 2. 주요 캐리가 같고 보드 40% 이상 겹침 → flex 유닛 차이로 트레이트만 달라진 변형
        //    예: 싸움꾼_마스터이 vs N.O.V.A._마스터이 → 마스터이 기반 같은 아키타입
        if (left.primaryCarryId != null
                && left.primaryCarryId.equals(right.primaryCarryId)
                && jaccardSimilarity(left.boardUnitIds, right.boardUnitIds) >= MIN_CARRY_BOARD_SIMILARITY) {
            return true;
        }

        // 3. 보드 유닛 Jaccard 유사도 (캐리 무관)
        return jaccardSimilarity(left.boardUnitIds, right.boardUnitIds) >= BOARD_UNIT_SIMILARITY_THRESHOLD;
    }

    private boolean hasSimilarCoreUnits(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }

        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);

        int smallerCoreSize = Math.min(left.size(), right.size());
        if (intersection.size() < Math.min(MIN_CORE_UNIT_COUNT, smallerCoreSize)) {
            return false;
        }

        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        double similarity = (double) intersection.size() / union.size();
        return similarity >= CORE_UNIT_SIMILARITY_THRESHOLD;
    }

    private double jaccardSimilarity(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }

        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);

        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        return (double) intersection.size() / union.size();
    }

    private String buildSignature(DeckProfile profile) {
        return profile.traitSignature + "::" + String.join("_", profile.coreUnitIds);
    }

    /**
     * #137: 병합된 전체 샘플의 unitFrequency 기준으로 core unit을 재선정해 canonical signature 생성.
     * 최초 관측 샘플 기준 buildSignature()와 달리 집계 순서에 무관하게 동일한 signature 보장.
     */
    private String buildCanonicalSignature(DeckStat stat) {
        // 빈도 내림차순 → rarity/tier 순 정렬로 core unit 후보 선정
        List<String> frequencySortedIds = stat.unitFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .toList();

        Set<String> canonicalCoreIds = frequencySortedIds.stream()
                .limit(CORE_UNIT_LIMIT)
                .collect(Collectors.toCollection(TreeSet::new));

        if (canonicalCoreIds.size() < MIN_CORE_UNIT_COUNT) {
            frequencySortedIds.forEach(canonicalCoreIds::add);
        }

        return stat.profile.traitSignature + "::" + String.join("_", canonicalCoreIds);
    }

    private void saveDeckStats(
            Map<String, DeckStat> deckStatMap,
            int totalParticipants,
            RankFilter rankFilter,
            String patchVersion,
            LocalDate dataStartDate
    ) {
        if (totalParticipants == 0) {
            return;
        }

        List<Map.Entry<String, DeckStat>> ranked = deckStatMap.entrySet().stream()
                .filter(entry -> entry.getValue().count >= MIN_SAMPLE)
                .sorted((a, b) -> {
                    double leftPlayRate = (double) a.getValue().count / totalParticipants * 100;
                    double rightPlayRate = (double) b.getValue().count / totalParticipants * 100;
                    int playRateCompare = Double.compare(rightPlayRate, leftPlayRate);
                    if (playRateCompare != 0) {
                        return playRateCompare;
                    }
                    return Double.compare(a.getValue().avgPlacement(), b.getValue().avgPlacement());
                })
                .toList();

        // #132: 집계 결과가 없으면 기존 데이터를 보호하고 저장 건너뜀
        if (ranked.isEmpty()) {
            logger.warn("[{}][{}] 집계 결과 없음 (totalParticipants={}) — 기존 데이터 유지",
                    rankFilter, patchVersion, totalParticipants);
            return;
        }

        refreshPatchDecks(rankFilter, patchVersion);

        for (int index = 0; index < ranked.size(); index++) {
            Map.Entry<String, DeckStat> entry = ranked.get(index);
            DeckStat stat = entry.getValue();
            double playRate = (double) stat.count / totalParticipants * 100;
            String tier = assignTierByRatio(index, ranked.size());
            String name = buildDeckName(stat.traits);

            // #137: 병합된 전체 샘플 기준 빈도로 core unit 재선정 → canonical signature 재계산
            String canonicalSignature = buildCanonicalSignature(stat);

            MetaDeck deck = metaDeckRepository
                    .findBySignatureAndRankFilterAndPatchVersion(canonicalSignature, rankFilter, patchVersion)
                    .orElseGet(() -> MetaDeck.builder()
                            .signature(canonicalSignature)
                            .rankFilter(rankFilter)
                            .name(name)
                            .patchVersion(patchVersion)
                            .tier(tier)
                            .playRate(playRate)
                            .winRate(stat.winRate())
                            .top4Rate(stat.top4Rate())
                            .avgPlacement(stat.avgPlacement())
                            .sampleSize(stat.count)
                            .dataStartDate(dataStartDate)
                            .build());

            if (deck.getId() != null) {
                deck.update(tier, playRate, stat.winRate(), stat.top4Rate(),
                        stat.avgPlacement(), stat.count, patchVersion, dataStartDate);
                deck.getUnits().clear();
                deck.getTraits().clear();
                deck.getArtifactStats().clear();
            }

            metaDeckRepository.save(deck);
            saveDeckDetails(deck, stat, patchVersion);

            logger.info("[{}][{}] saved name={} tier={} pickRate={}% winRate={}% sample={}",
                    rankFilter, patchVersion, name, tier,
                    String.format("%.2f", playRate), String.format("%.1f", stat.winRate()), stat.count);
        }

        logger.info("[{}][{}] aggregate complete: {} decks saved (minPlayRate={}%)",
                rankFilter, patchVersion, ranked.size(), MIN_PLAY_RATE);
    }

    private void refreshPatchDecks(RankFilter rankFilter, String patchVersion) {
        List<MetaDeck> existingDecks = metaDeckRepository.findAllByRankFilterAndPatchVersion(rankFilter, patchVersion);
        if (existingDecks.isEmpty()) {
            return;
        }

        metaDeckRepository.deleteAll(existingDecks);
        metaDeckRepository.flush();
        logger.info("[{}][{}] removed {} existing meta decks before refresh",
                rankFilter, patchVersion, existingDecks.size());
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

        // MIN_UNIT_FREQUENCY(25%) 미만 출현 유닛 = 빌드업·필러로 판단해 제외
        // stat.count=15 기준: minUnitFreq=3 → 4게임 이상 등장한 유닛만 포함
        int minUnitFreq = Math.max(1, (int) (stat.count * MIN_UNIT_FREQUENCY));
        List<UnitStat> unitStats = stat.unitStats.values().stream()
                .filter(u -> u.count >= minUnitFreq)
                .filter(this::isShopUnit)
                .sorted(Comparator.comparingInt(UnitStat::getCount).reversed())
                .limit(12)
                .toList();
        Set<String> carryUnitIds = unitStats.stream()
                .filter(unitStat -> !unitStat.topItems().isEmpty())
                .sorted(Comparator.comparingInt(UnitStat::itemCount).reversed()
                        .thenComparing(Comparator.comparingInt(UnitStat::getCount).reversed()))
                .limit(RECOMMENDED_CARRY_LIMIT)
                .map(unitStat -> unitStat.characterId)
                .collect(Collectors.toSet());
        for (UnitStat unitStat : unitStats) {
            boolean isCarry = carryUnitIds.contains(unitStat.characterId);
            DeckUnit unit = DeckUnit.builder()
                    .metaDeck(deck)
                    .characterId(unitStat.characterId)
                    .championName(extractName(unitStat.characterId))
                    .cost(raritytoCost(unitStat.rarity))
                    .isCarry(isCarry)
                    .recommendedItems(toJson(isCarry ? unitStat.topItems() : List.of()))
                    .starLevel(recommendedStarLevel(unitStat))
                    .build();
            deck.getUnits().add(unit);
        }

        List<ItemStat> itemStats = stat.itemStats.values().stream()
                .filter(itemStat -> itemStat.count >= MIN_ITEM_SAMPLE)
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

    private String assignTierByRatio(int index, int totalCount) {
        if (totalCount <= 0) {
            return "D";
        }

        int sLimit = ratioLimit(totalCount, S_TIER_RATIO);
        int aLimit = sLimit + ratioLimit(totalCount, A_TIER_RATIO);
        int bLimit = aLimit + ratioLimit(totalCount, B_TIER_RATIO);
        int cLimit = bLimit + ratioLimit(totalCount, C_TIER_RATIO);

        if (index < sLimit) return "S";
        if (index < aLimit) return "A";
        if (index < bLimit) return "B";
        if (index < cLimit) return "C";
        return "D";
    }

    private int ratioLimit(int totalCount, double ratio) {
        return Math.max(1, (int) Math.ceil(totalCount * ratio));
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

    private int recommendedStarLevel(UnitStat unitStat) {
        int cost = raritytoCost(unitStat.rarity);
        int tier = Math.min(unitStat.maxTier, 3); // TFT 최대 3성
        if (cost >= 4) {
            return Math.min(tier, 2); // 4~5코스트 최대 2성
        }
        return tier;
    }

    private String buildTraitIconUrl(String traitId) {
        return TftAssetUrlBuilder.buildTraitIconUrl(traitId);
    }

    // meta_decks에는 원본 client version을 그대로 저장한다.
    // client version → 표시용 패치 번호 변환은 조회 시점에 매핑 테이블을 참조해 계산한다 (#726 관련 소급 반영 문제 회피).
    private String normalizePatchVersion(String gameVersion) {
        if (gameVersion == null || gameVersion.isBlank()) {
            return UNKNOWN_PATCH_VERSION;
        }

        Matcher matcher = PATCH_VERSION_PATTERN.matcher(gameVersion);
        if (!matcher.find()) {
            return UNKNOWN_PATCH_VERSION;
        }

        return matcher.group(1);
    }

    @Override
    public Optional<String> findLatestPatchVersion(RankFilter rankFilter) {
        Map<String, String> clientVersionToPatch = buildClientVersionToPatchMap();
        return metaDeckRepository.findDistinctPatchVersionsByRankFilter(rankFilter).stream()
                .filter(rawVersion -> !UNKNOWN_PATCH_VERSION.equals(rawVersion))
                .map(rawVersion -> resolveDisplayPatchVersion(rawVersion, clientVersionToPatch))
                .max(this::comparePatchVersions);
    }

    @Override
    public List<String> resolveRawVersionsForPatch(RankFilter rankFilter, String displayPatchVersion) {
        Map<String, String> clientVersionToPatch = buildClientVersionToPatchMap();
        return metaDeckRepository.findDistinctPatchVersionsByRankFilter(rankFilter).stream()
                .filter(rawVersion -> resolveDisplayPatchVersion(rawVersion, clientVersionToPatch).equals(displayPatchVersion))
                .toList();
    }

    private Map<String, String> buildClientVersionToPatchMap() {
        return clientVersionPatchMappingRepository.findAll().stream()
                .collect(Collectors.toMap(ClientVersionPatchMapping::getClientVersion, ClientVersionPatchMapping::getPatchVersion));
    }

    // 매핑이 없는 클라이언트 버전은 원본 값을 그대로 노출 (데이터 누락처럼 보이지 않도록 UNKNOWN으로 치환하지 않음)
    private String resolveDisplayPatchVersion(String rawVersion, Map<String, String> clientVersionToPatch) {
        return clientVersionToPatch.getOrDefault(rawVersion, rawVersion);
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
        final LocalDate dataStartDate;
        int totalParticipants = 0;

        PatchDeckStats(LocalDate dataStartDate) {
            this.dataStartDate = dataStartDate;
        }
    }

    private boolean isShopUnit(UnitStat unitStat) {
        return TftShopUnitFilter.isShopUnit(unitStat.characterId);
    }

    // primaryCarryId: 아이템 2개 이상 장착 유닛 중 아이템 수 최다 → 아키타입 핵심 정체성
    private record DeckProfile(String traitSignature, Set<String> coreUnitIds, Set<String> boardUnitIds, String primaryCarryId) {
    }

    private static class DeckStat {
        final List<TraitDto> traits;
        final DeckProfile profile;
        final Map<String, UnitStat> unitStats = new HashMap<>();
        final Map<String, ItemStat> itemStats = new HashMap<>();
        // #137: 병합된 전체 샘플 기준 유닛 등장 빈도 집계 → frequency 기반 signature 재계산에 사용
        final Map<String, Integer> unitFrequency = new HashMap<>();
        int count = 0, wins = 0, top4 = 0;
        double totalPlace = 0;

        DeckStat(List<TraitDto> traits, DeckProfile profile) {
            this.traits = traits;
            this.profile = profile;
        }

        void record(ParticipantDto participant) {
            int placement = participant.getPlacement();
            count++;
            totalPlace += placement;
            if (placement == 1) wins++;
            if (placement <= 4) top4++;

            participant.getUnits().forEach(unit -> {
                String characterId = unit.getCharacter_id();
                if (characterId != null && !characterId.isBlank()) {
                    unitFrequency.merge(characterId, 1, Integer::sum);
                }
                UnitStat unitStat = unitStats.computeIfAbsent(unit.getCharacter_id(), UnitStat::new);
                unitStat.record(unit);

                if (unit.getItemNames() != null) {
                    unit.getItemNames().stream()
                            .filter(itemName -> itemName != null && !itemName.isBlank())
                            .forEach(itemName -> {
                                if (isRecommendedItem(itemName)) {
                                    unitStat.recordItem(itemName);
                                }
                                if (isArtifactItem(itemName)) {
                                    itemStats.computeIfAbsent(itemName, ItemStat::new).record(placement);
                                }
                            });
                }
            });

        }

        double winRate() { return count == 0 ? 0 : (double) wins / count * 100; }
        double top4Rate() { return count == 0 ? 0 : (double) top4 / count * 100; }
        double avgPlacement() { return count == 0 ? 0 : totalPlace / count; }
    }

    private static boolean isRecommendedItem(String itemId) {
        String normalized = itemId.toLowerCase();
        if (!normalized.startsWith("tft_item_")) {
            return false;
        }
        if (COMPONENT_ITEM_IDS.contains(normalized)) {
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
                && !normalized.contains("temporary");
    }

    private static boolean isArtifactItem(String itemId) {
        String normalized = itemId.toLowerCase();
        return !normalized.contains("emptybag")
                && (normalized.contains("artifact")
                || normalized.contains("ornn")
                || normalized.contains("shimmerscale"));
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

        int itemCount() {
            return itemCounts.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
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

}
