package com.tftgogo.global.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieService {

    private final RefreshTokenCookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    public ResponseCookie createCookie(String refreshToken) {
        return ResponseCookie.from(cookieProperties.getName(), refreshToken)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(cookieProperties.getPath())
                .maxAge(Duration.ofMillis(jwtProperties.getRefreshTokenExpirationMillis()))
                .build();
    }

    public ResponseCookie clearCookie() {
        return ResponseCookie.from(cookieProperties.getName(), "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(cookieProperties.getPath())
                .maxAge(Duration.ZERO)
                .build();
    }

    public Optional<String> resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieProperties.getName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }
}
