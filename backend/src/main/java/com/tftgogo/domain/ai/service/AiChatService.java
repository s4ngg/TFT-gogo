package com.tftgogo.domain.ai.service;

import com.tftgogo.domain.ai.client.AiServerClient;
import com.tftgogo.domain.ai.dto.AiChatRequest;
import com.tftgogo.domain.ai.dto.AiChatResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private static final Logger logger = LogManager.getLogger(AiChatService.class);

    private final AiServerClient aiServerClient;

    public AiChatService(AiServerClient aiServerClient) {
        this.aiServerClient = aiServerClient;
    }

    /**
     * AI 서버에 채팅 요청을 전달하고 응답을 반환한다.
     * AI 서버 오류 시 서비스 불가 fallback 응답을 반환한다.
     *
     * @param request 메시지 히스토리 + 소환사 컨텍스트
     * @return AI 채팅 응답
     */
    public AiChatResponse chat(AiChatRequest request) {
        AiChatResponse response = aiServerClient.chat(request);

        if (response == null) {
            logger.warn("AI 서버 채팅 응답 없음, fallback 반환");
            return AiChatResponse.serviceUnavailable();
        }

        return response;
    }
}
