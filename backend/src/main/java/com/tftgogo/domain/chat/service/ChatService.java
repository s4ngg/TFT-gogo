package com.tftgogo.domain.chat.service;

import com.tftgogo.domain.chat.dto.request.ChatMessageRequest;
import com.tftgogo.domain.chat.dto.response.ChatMessageResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {

    List<ChatMessageResponse> getMessages(String roomId, int limit);

    ChatMessageResponse sendMessage(String roomId, ChatMessageRequest request);

    SseEmitter subscribe(String roomId);
}
