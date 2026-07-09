package com.tftgogo.domain.community.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.community.chat.dto.response.ChatRealtimeEvent;
import com.tftgogo.domain.community.chat.service.ChatSseHub;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Profile("!local & !dev")
@RequiredArgsConstructor
public class RedisChatRealtimeSubscriber implements MessageListener {

    private static final Logger logger = LogManager.getLogger(RedisChatRealtimeSubscriber.class);

    private final ChatSseHub chatSseHub;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatRealtimeEvent event = objectMapper.readValue(
                    new String(message.getBody(), StandardCharsets.UTF_8),
                    ChatRealtimeEvent.class
            );

            chatSseHub.broadcast(event.roomId(), event.message());
        } catch (IOException e) {
            logger.warn("Failed to read chat realtime event.");
        }
    }
}
