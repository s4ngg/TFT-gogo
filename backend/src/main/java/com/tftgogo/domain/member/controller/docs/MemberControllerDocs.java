package com.tftgogo.domain.member.controller.docs;

import com.tftgogo.domain.member.dto.response.MemberResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "Member", description = "회원 API")
public interface MemberControllerDocs {

    @Operation(summary = "내 정보 조회", description = "JWT 인증 정보를 기준으로 현재 로그인한 회원 정보를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 정보 조회 성공")
    })
    ResponseEntity<ApiResponse<MemberResponse>> getMe(Authentication authentication);
}
