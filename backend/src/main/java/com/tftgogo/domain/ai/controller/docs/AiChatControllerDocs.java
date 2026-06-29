package com.tftgogo.domain.ai.controller.docs;

import com.tftgogo.domain.ai.dto.request.AiChatRequest;
import com.tftgogo.domain.ai.dto.response.AiChatResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "AI", description = "TFT AI 어시스턴트 API")
public interface AiChatControllerDocs {

    @Operation(
            summary = "AI 채팅",
            description = "TFT AI 어시스턴트에게 질문을 전송하고 응답을 받습니다. "
                    + "인증된 사용자만 사용 가능하며, 분당 10회 요청 제한이 적용됩니다. "
                    + "Spring이 AI 서버로 요청을 프록시하며, AI 서버 오류 시 서비스 불가 메시지를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "응답 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 한도 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI 서버 연결 실패")
    })
    ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @RequestBody AiChatRequest request
    );
}
