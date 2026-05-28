package com.tftgogo.domain.deck.service.impl;

import com.tftgogo.domain.deck.dto.response.MetaDeckResponse;
import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.domain.deck.entity.MetaDeckChampion;
import com.tftgogo.domain.deck.entity.MetaDeckTrait;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MetaDeckServiceImpl implements MetaDeckService {

    private static final Logger logger = LogManager.getLogger(MetaDeckServiceImpl.class);

    // 스케줄러당 수집할 소환사 수 (레이트 리밋 고려)
    private static final int MAX_SUMMONERS = 30;
    // 소환사당 가져올 최근 매치 수
    private static final int MATCHES_PER_SUMMONER = 10;
    // 덱으로 인정할 최소 샘플 수
    private static final int MIN_SAMPLE = 10;
    // 덱 시그니처에 사용할 상위 트레이트 수
    private static final int SIGNATURE_TRAIT_COUNT = 3;

    private final MetaDeckRepository metaDeckRepository;
    private final RiotApiClient riotApiClient;

    // ── 프론트 응답 ────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<MetaDeckResponse> getMetaDecks() {
        return metaDeckRepository.findAllOrderByRank().stream()
                .map(MetaDeckResponse::from)
                .toList();
    }

    // ── Riot API 집계 ──────────────────────────────────────
    @Override
    @Transactional
    public void aggregateAndSave() {
        logger.info("메타 덱 집계 시작");

        // 1. Challenger + GM 소환사 PUUID 수집
        List<String> puuids = collectPuuids();
        if (puuids.isEmpty()) {
            logger.warn("수집된 소환사 PUUID 없음 - 집계 중단");
            return;
        }

        // 2. 매치 데이터 수집 및 파싱
        // signature → 집계 데이터 맵
        Map<String, DeckStat> deckStatMap = new HashMap<>();
        int totalMatches = 0;

        for (String puuid : puuids) {
            try {
                List<String> matchIds = riotApiClient.getMatchIds(puuid, MATCHES_PER_SUMMONER);
                for (String matchId : matchIds) {
                    MatchDto match = riotApiClient.getMatch(matchId);
                    if (match == null || match.getInfo() == null) continue;

                    processMatch(match, deckStatMap);
                    totalMatches++;
                    // 레이트 리밋 방지 (100req/2min → 약 1.2초 간격)
                    Thread.sleep(1200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("집계 중단 - 인터럽트 발생", e);
                return;
            } catch (Exception e) {
                logger.warn("소환사 {} 매치 수집 실패 - 건너뜀", puuid, e);
            }
        }

        logger.info("총 {}개 매치 처리 완료, {}개 조합 발견", totalMatches, deckStatMap.size());

        // 3. 덱 통계 계산 및 DB 저장
        saveDeckStats(deckStatMap, totalMatches * 8); // 매치당 8명
    }

    // ── PUUID 수집 ─────────────────────────────────────────
    private List<String> collectPuuids() {
        List<String> puuids = new ArrayList<>();
        try {
            LeagueListDto challenger = riotApiClient.getChallenger();
            if (challenger != null && challenger.getEntries() != null) {
                challenger.getEntries().stream()
                        .filter(e -> e.getPuuid() != null)
                        .limit(MAX_SUMMONERS / 2)
                        .forEach(e -> puuids.add(e.getPuuid()));
            }

            LeagueListDto grandmaster = riotApiClient.getGrandmaster();
            if (grandmaster != null && grandmaster.getEntries() != null) {
                grandmaster.getEntries().stream()
                        .filter(e -> e.getPuuid() != null)
                        .limit(MAX_SUMMONERS / 2)
                        .forEach(e -> puuids.add(e.getPuuid()));
            }
        } catch (Exception e) {
            logger.error("소환사 목록 조회 실패", e);
        }
        return puuids;
    }

    // ── 매치 1개 처리 ─────────────────────────────────────
    private void processMatch(MatchDto match, Map<String, DeckStat> deckStatMap) {
        for (ParticipantDto participant : match.getInfo().getParticipants()) {
            if (participant.getTraits() == null || participant.getUnits() == null) continue;

            // 활성화된 트레이트만 필터 (style > 0)
            List<TraitDto> activeTraits = participant.getTraits().stream()
                    .filter(t -> t.getStyle() > 0)
                    .sorted((a, b) -> Integer.compare(b.getNum_units(), a.getNum_units()))
                    .toList();

            if (activeTraits.isEmpty()) continue;

            String signature = buildSignature(activeTraits);
            DeckStat stat = deckStatMap.computeIfAbsent(signature, k -> new DeckStat(activeTraits, participant.getUnits()));
            stat.record(participant.getPlacement());
        }
    }

    // ── 조합 시그니처 생성 ─────────────────────────────────
    // 예: "Set13_Challenger-6_Set13_Blaster-4_Set13_Mage-3"
    private String buildSignature(List<TraitDto> activeTraits) {
        return activeTraits.stream()
                .limit(SIGNATURE_TRAIT_COUNT)
                .map(t -> t.getName() + "-" + t.getNum_units())
                .collect(Collectors.joining("_"));
    }

    // ── DB 저장 ────────────────────────────────────────────
    private void saveDeckStats(Map<String, DeckStat> deckStatMap, int totalParticipants) {
        // 샘플 충분한 덱만 필터 후 winRate 기준 정렬
        List<Map.Entry<String, DeckStat>> ranked = deckStatMap.entrySet().stream()
                .filter(e -> e.getValue().count >= MIN_SAMPLE)
                .sorted((a, b) -> Double.compare(
                        b.getValue().winRate(), a.getValue().winRate()))
                .toList();

        for (int i = 0; i < ranked.size(); i++) {
            Map.Entry<String, DeckStat> entry = ranked.get(i);
            String signature = entry.getKey();
            DeckStat stat = entry.getValue();

            int rank = i + 1;
            double pickRate = (double) stat.count / totalParticipants * 100;
            String grade = assignGrade(stat.winRate(), stat.top4Rate());

            // 트레이트 이름에서 덱 이름 생성
            String name = buildDeckName(stat.traits);

            MetaDeck deck = metaDeckRepository.findBySignature(signature)
                    .orElseGet(() -> MetaDeck.builder()
                            .signature(signature)
                            .name(name)
                            .grade(grade)
                            .rank(rank)
                            .winRate(stat.winRate())
                            .top4Rate(stat.top4Rate())
                            .avgPlace(stat.avgPlace())
                            .pickRate(pickRate)
                            .sampleCount(stat.count)
                            .build());

            if (deck.getId() != null) {
                deck.update(grade, rank, stat.winRate(), stat.top4Rate(),
                        stat.avgPlace(), pickRate, stat.count);
                // 기존 챔피언/트레이트 제거 후 재삽입
                deck.getChampions().clear();
                deck.getTraits().clear();
            }

            metaDeckRepository.save(deck);
            saveChampionsAndTraits(deck, stat);

            logger.info("덱 저장: rank={} name={} winRate={:.1f}% sample={}",
                    rank, name, stat.winRate(), stat.count);
        }

        logger.info("메타 덱 집계 완료: {}개 덱 저장", ranked.size());
    }

    // ── 챔피언/트레이트 저장 ───────────────────────────────
    private void saveChampionsAndTraits(MetaDeck deck, DeckStat stat) {
        // 트레이트
        for (TraitDto t : stat.traits) {
            String tone = switch (t.getStyle()) {
                case 1 -> "bronze";
                case 2 -> "silver";
                case 3 -> "gold";
                case 4 -> "prismatic";
                default -> "bronze";
            };
            String traitName = extractName(t.getName()); // Set13_Challenger → Challenger
            MetaDeckTrait trait = MetaDeckTrait.builder()
                    .metaDeck(deck)
                    .traitId(t.getName())
                    .traitName(traitName)
                    .unitCount(t.getNum_units())
                    .tone(tone)
                    .iconUrl(buildTraitIconUrl(t.getName()))
                    .build();
            deck.getTraits().add(trait);
        }

        // 챔피언
        for (UnitDto u : stat.units) {
            String champName = extractName(u.getCharacter_id()); // TFT13_Jinx → Jinx
            MetaDeckChampion champ = MetaDeckChampion.builder()
                    .metaDeck(deck)
                    .championId(u.getCharacter_id())
                    .championName(champName)
                    .imageUrl(buildChampionImageUrl(u.getCharacter_id()))
                    .stars(u.getTier())
                    .frequency(1.0)
                    .build();
            deck.getChampions().add(champ);
        }
    }

    // ── 유틸 ───────────────────────────────────────────────
    private String extractName(String id) {
        // TFT13_Jinx → Jinx / Set13_Challenger → Challenger
        int idx = id.lastIndexOf('_');
        return idx >= 0 ? id.substring(idx + 1) : id;
    }

    private String buildDeckName(List<TraitDto> traits) {
        return traits.stream()
                .limit(2)
                .map(t -> extractName(t.getName()))
                .collect(Collectors.joining(" "));
    }

    private String assignGrade(double winRate, double top4Rate) {
        if (winRate >= 20) return "S";
        if (winRate >= 16) return "A+";
        if (winRate >= 13) return "A";
        if (top4Rate >= 55) return "B";
        if (top4Rate >= 45) return "C";
        return "D";
    }

    private String buildTraitIconUrl(String traitId) {
        // Community Dragon CDN 패턴
        return "https://raw.communitydragon.org/latest/game/assets/ux/traiticons/"
                + traitId.toLowerCase() + ".png";
    }

    private String buildChampionImageUrl(String characterId) {
        return "https://raw.communitydragon.org/latest/game/assets/characters/"
                + characterId.toLowerCase() + "/hud/" + characterId.toLowerCase() + "_square.tft.png";
    }

    // ── 내부 집계 구조체 ───────────────────────────────────
    private static class DeckStat {
        final List<TraitDto> traits;
        final List<UnitDto> units;
        int count = 0;
        int wins = 0;
        int top4 = 0;
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
        double avgPlace() { return count == 0 ? 0 : totalPlace / count; }
    }
}
