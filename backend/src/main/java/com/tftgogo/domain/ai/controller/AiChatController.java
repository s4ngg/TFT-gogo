package com.tftgogo.domain.ai.controller;

import com.tftgogo.domain.ai.controller.docs.AiChatControllerDocs;
import com.tftgogo.domain.ai.dto.AiChatRequest;
import com.tftgogo.domain.ai.dto.AiChatResponse;
import com.tftgogo.domain.ai.service.AiChatService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController implements AiChatControllerDocs {

    private final AiChatService aiChatService;

    /**
     * TFT AI 어시스턴트 채팅 엔드포인트.
     *
     * 프론트 → Spring → AI 서버 흐름의 Spring 프록시 엔드포인트.
     * AI 서버 오류 시 서비스 불가 메시지를 응답으로 반환한다.
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @RequestBody AiChatRequest request
    ) {
        AiChatResponse response = aiChatService.chat(request);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.success("AI 서버 연결 실패", AiChatResponse.serviceUnavailable()));
        }
        return ResponseEntity.ok(ApiResponse.success("AI 채팅 응답 완료", response));
    }
}
