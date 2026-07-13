package com.tftgogo.domain.community.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.community.chat.dto.response.ChatMessageResponse;
import com.tftgogo.domain.community.chat.dto.response.ChatRealtimeEvent;
import com.tftgogo.domain.community.chat.service.ChatSseHub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisChatRealtimeSubscriberTest {

    @Mock
    private ChatSseHub chatSseHub;

    @Mock
    private Message redisMessage;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void Redis_메시지를_수신하면_SSE_hub으로_broadcast한다() throws Exception {
        // given
        RedisChatRealtimeSubscriber subscriber = new RedisChatRealtimeSubscriber(chatSseHub, objectMapper);
        ChatMessageResponse message = message();
        ChatRealtimeEvent event = new ChatRealtimeEvent("party-recruitment", message);

        when(redisMessage.getBody()).thenReturn(objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8));

        // when
        subscriber.onMessage(redisMessage, null);

        // then
        ArgumentCaptor<ChatMessageResponse> messageCaptor = ArgumentCaptor.forClass(ChatMessageResponse.class);
        verify(chatSseHub).broadcast(eq("party-recruitment"), messageCaptor.capture());
        assertThat(messageCaptor.getValue())
                .extracting(
                        ChatMessageResponse::getId,
                        ChatMessageResponse::getRoomId,
                        ChatMessageResponse::getSenderId,
                        ChatMessageResponse::getSenderName,
                        ChatMessageResponse::getTier,
                        ChatMessageResponse::getContent,
                        ChatMessageResponse::getCreatedAt
                )
                .containsExactly(
                        message.getId(),
                        message.getRoomId(),
                        message.getSenderId(),
                        message.getSenderName(),
                        message.getTier(),
                        message.getContent(),
                        message.getCreatedAt()
                );
    }

    @Test
    void Redis_메시지_역직렬화에_실패하면_broadcast하지_않는다() {
        // given
        RedisChatRealtimeSubscriber subscriber = new RedisChatRealtimeSubscriber(chatSseHub, objectMapper);

        when(redisMessage.getBody()).thenReturn("{bad-json".getBytes(StandardCharsets.UTF_8));

        // when
        subscriber.onMessage(redisMessage, null);

        // then
        verify(chatSseHub, never()).broadcast(any(), any());
    }

    private ChatMessageResponse message() {
        return ChatMessageResponse.builder()
                .id("message-1")
                .roomId("party-recruitment")
                .senderId(1L)
                .senderName("소정")
                .tier("Unranked")
                .content("같이 게임해요")
                .createdAt(Instant.parse("2026-06-25T00:00:00Z"))
                .build();
    }
}
