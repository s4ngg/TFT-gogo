package com.tftgogo.global.security.oauth;

public enum SocialOAuth2ErrorCode {

    EMAIL_EXISTS("email_exists"),
    EMAIL_REQUIRED("email_required"),
    PROVIDER_ERROR("provider_error");

    private final String value;

    SocialOAuth2ErrorCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
