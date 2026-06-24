package com.tftgogo.global.security.oauth;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;

import java.net.URI;
import java.util.Locale;

public final class OAuth2UrlValidator {

    private OAuth2UrlValidator() {
    }

    public static boolean isHttpOrHttpsAbsoluteUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            validate(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String normalizeHttpOrHttpsAbsoluteUrl(String value, ErrorCode errorCode) {
        try {
            return validate(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(errorCode);
        }
    }

    private static String validate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OAuth2 URL is required.");
        }

        String trimmedValue = value.trim();
        if (trimmedValue.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("OAuth2 URL contains whitespace.");
        }

        URI uri;
        try {
            uri = URI.create(trimmedValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("OAuth2 URL is invalid.", e);
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("OAuth2 URL scheme is required.");
        }

        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        if (!normalizedScheme.equals("http") && !normalizedScheme.equals("https")) {
            throw new IllegalArgumentException("OAuth2 URL scheme is not supported.");
        }

        if (uri.getHost() == null) {
            throw new IllegalArgumentException("OAuth2 URL host is required.");
        }

        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("OAuth2 URL userinfo is not allowed.");
        }

        return uri.toString();
    }
}
