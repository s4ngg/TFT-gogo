package com.tftgogo.domain.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.ai.dto.AiRecommendResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class AiServerClient {

    private static final Logger logger = LogManager.getLogger(AiServerClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiServerClient(AiServerProperties props, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getTimeoutSeconds() * 1000);
        factory.setReadTimeout(props.getTimeoutSeconds() * 1000);

        this.restClient = RestClient.builder()
                .baseUrl(props.getUrl())
                .requestFactory(factory)
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
