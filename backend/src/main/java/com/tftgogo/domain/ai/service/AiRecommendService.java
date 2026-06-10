package com.tftgogo.domain.ai.service;

import com.tftgogo.domain.ai.client.AiServerClient;
import com.tftgogo.domain.ai.dto.AiRecommendResponse;
import com.tftgogo.domain.deck.dto.response.MetaDeckListResponse;
import com.tftgogo.domain.deck.dto.response.MetaDeckResponse;
import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.domain.match.dto.response.SummonerProfileResponse;
import com.tftgogo.domain.match.service.MatchCollectionService;
import com.tftgogo.domain.summoner.service.SummonerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiRecommendService {

    private static final Logger logger = LogManager.getLogger(AiRecommendService.class);
    private static final int MATCH_COUNT = 20;

    private final AiServerClient aiServerClient;
    private final SummonerService summonerService;
    private final MatchCollectionService matchCollectionService;
    private final MetaDeckService metaDeckService;

    public AiRecommendService(AiServerClient aiServerClient,
                               SummonerService summonerService,
                               MatchCollectionService matchCollectionService,
                               MetaDeckService metaDeckService) {
        this.aiServerClient = aiServerClient;
        this.summonerService = summonerService;
        this.matchCollectionService = matchCollectionService;
        this.metaDeckService = metaDeckService;
    }

    /**
     * 소환사 전적 분석 + AI 추천 결과 반환.
     *
     * @param gameName 소환사 게임 이름
     * @param tagLine  소환사 태그라인
     * @return AI 분석 결과 (오류 시 null → 컨트롤러에서 fallback 처리)
     */
    public AiRecommendResponse recommend(String gameName, String tagLine) {
        // Step 1: PUUID 조회
        SummonerProfileResponse profile;
        try {
            profile = summonerService.getProfile(gameName, tagLine);
        } catch (Exception e) {
            logger.warn("AI 추천 - 소환사 조회 실패: gameName={}, tagLine={}, error={}", gameName, tagLine, e.getMessage());
            return null;
        }
        String puuid = profile.getPuuid();

        // Step 2: 최근 랭크 게임 전적 조회 (캐시 우선, 없으면 Riot API 수집)
        List<MatchSummaryResponse> matches = matchCollectionService.getRankedMatchSummaries(puuid, MATCH_COUNT);
        if (matches.isEmpty()) {
            logger.info("AI 추천 - 랭크 전적 없음: puuid={}", puuid);
            return null;
        }

        // Step 3: 현재 메타 덱 목록 조회
        MetaDeckListResponse metaDeckList = metaDeckService.getMetaDecks(RankFilter.MASTER_PLUS);
        List<MetaDeckResponse> metaDecks = metaDeckList.getDecks();

        // Step 4: AI 서버 요청 바디 구성 후 호출
        Map<String, Object> requestBody = buildRequestBody(gameName, tagLine, matches, metaDecks);
        return aiServerClient.analyzeWithMeta(requestBody);
    }

    private Map<String, Object> buildRequestBody(String gameName, String tagLine,
                                                  List<MatchSummaryResponse> matches,
                                                  List<MetaDeckResponse> metaDecks) {
        Map<String, Object> body = new HashMap<>();
        body.put("summoner_name", gameName);
        body.put("tag_line", tagLine);
        body.put("matches", matches.stream().map(this::toMatchMap).collect(Collectors.toList()));
        body.put("meta_decks", metaDecks.stream().map(this::toMetaDeckMap).collect(Collectors.toList()));
        return body;
    }

    private Map<String, Object> toMatchMap(MatchSummaryResponse m) {
        Map<String, Object> map = new HashMap<>();
        map.put("match_id", m.getMatchId());
        map.put("game_datetime", m.getGameDatetime());
        map.put("game_length", m.getGameLength());
        map.put("game_version", m.getGameVersion());
        map.put("queue_type", m.getQueueType());
        map.put("placement", m.getPlacement());
        map.put("level", m.getLevel());
        map.put("last_round", m.getLastRound());
        map.put("gold_left", m.getGoldLeft());
        map.put("players_eliminated", m.getPlayersEliminated());
        map.put("total_damage_to_players", m.getTotalDamageToPlayers());
        map.put("traits", m.getTraits().stream().map(t -> {
            Map<String, Object> tm = new HashMap<>();
            tm.put("name", t.getName());
            tm.put("num_units", t.getNumUnits());
            tm.put("style", t.getStyle());
            tm.put("tier_current", t.getTierCurrent());
            tm.put("tier_total", t.getTierTotal());
            return tm;
        }).collect(Collectors.toList()));
        map.put("units", m.getUnits().stream().map(u -> {
            Map<String, Object> um = new HashMap<>();
            um.put("character_id", u.getCharacterId());
            um.put("name", u.getName());
            um.put("tier", u.getTier());
            um.put("rarity", u.getRarity());
            um.put("item_names", u.getItemNames());
            return um;
        }).collect(Collectors.toList()));
        return map;
    }

    private Map<String, Object> toMetaDeckMap(MetaDeckResponse d) {
        Map<String, Object> map = new HashMap<>();
        map.put("rank", d.getRank());
        map.put("grade", d.getGrade());
        map.put("trait_suffixes", d.getTraitSuffixes());
        map.put("top4_rate", d.getTop4());
        map.put("avg_place", d.getAvgPlace());
        map.put("pick_rate", d.getPickRate());
        return map;
    }
}
