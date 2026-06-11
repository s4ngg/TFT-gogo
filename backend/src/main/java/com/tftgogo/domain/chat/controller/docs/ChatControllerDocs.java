package com.tftgogo.domain.chat.controller.docs;

import com.tftgogo.domain.chat.dto.request.ChatMessageRequest;
import com.tftgogo.domain.chat.dto.response.ChatMessageResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "Chat", description = "커뮤니티 실시간 채팅 API")
public interface ChatControllerDocs {

    @Operation(summary = "채팅 메시지 조회", description = "roomId 기준 최근 채팅 메시지를 최대 100개까지 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @Parameter(description = "채팅방 ID", example = "party-recruitment", required = true)
            @PathVariable String roomId,
            @Parameter(description = "조회 개수", example = "100")
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int limit
    );

    @Operation(summary = "채팅 메시지 전송", description = "JWT 인증 사용자가 선택한 roomId로 채팅 메시지를 전송합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전송 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Parameter(description = "채팅방 ID", example = "party-recruitment", required = true)
            @PathVariable String roomId,
            @Valid @RequestBody ChatMessageRequest request
    );

    @Operation(
            summary = "채팅 SSE 구독",
            description = "roomId 기준 실시간 메시지를 text/event-stream으로 구독합니다. 이벤트 data는 ApiResponse<ChatMessageResponse> 형태입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "스트림 연결 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    SseEmitter stream(
            @Parameter(description = "채팅방 ID", example = "party-recruitment", required = true)
            @PathVariable String roomId
    );
}
