package com.tftgogo.domain.community.chat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.community.chat.dto.response.ChatMessageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisChatRealtimePublisherTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void Redis_발행에_성공하면_직렬화한_이벤트를_publish한다() throws Exception {
        // given
        RedisChatRealtimePublisher publisher = new RedisChatRealtimePublisher(redisTemplate, objectMapper);
        ChatMessageResponse message = message();

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"roomId\":\"general\"}");

        // when
        publisher.publish("general", message);

        // then
        verify(redisTemplate).convertAndSend("tftgogo:community-chat", "{\"roomId\":\"general\"}");
    }

    @Test
    void 이벤트_직렬화에_실패하면_예외를_전파한다() throws Exception {
        // given
        RedisChatRealtimePublisher publisher = new RedisChatRealtimePublisher(redisTemplate, objectMapper);
        ChatMessageResponse message = message();

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("serialize failed") {
                });

        // when, then
        assertThatThrownBy(() -> publisher.publish("general", message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to serialize chat realtime event.");
    }

    @Test
    void Redis_발행에_실패하면_예외를_전파한다() throws Exception {
        // given
        RedisChatRealtimePublisher publisher = new RedisChatRealtimePublisher(redisTemplate, objectMapper);
        ChatMessageResponse message = message();
        RuntimeException failure = new RuntimeException("redis failed");

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"roomId\":\"general\"}");
        doThrow(failure).when(redisTemplate).convertAndSend(eq("tftgogo:community-chat"), any(String.class));

        // when, then
        assertThatThrownBy(() -> publisher.publish("general", message))
                .isSameAs(failure);
    }

    private ChatMessageResponse message() {
        return ChatMessageResponse.builder()
                .id("message-1")
                .roomId("general")
                .senderId(1L)
                .senderName("소정")
                .tier("Unranked")
                .content("안녕하세요")
                .createdAt(Instant.parse("2026-06-25T00:00:00Z"))
                .build();
    }
}
