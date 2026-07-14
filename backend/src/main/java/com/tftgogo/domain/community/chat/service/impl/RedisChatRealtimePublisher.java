package com.tftgogo.domain.community.chat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.community.chat.dto.response.ChatMessageResponse;
import com.tftgogo.domain.community.chat.dto.response.ChatRealtimeEvent;
import com.tftgogo.domain.community.chat.service.ChatRealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!local")
@RequiredArgsConstructor
public class RedisChatRealtimePublisher implements ChatRealtimePublisher {

    private static final Logger logger = LogManager.getLogger(RedisChatRealtimePublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String roomId, ChatMessageResponse message) {
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(new ChatRealtimeEvent(roomId, message)));
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize chat realtime event. roomId={}", roomId, e);
        } catch (RuntimeException e) {
            logger.warn("Failed to publish chat realtime event. roomId={}", roomId, e);
        }
    }
}
