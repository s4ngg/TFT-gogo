package com.tftgogo.domain.ai.controller.docs;

import com.tftgogo.domain.ai.dto.GameGuideAiPathfinderRequest;
import com.tftgogo.domain.ai.dto.GameGuideAiPathfinderResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "AI", description = "TFT AI 어시스턴트 API")
public interface GameGuideAiPathfinderControllerDocs {

    @Operation(
            summary = "GameGuide AI 패스파인더",
            description = "Guide 페이지의 현재 탭과 사용자 질문을 기반으로 게임가이드 안내 응답을 반환합니다. "
                    + "현재 MVP 단계에서는 AI 서버 연결 전 fallback 응답을 제공합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "응답 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    ResponseEntity<ApiResponse<GameGuideAiPathfinderResponse>> pathfind(
            @RequestBody GameGuideAiPathfinderRequest request
    );
}
