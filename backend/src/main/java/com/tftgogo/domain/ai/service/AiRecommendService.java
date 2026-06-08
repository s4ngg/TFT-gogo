package com.tftgogo.domain.ai.service;

import com.tftgogo.domain.ai.client.AiServerClient;
import com.tftgogo.domain.ai.dto.AiRecommendResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 추천 서비스.
 *
 * 현재는 뼈대만 작성되어 있으며, 팀원의 전적 API 완성 후 아래 TODO를 채운다.
 *
 * TODO (팀원 전적 API 완성 후):
 *   1. SummonerService / MatchService 주입
 *   2. gameName + tagLine으로 PUUID 조회
 *   3. 최근 20 랭크 게임 전적 조회
 *   4. MetaDeckService에서 현재 메타 덱 목록 조회
 *   5. AI 서버 요청 바디 구성 후 analyzeWithMeta() 호출
 */
@Service
public class AiRecommendService {

    private static final Logger logger = LogManager.getLogger(AiRecommendService.class);

    private final AiServerClient aiServerClient;

    public AiRecommendService(AiServerClient aiServerClient) {
        this.aiServerClient = aiServerClient;
    }

    /**
     * 소환사 전적 분석 + AI 추천 결과 반환.
     *
     * @param gameName 소환사 게임 이름
     * @param tagLine  소환사 태그라인
     * @return AI 분석 결과 (오류 시 null → 컨트롤러에서 fallback 처리)
     */
    public AiRecommendResponse recommend(String gameName, String tagLine) {
        // TODO Step 1: PUUID 조회
        // String puuid = summonerService.getPuuid(gameName, tagLine);

        // TODO Step 2: 최근 20 랭크 게임 전적 조회
        // List<MatchRecord> matches = matchService.getRecentRankedMatches(puuid, 20);
        // if (matches.isEmpty()) return null;

        // TODO Step 3: 현재 메타 덱 목록 조회
        // List<MetaDeck> metaDecks = metaDeckService.getCurrentMetaDecks();

        // TODO Step 4: AI 서버 요청 바디 구성
        // Map<String, Object> requestBody = buildRequestBody(gameName, tagLine, matches, metaDecks);
        // return aiServerClient.analyzeWithMeta(requestBody);

        // 팀원 코드 완성 전 임시: AI 서버 연동 테스트용 빈 요청
        logger.info("AI 추천 요청 - gameName: {}, tagLine: {}", gameName, tagLine);
        Map<String, Object> dummyRequest = new HashMap<>();
        dummyRequest.put("summoner_name", gameName);
        dummyRequest.put("tag_line", tagLine);
        dummyRequest.put("matches", List.of());
        dummyRequest.put("meta_decks", List.of());
        return aiServerClient.analyzeWithMeta(dummyRequest);
    }

    /**
     * AI 서버 요청 바디 구성 헬퍼.
     * 팀원 전적 API 완성 후 실제 데이터로 채운다.
     */
    private Map<String, Object> buildRequestBody(
            String gameName,
            String tagLine,
            List<Map<String, Object>> matches,
            List<Map<String, Object>> metaDecks
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("summoner_name", gameName);
        body.put("tag_line", tagLine);
        body.put("matches", matches);
        body.put("meta_decks", metaDecks);
        return body;
    }
}
