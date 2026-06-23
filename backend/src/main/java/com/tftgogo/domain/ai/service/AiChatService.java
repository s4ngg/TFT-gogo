package com.tftgogo.domain.ai.service;

import com.tftgogo.domain.ai.client.AiServerClient;
import com.tftgogo.domain.ai.dto.AiChatRequest;
import com.tftgogo.domain.ai.dto.AiChatResponse;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private static final Logger logger = LogManager.getLogger(AiChatService.class);

    private final AiServerClient aiServerClient;
    private final AiChatRateLimiter rateLimiter;

    public AiChatService(AiServerClient aiServerClient, AiChatRateLimiter rateLimiter) {
        this.aiServerClient = aiServerClient;
        this.rateLimiter = rateLimiter;
    }

    public AiChatResponse chat(Long userId, AiChatRequest request) {
        if (!rateLimiter.tryAcquire(userId)) {
            logger.warn("AI 채팅 rate limit 초과: userId={}", userId);
            throw new BusinessException(ErrorCode.AI_CHAT_RATE_LIMIT);
        }
        try {
            AiChatResponse response = aiServerClient.chat(request);
            if (response == null) {
                logger.warn("AI 서버 채팅 응답 없음");
                throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("AI 서버 호출 실패", e);
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        }
    }
}
