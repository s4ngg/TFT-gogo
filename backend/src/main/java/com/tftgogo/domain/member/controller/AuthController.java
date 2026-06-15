package com.tftgogo.domain.member.controller;

import com.tftgogo.domain.member.controller.docs.AuthControllerDocs;
import com.tftgogo.domain.member.dto.request.LoginRequest;
import com.tftgogo.domain.member.dto.request.SignupRequest;
import com.tftgogo.domain.member.dto.response.AuthResponse;
import com.tftgogo.domain.member.dto.response.SocialLoginStartResponse;
import com.tftgogo.domain.member.entity.SocialProvider;
import com.tftgogo.domain.member.service.MemberService;
import com.tftgogo.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = memberService.signup(request);
        return ResponseEntity.ok(ApiResponse.success("회원가입 성공", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = memberService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
    }

    @Override
    @GetMapping("/social/{provider}")
    public ResponseEntity<ApiResponse<SocialLoginStartResponse>> getSocialLoginStartUrl(
            @PathVariable("provider") String provider
    ) {
        SocialProvider socialProvider = SocialProvider.fromRegistrationId(provider);
        String authorizationUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/oauth2/authorization/{provider}")
                .buildAndExpand(socialProvider.registrationId())
                .toUriString();

        return ResponseEntity.ok(ApiResponse.success(
                "소셜 로그인 시작 URL 조회 성공",
                SocialLoginStartResponse.of(authorizationUrl)
        ));
    }
}
