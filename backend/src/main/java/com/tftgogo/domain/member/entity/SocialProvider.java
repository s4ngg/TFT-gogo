package com.tftgogo.domain.member.entity;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;

import java.util.Locale;

public enum SocialProvider {

    GOOGLE,
    KAKAO,
    NAVER;

    public String registrationId() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SocialProvider fromRegistrationId(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }

        try {
            return SocialProvider.valueOf(registrationId.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
