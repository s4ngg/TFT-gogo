package com.tftgogo.domain.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.ai.dto.AiChatRequest;
import com.tftgogo.domain.ai.dto.AiChatResponse;
import com.tftgogo.domain.ai.dto.AiRecommendResponse;
import com.tftgogo.domain.ai.dto.GameGuideAiPathfinderRequest;
import com.tftgogo.domain.ai.dto.GameGuideAiPathfinderResponse;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiServerClient {

    private static final Logger logger = LogManager.getLogger(AiServerClient.class);
    private static final int CHAT_READ_TIMEOUT_SECONDS = 30;

    private final RestClient restClient;
    private final RestClient chatRestClient;
    private final ObjectMapper objectMapper;
    private final String internalSecret;

    public AiServerClient(AiServerProperties props, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getTimeoutSeconds() * 1000);
        factory.setReadTimeout(props.getTimeoutSeconds() * 1000);

        SimpleClientHttpRequestFactory chatFactory = new SimpleClientHttpRequestFactory();
        chatFactory.setConnectTimeout(props.getTimeoutSeconds() * 1000);
        chatFactory.setReadTimeout(CHAT_READ_TIMEOUT_SECONDS * 1000);

        this.restClient = RestClient.builder()
                .baseUrl(props.getUrl())
                .requestFactory(factory)
                .build();
        this.chatRestClient = RestClient.builder()
                .baseUrl(props.getUrl())
                .requestFactory(chatFactory)
                .build();
        this.objectMapper = objectMapper;
        this.internalSecret = props.getInternalSecret();
    }

    /**
     * AI 서버에 전적 분석 + 메타 덱 매칭 요청.
     * 통신 오류 시 {@link BusinessException}(AI_SERVER_ERROR)을 던진다.
     */
    public AiRecommendResponse analyzeWithMeta(Map<String, Object> requestBody) {
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            AiRecommendResponse response = restClient.post()
                    .uri("/api/analyze/with-meta")
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Secret", internalSecret)
                    .body(json)
                    .retrieve()
                    .body(AiRecommendResponse.class);
            if (response == null) {
                logger.warn("AI 서버 빈 응답 수신");
                throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("AI 서버 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    /**
     * AI 서버 채팅 요청.
     *
     * @param request 메시지 히스토리 + 소환사 컨텍스트
     * @return AI 응답, 오류 시 null
     */
    public AiChatResponse chat(AiChatRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            return chatRestClient.post()
                    .uri("/api/chat")
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Secret", internalSecret)
                    .body(json)
                    .retrieve()
                    .body(AiChatResponse.class);
        } catch (Exception e) {
            logger.warn("AI 서버 채팅 호출 실패, fallback 사용: {}", e.getMessage());
            return null;
        }
    }

    public GameGuideAiPathfinderResponse pathfindGameGuide(GameGuideAiPathfinderRequest request) {
        return pathfindGameGuide(request, toMinimalSelectedEntries(request.getSelectedRefs()));
    }

    public GameGuideAiPathfinderResponse pathfindGameGuide(
            GameGuideAiPathfinderRequest request,
            List<GameGuideSelectedEntry> selectedEntries
    ) {
        try {
            String json = objectMapper.writeValueAsString(toGameGuidePathfinderBody(request, selectedEntries));
            byte[] responseBody = chatRestClient.post()
                    .uri("/api/gameguide/pathfinder")
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Secret", internalSecret)
                    .body(json)
                    .retrieve()
                    .body(byte[].class);
            if (responseBody == null || responseBody.length == 0) {
                return null;
            }
            return objectMapper.readValue(responseBody, GameGuideAiPathfinderResponse.class);
        } catch (Exception e) {
            logger.warn("GameGuide AI 서버 호출 실패, fallback 사용: {}", e.getMessage());
            return null;
        }
    }

    /**
     * AI 서버 헬스체크.
     */
    public boolean isHealthy() {
        try {
            restClient.get()
                    .uri("/health")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            logger.warn("AI 서버 헬스체크 실패: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> toGameGuidePathfinderBody(
            GameGuideAiPathfinderRequest request,
            List<GameGuideSelectedEntry> selectedEntries
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("patch_version", request.getPatchVersion());
        body.put("active_tab", request.getActiveTab());
        body.put("mode", request.getMode());
        body.put("selected_entries", toSelectedEntryBodies(selectedEntries));
        body.put("candidate_refs", toGuideRefBodies(request.getCandidateRefs()));
        body.put("question", request.getQuestion());
        return body;
    }

    private List<Map<String, Object>> toSelectedEntryBodies(List<GameGuideSelectedEntry> selectedEntries) {
        if (selectedEntries == null || selectedEntries.isEmpty()) {
            return List.of();
        }

        return selectedEntries.stream()
                .map(this::toSelectedEntryBody)
                .toList();
    }

    private List<GameGuideSelectedEntry> toMinimalSelectedEntries(
            List<GameGuideAiPathfinderRequest.GuideRefDto> refs
    ) {
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }

        return refs.stream()
                .map(ref -> new GameGuideSelectedEntry(
                        ref.getGuideType(),
                        ref.getTargetKey(),
                        ref.getName(),
                        null,
                        Map.of()
                ))
                .toList();
    }

    private Map<String, Object> toSelectedEntryBody(GameGuideSelectedEntry entry) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("guide_type", entry.guideType());
        body.put("target_key", entry.targetKey());
        if (entry.name() != null) {
            body.put("name", entry.name());
        }
        if (entry.summary() != null) {
            body.put("summary", entry.summary());
        }
        body.put("data", entry.data() == null ? Map.of() : entry.data());
        return body;
    }

    private List<Map<String, Object>> toGuideRefBodies(List<GameGuideAiPathfinderRequest.GuideRefDto> refs) {
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }

        return refs.stream()
                .map(this::toGuideRefBody)
                .toList();
    }

    private Map<String, Object> toGuideRefBody(GameGuideAiPathfinderRequest.GuideRefDto ref) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("guide_type", ref.getGuideType());
        body.put("target_key", ref.getTargetKey());
        if (ref.getName() != null) {
            body.put("name", ref.getName());
        }
        return body;
    }

    public record GameGuideSelectedEntry(
            String guideType,
            String targetKey,
            String name,
            String summary,
            Map<String, Object> data
    ) {
    }
}
