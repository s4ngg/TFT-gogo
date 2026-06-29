package com.tftgogo.domain.member.service;

import com.tftgogo.domain.member.dto.request.SocialLoginCommand;
import com.tftgogo.domain.member.dto.request.LoginRequest;
import com.tftgogo.domain.member.dto.request.SignupRequest;
import com.tftgogo.domain.member.dto.response.AuthResponse;
import com.tftgogo.domain.member.dto.response.MemberResponse;

public interface MemberService {

    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse socialLogin(SocialLoginCommand command);

    AuthResponse refresh(String refreshToken);

    void logout(Long userId, String accessToken, String refreshToken);

    MemberResponse getMe(Long userId);
}
