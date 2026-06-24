package com.tftgogo.domain.member.dto.response;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private MemberResponse user;

    public static AuthResponse of(String accessToken, MemberResponse user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .user(user)
                .build();
    }

}
