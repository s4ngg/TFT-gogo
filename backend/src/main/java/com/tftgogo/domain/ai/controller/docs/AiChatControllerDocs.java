package com.tftgogo.domain.ai.controller.docs;

import com.tftgogo.domain.ai.dto.AiChatRequest;
import com.tftgogo.domain.ai.dto.AiChatResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "AI", description = "TFT AI 어시스턴트 API")
public interface AiChatControllerDocs {

    @Operation(
            summary = "AI 채팅",
            description = "TFT AI 어시스턴트에게 질문을 전송하고 응답을 받습니다. "
                    + "Spring이 AI 서버로 요청을 프록시하며, AI 서버 오류 시 서비스 불가 메시지를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "응답 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI 서버 연결 실패")
    })
    ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @RequestBody AiChatRequest request
    );
}
