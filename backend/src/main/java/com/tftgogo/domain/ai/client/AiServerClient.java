package com.tftgogo.domain.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.ai.dto.AiChatRequest;
import com.tftgogo.domain.ai.dto.AiChatResponse;
import com.tftgogo.domain.ai.dto.AiRecommendResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * AI 서버(FastAPI) HTTP 클라이언트.
 * AI 서버가 타임아웃되거나 오류를 반환하면 null을 반환하고 호출부에서 fallback 처리.
 */
@Component
public class AiServerClient {

    private static final Logger logger = LogManager.getLogger(AiServerClient.class);
    private static final int CHAT_READ_TIMEOUT_SECONDS = 30;

    private final RestClient restClient;
    private final RestClient chatRestClient;
    private final ObjectMapper objectMapper;

    public AiServerClient(
            @Value("${ai.server.url:http://localhost:8000}") String aiServerUrl,
            @Value("${ai.server.timeout-seconds:10}") int timeoutSeconds,
            ObjectMapper objectMapper
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);

        SimpleClientHttpRequestFactory chatFactory = new SimpleClientHttpRequestFactory();
        chatFactory.setConnectTimeout(timeoutSeconds * 1000);
        chatFactory.setReadTimeout(CHAT_READ_TIMEOUT_SECONDS * 1000);

        this.restClient = RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(factory)
                .build();
        this.chatRestClient = RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(chatFactory)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * AI 서버에 전적 분석 + 메타 덱 매칭 요청.
     *
     * @param requestBody Spring이 구성한 요청 바디 (전적 + 메타 덱)
     * @return AI 분석 결과, 오류 시 null
     */
    public AiRecommendResponse analyzeWithMeta(Map<String, Object> requestBody) {
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            return restClient.post()
                    .uri("/api/analyze/with-meta")
                    .header("Content-Type", "application/json")
                    .body(json)
                    .retrieve()
                    .body(AiRecommendResponse.class);
        } catch (Exception e) {
            logger.warn("AI 서버 호출 실패, fallback 사용: {}", e.getMessage());
            return null;
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
                    .body(json)
                    .retrieve()
                    .body(AiChatResponse.class);
        } catch (Exception e) {
            logger.warn("AI 서버 채팅 호출 실패, fallback 사용: {}", e.getMessage());
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
}
