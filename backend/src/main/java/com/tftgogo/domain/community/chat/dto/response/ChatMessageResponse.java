package com.tftgogo.domain.community.chat.dto.response;

import com.tftgogo.domain.community.chat.model.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ChatMessageResponse {

    private String id;
    private String roomId;
    private Long senderId;
    private String senderName;
    private String tier;
    private String content;
    private Instant createdAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.id())
                .roomId(message.roomId())
                .senderId(message.senderId())
                .senderName(message.senderName())
                .tier(message.tier())
                .content(message.content())
                .createdAt(message.createdAt())
                .build();
    }
}
