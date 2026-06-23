package com.tftgogo.domain.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.ai.dto.AiChatRequest;
import com.tftgogo.domain.ai.dto.AiChatResponse;
import com.tftgogo.domain.ai.dto.AiRecommendResponse;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class AiServerClient {

    private static final Logger logger = LogManager.getLogger(AiServerClient.class);
    private static final int CHAT_READ_TIMEOUT_SECONDS = 30;

    private final RestClient restClient;
    private final RestClient chatRestClient;
    private final ObjectMapper objectMapper;

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
