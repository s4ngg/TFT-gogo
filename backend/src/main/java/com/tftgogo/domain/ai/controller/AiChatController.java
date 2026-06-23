package com.tftgogo.domain.ai.controller;

import com.tftgogo.domain.ai.controller.docs.AiChatControllerDocs;
import com.tftgogo.domain.ai.dto.AiChatRequest;
import com.tftgogo.domain.ai.dto.AiChatResponse;
import com.tftgogo.domain.ai.service.AiChatService;
import com.tftgogo.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController implements AiChatControllerDocs {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AiChatRequest request
    ) {
        AiChatResponse response = aiChatService.chat(userId, request);
        return ResponseEntity.ok(ApiResponse.success("AI 채팅 응답 완료", response));
    }
}
