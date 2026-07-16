package com.tftgogo.domain.member.dto.request;

import com.tftgogo.domain.member.entity.SocialProvider;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialLoginCommand {

    private SocialProvider provider;
    private String socialId;
    private String email;
    private String nickname;
    private String profileImage;

    public static SocialLoginCommand of(
            SocialProvider provider,
            String socialId,
            String email,
            String nickname,
            String profileImage
    ) {
        return SocialLoginCommand.builder()
                .provider(provider)
                .socialId(socialId)
                .email(email)
                .nickname(nickname)
                .profileImage(profileImage)
                .build();
    }
}
