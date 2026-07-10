package com.tftgogo.domain.ai.service;

import com.tftgogo.domain.ai.client.AiServerClient;
import com.tftgogo.domain.ai.dto.response.AiRecommendResponse;
import com.tftgogo.domain.deck.dto.response.MetaDeckListResponse;
import com.tftgogo.domain.deck.dto.response.MetaDeckResponse;
import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.domain.match.service.MatchCollectionService;
import com.tftgogo.domain.search.dto.response.SummonerProfileResponse;
import com.tftgogo.domain.search.service.SummonerService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiRecommendService {

    private static final int MATCH_COUNT = 20;

    private final AiServerClient aiServerClient;
    private final SummonerService summonerService;
    private final MatchCollectionService matchCollectionService;
    private final MetaDeckService metaDeckService;
    private final GuideTraitRepository guideTraitRepository;

    public AiRecommendService(AiServerClient aiServerClient,
                               SummonerService summonerService,
                               MatchCollectionService matchCollectionService,
                               MetaDeckService metaDeckService,
                               GuideTraitRepository guideTraitRepository) {
        this.aiServerClient = aiServerClient;
        this.summonerService = summonerService;
        this.matchCollectionService = matchCollectionService;
        this.metaDeckService = metaDeckService;
        this.guideTraitRepository = guideTraitRepository;
    }

    /**
     * 소환사 전적 분석 + AI 추천 결과 반환.
     *
     * @param gameName 소환사 게임 이름
     * @param tagLine  소환사 태그라인
     * @return AI 분석 결과, 랭크 전적이 없으면 null
     */
    public AiRecommendResponse recommend(String gameName, String tagLine) {
        // Step 1: PUUID 조회 — 실패 시 BusinessException 전파 (GlobalExceptionHandler가 처리)
        SummonerProfileResponse profile = summonerService.getProfile(gameName, tagLine);
        String puuid = profile.getPuuid();

        // Step 2: 최근 랭크 게임 전적 조회 (캐시 우선, 없으면 Riot API 수집)
        List<MatchSummaryResponse> matches = matchCollectionService.getRankedMatchSummaries(puuid, MATCH_COUNT);
        if (matches.isEmpty()) {
            return null;
        }

        // Step 3: 현재 메타 덱 목록 조회
        MetaDeckListResponse metaDeckList = metaDeckService.getMetaDecks(RankFilter.MASTER_PLUS);
        List<MetaDeckResponse> metaDecks = metaDeckList.getDecks();

        // Step 4: AI 서버 요청 바디 구성 후 호출
        Map<String, Object> requestBody = buildRequestBody(gameName, tagLine, matches, metaDecks);
        AiRecommendResponse response = aiServerClient.analyzeWithMeta(requestBody);
        applyKoreanTraitNames(response);
        return response;
    }

    /**
     * ai-server는 CDragon trait key에서 뽑은 영문 suffix(예: "psyops")를 그대로 내려주므로,
     * 현재 패치의 GuideTrait 한글 이름으로 치환한다. 매칭되는 GuideTrait가 없는 시너지
     * (숨김/내부 태그 등)는 원래 suffix를 그대로 둔다.
     */
    private void applyKoreanTraitNames(AiRecommendResponse response) {
        if (response == null) {
            return;
        }
        boolean hasGoodTraits = response.getGoodTraits() != null && !response.getGoodTraits().isEmpty();
        boolean hasBadTraits = response.getBadTraits() != null && !response.getBadTraits().isEmpty();
        if (!hasGoodTraits && !hasBadTraits) {
            return;
        }

        guideTraitRepository.findLatestPatchVersion().ifPresent(patchVersion -> {
            Map<String, String> koreanNameBySuffix = guideTraitRepository
                    .findByPatchVersionOrderByNameAscIdAsc(patchVersion)
                    .stream()
                    .collect(Collectors.toMap(
                            this::traitSuffix,
                            GuideTrait::getName,
                            (existing, duplicate) -> existing
                    ));

            renameTraits(response.getGoodTraits(), koreanNameBySuffix);
            renameTraits(response.getBadTraits(), koreanNameBySuffix);
        });
    }

    private void renameTraits(List<AiRecommendResponse.TraitStat> traits, Map<String, String> koreanNameBySuffix) {
        if (traits == null) {
            return;
        }
        for (AiRecommendResponse.TraitStat trait : traits) {
            String koreanName = koreanNameBySuffix.get(trait.getName());
            if (koreanName != null) {
                trait.setName(koreanName);
            }
        }
    }

    private String traitSuffix(GuideTrait trait) {
        String traitKey = trait.getTraitKey();
        int lastUnderscore = traitKey.lastIndexOf('_');
        String suffix = lastUnderscore >= 0 ? traitKey.substring(lastUnderscore + 1) : traitKey;
        return suffix.toLowerCase(Locale.ROOT);
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
