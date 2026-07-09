package com.tftgogo.domain.member.controller.docs;

import com.tftgogo.domain.member.dto.request.LoginRequest;
import com.tftgogo.domain.member.dto.request.SignupRequest;
import com.tftgogo.domain.member.dto.response.AuthResponse;
import com.tftgogo.domain.member.dto.response.SocialLoginStartResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "회원 인증 API")
public interface AuthControllerDocs {

    @Operation(
            summary = "회원가입",
            description = "이메일, 비밀번호, 닉네임으로 회원가입하고 JWT 액세스 토큰을 반환하며 Refresh Token은 HttpOnly 쿠키로 설정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공")
    })
    ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request);

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하고 JWT 액세스 토큰을 반환하며 Refresh Token은 HttpOnly 쿠키로 설정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공")
    })
    ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request);

    @Operation(
            summary = "토큰 재발급",
            description = "HttpOnly Refresh Token 쿠키를 검증하고 rotation한 뒤 새 Access Token을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token 없음, 만료, 재사용 탐지 또는 세션 무효")
    })
    ResponseEntity<ApiResponse<AuthResponse>> refresh(HttpServletRequest request);

    @Operation(
            summary = "로그아웃",
            description = "Refresh Token 세션을 폐기하고 유효한 Access Token jti를 blocklist에 등록한 뒤 Refresh Token 쿠키를 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            HttpServletRequest request
    );

    @Operation(
            summary = "소셜 로그인 시작 URL 조회",
            description = """
                    provider(google, naver)에 맞는 Spring OAuth2 브라우저 리다이렉트 시작 URL을 반환합니다.
                    반환되는 authorizationUrl은 http/https 절대 URL이며, 상대 경로 또는 userinfo가 포함된 URL은 허용하지 않습니다.
                    실제 provider 인증 완료에는 각 provider client-id/client-secret과 redirect-uri 설정이 필요합니다.
                    지원 provider라도 OAuth2 client-id/client-secret/redirect-uri 설정이 없으면 이 API는 503 ApiResponse 실패를 반환합니다.
                    실제 OAuth 인증 시작 엔드포인트(/oauth2/authorization/{provider})와 콜백(/login/oauth2/code/{provider})은 브라우저 리다이렉트용이며 ApiResponse를 반환하지 않습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "소셜 로그인 시작 URL 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 provider 또는 잘못된 OAuth2 base URL"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "OAuth2 provider client 설정 누락")
    })
    ResponseEntity<ApiResponse<SocialLoginStartResponse>> getSocialLoginStartUrl(
            @Parameter(description = "소셜 로그인 provider: google, naver")
            @PathVariable("provider") String provider
    );
}
