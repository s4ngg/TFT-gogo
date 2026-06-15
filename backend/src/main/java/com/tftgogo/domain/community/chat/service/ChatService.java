package com.tftgogo.domain.community.chat.service;

import com.tftgogo.domain.community.chat.dto.request.ChatMessageCreateRequest;
import com.tftgogo.domain.community.chat.dto.response.ChatMessageResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {

    void ensureRoom(String roomId);

    List<ChatMessageResponse> getRecentMessages(String roomId);

    ChatMessageResponse sendMessage(Long userId, ChatMessageCreateRequest request);

    SseEmitter subscribe(String roomId);
}
