package com.tftgogo.domain.member.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private MemberResponse user;
    @JsonIgnore
    private String refreshToken;

    public static AuthResponse of(String accessToken, MemberResponse user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .user(user)
                .build();
    }

    public static AuthResponse of(String accessToken, MemberResponse user, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .user(user)
                .refreshToken(refreshToken)
                .build();
    }

}
