package com.tftgogo.domain.community.chat.controller;

import com.tftgogo.domain.community.chat.controller.docs.ChatControllerDocs;
import com.tftgogo.domain.community.chat.dto.request.ChatMessageCreateRequest;
import com.tftgogo.domain.community.chat.dto.response.ChatMessageResponse;
import com.tftgogo.domain.community.chat.service.ChatService;
import com.tftgogo.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/community/chat")
@RequiredArgsConstructor
public class ChatController implements ChatControllerDocs {

    private final ChatService chatService;

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getRecentMessages(@PathVariable String roomId) {
        List<ChatMessageResponse> response = chatService.getRecentMessages(roomId);
        return ResponseEntity.ok(ApiResponse.success("채팅 메시지 조회 성공", response));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody ChatMessageCreateRequest request
    ) {
        ChatMessageResponse response = chatService.sendMessage(request);
        return ResponseEntity.ok(ApiResponse.success("채팅 메시지 전송 성공", response));
    }

    @GetMapping(path = "/rooms/{roomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String roomId) {
        return chatService.subscribe(roomId);
    }
}
