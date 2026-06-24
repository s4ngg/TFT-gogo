package com.tftgogo.domain.member.controller.docs;

import com.tftgogo.domain.member.dto.response.MemberResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Member", description = "회원 API")
public interface MemberControllerDocs {

    @Operation(
            summary = "내 정보 조회",
            description = "JWT 액세스 토큰으로 인증된 현재 회원 정보를 반환합니다."
    )
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    ResponseEntity<ApiResponse<MemberResponse>> getMe(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    );
}
