package com.tftgogo.domain.member.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialLoginStartResponse {

    private String authorizationUrl;

    public static SocialLoginStartResponse of(String authorizationUrl) {
        return SocialLoginStartResponse.builder()
                .authorizationUrl(authorizationUrl)
                .build();
    }
}
