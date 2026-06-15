package com.tftgogo.global.security.oauth;

import com.tftgogo.domain.member.dto.command.SocialLoginCommand;
import com.tftgogo.domain.member.entity.SocialProvider;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;

import java.util.Locale;
import java.util.Map;

public final class SocialOAuth2UserInfo {

    private static final int EMAIL_MAX_LENGTH = 255;
    private static final int NICKNAME_MAX_LENGTH = 50;
    private static final int PROFILE_IMAGE_MAX_LENGTH = 500;

    private SocialOAuth2UserInfo() {
    }

    public static SocialLoginCommand toCommand(SocialProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> fromGoogle(provider, attributes);
            case KAKAO -> fromKakao(provider, attributes);
            case NAVER -> fromNaver(provider, attributes);
        };
    }

    private static SocialLoginCommand fromGoogle(SocialProvider provider, Map<String, Object> attributes) {
        if (!isTrue(attributes.get("email_verified"))) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }

        String email = requiredEmail(attributes, "email");

        return SocialLoginCommand.of(
                provider,
                normalizeId(requiredString(attributes, "sub")),
                email,
                normalizeNickname(optionalString(attributes, "name"), email),
                normalizeProfileImage(optionalString(attributes, "picture"))
        );
    }

    private static SocialLoginCommand fromKakao(SocialProvider provider, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = nestedMap(attributes, "kakao_account");
        Object verified = kakaoAccount.get("is_email_verified");
        if (verified != null && !isTrue(verified)) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }

        Map<String, Object> profile = nestedMap(kakaoAccount, "profile");
        String email = requiredEmail(kakaoAccount, "email");

        return SocialLoginCommand.of(
                provider,
                normalizeId(requiredString(attributes, "id")),
                email,
                normalizeNickname(optionalString(profile, "nickname"), email),
                normalizeProfileImage(firstPresent(
                        optionalString(profile, "profile_image_url"),
                        optionalString(profile, "thumbnail_image_url")
                ))
        );
    }

    private static SocialLoginCommand fromNaver(SocialProvider provider, Map<String, Object> attributes) {
        Map<String, Object> response = nestedMap(attributes, "response");
        String email = requiredEmail(response, "email");

        return SocialLoginCommand.of(
                provider,
                normalizeId(requiredString(response, "id")),
                email,
                normalizeNickname(optionalString(response, "nickname"), email),
                normalizeProfileImage(optionalString(response, "profile_image"))
        );
    }

    private static String normalizeId(String value) {
        if (value.length() > EMAIL_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return value;
    }

    private static String normalizeEmail(String value) {
        String email = value.trim().toLowerCase(Locale.ROOT);
        if (email.length() > EMAIL_MAX_LENGTH || !email.contains("@")) {
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_REQUIRED);
        }
        return email;
    }

    private static String normalizeNickname(String value, String email) {
        String nickname = value == null || value.isBlank()
                ? email.substring(0, email.indexOf('@'))
                : value.trim();

        if (nickname.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }

        return nickname.length() > NICKNAME_MAX_LENGTH
                ? nickname.substring(0, NICKNAME_MAX_LENGTH)
                : nickname;
    }

    private static String normalizeProfileImage(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String profileImage = value.trim();
        return profileImage.length() > PROFILE_IMAGE_MAX_LENGTH ? null : profileImage;
    }

    private static String requiredString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);

        if (value == null) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }

        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }

        return text;
    }

    private static String requiredEmail(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);

        if (value == null) {
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_REQUIRED);
        }

        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_REQUIRED);
        }

        return normalizeEmail(text);
    }

    private static String optionalString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private static Map<String, Object> nestedMap(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .filter(entry -> entry.getKey() instanceof String)
                    .collect(
                            java.util.stream.Collectors.toMap(
                                    entry -> (String) entry.getKey(),
                                    Map.Entry::getValue
                            )
                    );
        }

        throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
    }

    private static boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static String firstPresent(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
