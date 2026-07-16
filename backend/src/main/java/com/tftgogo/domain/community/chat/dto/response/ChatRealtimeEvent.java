package com.tftgogo.domain.community.chat.dto.response;

public record ChatRealtimeEvent(
        String roomId,
        ChatMessageResponse message
) {
}
