package com.tftgogo.domain.community.chat.controller.docs;

import com.tftgogo.domain.community.chat.dto.request.ChatMessageCreateRequest;
import com.tftgogo.domain.community.chat.dto.response.ChatMessageResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "Chat", description = "커뮤니티 실시간 채팅 API")
public interface ChatControllerDocs {

    @Operation(summary = "채팅방 최근 메시지 조회", description = "메모리에 보관된 채팅방 최근 메시지를 최대 100개까지 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅 메시지 조회 성공")
    })
    ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getRecentMessages(@PathVariable String roomId);

    @Operation(summary = "채팅 메시지 전송", description = "채팅방에 메시지를 저장하고 SSE 구독자에게 전파합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅 메시지 전송 성공")
    })
    ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(@Valid @RequestBody ChatMessageCreateRequest request);

    @Operation(summary = "채팅방 SSE 구독", description = "채팅방의 snapshot/message SSE 이벤트를 구독합니다. 응답은 text/event-stream 형식이라 ApiResponse로 감싸지 않습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE 구독 연결 성공")
    })
    SseEmitter subscribe(@PathVariable String roomId);
}
