package com.tftgogo.global.security.oauth;

import com.tftgogo.domain.member.dto.command.SocialLoginCommand;
import com.tftgogo.domain.member.entity.SocialProvider;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocialOAuth2UserInfoTest {

    @Test
    void 구글_속성은_이메일인증이_확인되면_소셜로그인_명령으로_변환한다() {
        // given
        Map<String, Object> attributes = Map.of(
                "sub", "google-1",
                "email", "SOCIAL@example.com",
                "email_verified", true,
                "name", "소셜회원",
                "picture", "https://example.com/profile.png"
        );

        // when
        SocialLoginCommand command = SocialOAuth2UserInfo.toCommand(SocialProvider.GOOGLE, attributes);

        // then
        assertThat(command)
                .extracting(
                        SocialLoginCommand::getProvider,
                        SocialLoginCommand::getSocialId,
                        SocialLoginCommand::getEmail,
                        SocialLoginCommand::getNickname,
                        SocialLoginCommand::getProfileImage
                )
                .containsExactly(
                        SocialProvider.GOOGLE,
                        "google-1",
                        "social@example.com",
                        "소셜회원",
                        "https://example.com/profile.png"
                );
    }

    @Test
    void 구글_이메일이_검증되지_않으면_소셜로그인_실패로_처리한다() {
        // given
        Map<String, Object> attributes = Map.of(
                "sub", "google-1",
                "email", "social@example.com",
                "email_verified", false
        );

        // when, then
        assertThatThrownBy(() -> SocialOAuth2UserInfo.toCommand(SocialProvider.GOOGLE, attributes))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SOCIAL_LOGIN_FAILED));
    }

    @Test
    void 카카오_속성은_중첩된_계정과_프로필에서_값을_읽는다() {
        // given
        Map<String, Object> attributes = Map.of(
                "id", 12345L,
                "kakao_account", Map.of(
                        "email", "social@example.com",
                        "is_email_verified", true,
                        "profile", Map.of(
                                "nickname", "카카오회원",
                                "profile_image_url", "https://example.com/kakao.png"
                        )
                )
        );

        // when
        SocialLoginCommand command = SocialOAuth2UserInfo.toCommand(SocialProvider.KAKAO, attributes);

        // then
        assertThat(command)
                .extracting(
                        SocialLoginCommand::getProvider,
                        SocialLoginCommand::getSocialId,
                        SocialLoginCommand::getEmail,
                        SocialLoginCommand::getNickname,
                        SocialLoginCommand::getProfileImage
                )
                .containsExactly(
                        SocialProvider.KAKAO,
                        "12345",
                        "social@example.com",
                        "카카오회원",
                        "https://example.com/kakao.png"
                );
    }

    @Test
    void 네이버_속성은_response_객체에서_값을_읽고_긴_닉네임은_저장길이에_맞춘다() {
        // given
        String longNickname = "가".repeat(60);
        Map<String, Object> attributes = Map.of(
                "response", Map.of(
                        "id", "naver-1",
                        "email", "social@example.com",
                        "nickname", longNickname,
                        "profile_image", "https://example.com/naver.png"
                )
        );

        // when
        SocialLoginCommand command = SocialOAuth2UserInfo.toCommand(SocialProvider.NAVER, attributes);

        // then
        assertThat(command.getNickname()).hasSize(50);
        assertThat(command.getSocialId()).isEqualTo("naver-1");
        assertThat(command.getEmail()).isEqualTo("social@example.com");
    }

    @Test
    void 이메일이_없으면_소셜로그인_실패로_처리한다() {
        // given
        Map<String, Object> attributes = Map.of(
                "response", Map.of(
                        "id", "naver-1",
                        "nickname", "네이버회원"
                )
        );

        // when, then
        assertThatThrownBy(() -> SocialOAuth2UserInfo.toCommand(SocialProvider.NAVER, attributes))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SOCIAL_EMAIL_REQUIRED));
    }
}
