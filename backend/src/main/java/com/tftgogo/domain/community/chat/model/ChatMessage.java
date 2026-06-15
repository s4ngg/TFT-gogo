package com.tftgogo.domain.community.chat.model;

import java.time.Instant;

public record ChatMessage(
        String id,
        String roomId,
        String senderName,
        String tier,
        String content,
        Instant createdAt
) {
}
