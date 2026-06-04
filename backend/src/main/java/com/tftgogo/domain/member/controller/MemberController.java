package com.tftgogo.domain.member.controller;

import com.tftgogo.domain.member.controller.docs.MemberControllerDocs;
import com.tftgogo.domain.member.dto.response.MemberResponse;
import com.tftgogo.domain.member.service.MemberService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController implements MemberControllerDocs {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMe(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        MemberResponse response = memberService.getMe(userId);
        return ResponseEntity.ok(ApiResponse.success("내 정보 조회 성공", response));
    }
}
