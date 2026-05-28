package com.tftgogo.domain.deck.service.impl;

import com.tftgogo.domain.deck.dto.response.MetaDeckResponse;
import com.tftgogo.domain.deck.entity.*;
import com.tftgogo.domain.deck.repository.MetaDeckRepository;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.global.riot.RiotApiClient;
import com.tftgogo.global.riot.dto.LeagueListDto;
import com.tftgogo.global.riot.dto.MatchDto;
import com.tftgogo.global.riot.dto.MatchDto.ParticipantDto;
import com.tftgogo.global.riot.dto.MatchDto.TraitDto;
import com.tftgogo.global.riot.dto.MatchDto.UnitDto;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class MetaDeckServiceImpl implements MetaDeckService {

    private static final Logger logger = LogManager.getLogger(MetaDeckServiceImpl.class);

    private static final int MAX_SUMMONERS        = 30;
    private static final int MATCHES_PER_SUMMONER = 10;
    private static final int MIN_SAMPLE           = 10;
    private static final int SIGNATURE_TRAIT_COUNT = 3;
    private static final long RATE_LIMIT_DELAY_MS = 1200L;

    private final MetaDeckRepository metaDeckRepository;
    private final RiotApiClient riotApiClient;

    // ── 프론트 응답 ────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<MetaDeckResponse> getMetaDecks() {
        List<MetaDeck> decks = metaDeckRepository.findAllOrderByWinRateDesc();
        AtomicInteger rank = new AtomicInteger(1);
        return decks.stream()
                .map(d -> MetaDeckResponse.from(d, rank.getAndIncrement()))
                .toList();
    }

    // ── Riot API 집계 (트랜잭션 없음 - 장시간 I/O 포함) ──────
    @Override
    public void aggregateAndSave() {
        logger.info("메타 덱 집계 시작");

        List<String> puuids = collectPuuids();
        if (puuids.isEmpty()) {
            logger.warn("수집된 소환사 PUUID 없음 - 집계 중단");
            return;
        }

        Map<String, DeckStat> deckStatMap = new HashMap<>();
        int totalParticipants = 0;

        for (String puuid : puuids) {
            try {
                List<String> matchIds = riotApiClient.getMatchIds(puuid, MATCHES_PER_SUMMONER);
                for (String matchId : matchIds) {
                    MatchDto match = riotApiClient.getMatch(matchId);
                    if (match == null || match.getInfo() == null) continue;

                    int count = processMatch(match, deckStatMap);
                    totalParticipants += count;
                    Thread.sleep(RATE_LIMIT_DELAY_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("집계 중단 - 인터럽트 발생", e);
                return;
            } catch (Exception e) {
                logger.warn("소환사 {} 매치 수집 실패 - 건너뜀", puuid, e);
            }
        }

        logger.info("총 {}명 참가자 처리 완료, {}개 조합 발견", totalParticipants, deckStatMap.size());
        // DB 저장만 트랜잭션 범위로 분리
        persistDeckStats(deckStatMap, totalParticipants);
    }

    // ── DB 저장만 트랜잭션 ─────────────────────────────────
    @Transactional
    public void persistDeckStats(Map<String, DeckStat> deckStatMap, int totalParticipants) {
        saveDeckStats(deckStatMap, totalParticipants);
    }

    // ── PUUID 수집 ─────────────────────────────────────────
    private List<String> collectPuuids() {
        List<String> puuids = new ArrayList<>();
        try {
            addPuuids(puuids, riotApiClient.getChallenger(), MAX_SUMMONERS / 2);
            addPuuids(puuids, riotApiClient.getGrandmaster(), MAX_SUMMONERS / 2);
        } catch (Exception e) {
            logger.error("소환사 목록 조회 실패", e);
        }
        return puuids;
    }

    private void addPuuids(List<String> puuids, LeagueListDto league, int limit) {
        if (league == null || league.getEntries() == null) return;
        league.getEntries().stream()
                .filter(e -> e.getPuuid() != null)
                .limit(limit)
                .forEach(e -> puuids.add(e.getPuuid()));
    }

    // ── 매치 1개 처리 ─────────────────────────────────────
    private int processMatch(MatchDto match, Map<String, DeckStat> deckStatMap) {
        int count = 0;
        for (ParticipantDto p : match.getInfo().getParticipants()) {
            if (p.getTraits() == null || p.getUnits() == null) continue;

            List<TraitDto> activeTraits = p.getTraits().stream()
                    .filter(t -> t.getStyle() > 0)
                    .sorted(Comparator.comparingInt(TraitDto::getNum_units).reversed()
                            .thenComparing(TraitDto::getName))   // 동률 시 이름으로 결정적 정렬
                    .toList();

            if (activeTraits.isEmpty()) continue;

            String signature = buildSignature(activeTraits);
            deckStatMap.computeIfAbsent(signature, k -> new DeckStat(activeTraits, p.getUnits()))
                       .record(p.getPlacement());
            count++;
        }
        return count;
    }

    // ── 조합 시그니처 ──────────────────────────────────────
    private String buildSignature(List<TraitDto> activeTraits) {
        return activeTraits.stream()
                .limit(SIGNATURE_TRAIT_COUNT)
                .map(t -> t.getName() + "-" + t.getNum_units())
                .collect(Collectors.joining("_"));
    }

    // ── DB 저장 ────────────────────────────────────────────
    private void saveDeckStats(Map<String, DeckStat> deckStatMap, int totalParticipants) {
        List<Map.Entry<String, DeckStat>> ranked = deckStatMap.entrySet().stream()
                .filter(e -> e.getValue().count >= MIN_SAMPLE)
                .sorted((a, b) -> Double.compare(b.getValue().winRate(), a.getValue().winRate()))
                .toList();

        // 현재 패치 버전은 나중에 Riot API로 조회 (임시 고정)
        String patchVersion = "15.1";

        for (Map.Entry<String, DeckStat> entry : ranked) {
            DeckStat stat = entry.getValue();
            double playRate = (double) stat.count / totalParticipants * 100;
            String tier = assignTier(stat.winRate(), stat.top4Rate());
            String name = buildDeckName(stat.traits);

            MetaDeck deck = metaDeckRepository.findBySignature(entry.getKey())
                    .orElseGet(() -> MetaDeck.builder()
                            .signature(entry.getKey())
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
            }

            metaDeckRepository.save(deck);
            saveUnitsAndTraits(deck, stat);

            logger.info("덱 저장: name={} tier={} winRate={}% sample={}",
                    name, tier, String.format("%.1f", stat.winRate()), stat.count);
        }

        logger.info("메타 덱 집계 완료: {}개 덱 저장", ranked.size());
    }

    // ── DeckUnit / DeckTrait 저장 ──────────────────────────
    private void saveUnitsAndTraits(MetaDeck deck, DeckStat stat) {
        // 트레이트
        for (TraitDto t : stat.traits) {
            String tone = switch (t.getStyle()) {
                case 1 -> "bronze";
                case 2 -> "silver";
                case 3 -> "gold";
                case 4 -> "prismatic";
                default -> "bronze";
            };
            DeckTrait trait = DeckTrait.builder()
                    .metaDeck(deck)
                    .traitId(t.getName())
                    .traitName(extractName(t.getName()))
                    .numUnits(t.getNum_units())
                    .tone(tone)
                    .iconUrl(buildTraitIconUrl(t.getName()))
                    .build();
            deck.getTraits().add(trait);
        }

        // 유닛
        for (UnitDto u : stat.units) {
            DeckUnit unit = DeckUnit.builder()
                    .metaDeck(deck)
                    .characterId(u.getCharacter_id())
                    .championName(extractName(u.getCharacter_id()))
                    .cost(0)        // cost는 별도 정적 데이터 필요 (추후 추가)
                    .isCarry(u.getTier() >= 2)
                    .starLevel(u.getTier())
                    .build();
            deck.getUnits().add(unit);
        }
    }

    // ── 유틸 ───────────────────────────────────────────────
    private String extractName(String id) {
        int idx = id.lastIndexOf('_');
        return idx >= 0 ? id.substring(idx + 1) : id;
    }

    private String buildDeckName(List<TraitDto> traits) {
        return traits.stream()
                .limit(2)
                .map(t -> extractName(t.getName()))
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

    private String buildTraitIconUrl(String traitId) {
        return "https://raw.communitydragon.org/latest/game/assets/ux/traiticons/"
                + traitId.toLowerCase() + ".png";
    }

    // ── 내부 집계 구조체 ───────────────────────────────────
    private static class DeckStat {
        final List<TraitDto> traits;
        final List<UnitDto> units;
        int count = 0, wins = 0, top4 = 0;
        double totalPlace = 0;

        DeckStat(List<TraitDto> traits, List<UnitDto> units) {
            this.traits = traits;
            this.units = units;
        }

        void record(int placement) {
            count++;
            totalPlace += placement;
            if (placement == 1) wins++;
            if (placement <= 4) top4++;
        }

        double winRate()  { return count == 0 ? 0 : (double) wins / count * 100; }
        double top4Rate() { return count == 0 ? 0 : (double) top4 / count * 100; }
        double avgPlacement() { return count == 0 ? 0 : totalPlace / count; }
    }
}
