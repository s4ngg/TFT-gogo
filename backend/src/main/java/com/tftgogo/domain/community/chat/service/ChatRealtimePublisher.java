package com.tftgogo.domain.community.chat.service;

import com.tftgogo.domain.community.chat.dto.response.ChatMessageResponse;

public interface ChatRealtimePublisher {

    String CHANNEL = "tftgogo:community-chat";

    void publish(String roomId, ChatMessageResponse message);
}
