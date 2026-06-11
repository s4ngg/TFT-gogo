package com.tftgogo.domain.chat.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageResponse {

    private String id;
    private String roomId;
    private String senderName;
    private String senderTier;
    private String message;
    private LocalDateTime createdAt;
    private long sequence;

    public static ChatMessageResponse of(
            String roomId,
            String senderName,
            String senderTier,
            String message,
            LocalDateTime createdAt,
            long sequence
    ) {
        return ChatMessageResponse.builder()
                .id(roomId + "-" + sequence)
                .roomId(roomId)
                .senderName(senderName)
                .senderTier(senderTier)
                .message(message)
                .createdAt(createdAt)
                .sequence(sequence)
                .build();
    }
}
