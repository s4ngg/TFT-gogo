package com.tftgogo.domain.chat.controller;

import com.tftgogo.domain.chat.controller.docs.ChatControllerDocs;
import com.tftgogo.domain.chat.dto.request.ChatMessageRequest;
import com.tftgogo.domain.chat.dto.response.ChatMessageResponse;
import com.tftgogo.domain.chat.service.ChatService;
import com.tftgogo.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class ChatController implements ChatControllerDocs {

    private final ChatService chatService;

    @Override
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int limit
    ) {
        List<ChatMessageResponse> response = chatService.getMessages(roomId, limit);
        return ResponseEntity.ok(ApiResponse.success("채팅 메시지 조회 성공", response));
    }

    @Override
    @PostMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @PathVariable String roomId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        ChatMessageResponse response = chatService.sendMessage(roomId, request);
        return ResponseEntity.ok(ApiResponse.success("채팅 메시지 전송 성공", response));
    }

    @Override
    @GetMapping(value = "/{roomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@PathVariable String roomId) {
        return ResponseEntity.ok(chatService.subscribe(roomId));
    }
}
