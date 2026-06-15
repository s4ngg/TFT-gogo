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
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "회원 인증 API")
public interface AuthControllerDocs {

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입하고 JWT 액세스 토큰을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공")
    })
    ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request);

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 JWT 액세스 토큰을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공")
    })
    ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request);

    @Operation(
            summary = "소셜 로그인 시작 URL 조회",
            description = """
                    provider(google, kakao, naver)에 맞는 Spring OAuth2 브라우저 리다이렉트 시작 URL을 반환합니다.
                    실제 OAuth 인증 시작 엔드포인트(/oauth2/authorization/{provider})와 콜백(/login/oauth2/code/{provider})은 브라우저 리다이렉트용이며 ApiResponse를 반환하지 않습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "소셜 로그인 시작 URL 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 provider")
    })
    ResponseEntity<ApiResponse<SocialLoginStartResponse>> getSocialLoginStartUrl(
            @Parameter(description = "소셜 로그인 provider: google, kakao, naver")
            @PathVariable("provider") String provider
    );
}
