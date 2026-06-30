package com.tftgogo.domain.ai.controller.docs;

import com.tftgogo.domain.ai.dto.GameGuideAiPathfinderRequest;
import com.tftgogo.domain.ai.dto.GameGuideAiPathfinderResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "AI", description = "TFT AI 어시스턴트 API")
public interface GameGuideAiPathfinderControllerDocs {

    @Operation(
            summary = "GameGuide AI 패스파인더",
            description = "Guide 페이지의 현재 탭과 사용자 질문을 기반으로 게임가이드 안내 응답을 반환합니다. "
                    + "인증된 사용자만 사용할 수 있으며 사용자별 AI 요청 제한을 적용합니다. "
                    + "AI 서버 오류 시 fallback 응답을 제공합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "응답 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 한도 초과")
    })
    ResponseEntity<ApiResponse<GameGuideAiPathfinderResponse>> pathfind(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @RequestBody GameGuideAiPathfinderRequest request
    );
}
