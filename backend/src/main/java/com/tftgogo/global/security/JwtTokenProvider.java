package com.tftgogo.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String ACCESS_TOKEN_AUDIENCE = "tftgogo-api";
    private static final String TOKEN_VERSION_CLAIM = "tokenVersion";

    private final JwtProperties jwtProperties;

    public String createAccessToken(Long userId) {
        return createAccessToken(userId, 0L);
    }

    public String createAccessToken(Long userId, long authTokenVersion) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getAccessTokenExpirationMillis());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .audience()
                .add(ACCESS_TOKEN_AUDIENCE)
                .and()
                .claim(TOKEN_VERSION_CLAIM, authTokenVersion)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public Long getUserId(String token) {
        String subject = parseClaims(token).getSubject();
        return Long.valueOf(subject);
    }

    public String getTokenId(String token) {
        return parseClaims(token).getId();
    }

    public long getAuthTokenVersion(String token) {
        Object tokenVersion = parseClaims(token).get(TOKEN_VERSION_CLAIM);

        if (!(tokenVersion instanceof Number number)) {
            throw new IllegalArgumentException("JWT tokenVersion claim is invalid.");
        }

        return number.longValue();
    }

    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String subject = claims.getSubject();
            String tokenId = claims.getId();

            if ((subject == null) || subject.isBlank() || tokenId == null || tokenId.isBlank()) {
                return false;
            }

            Set<String> audience = claims.getAudience();
            if (audience == null || !audience.contains(ACCESS_TOKEN_AUDIENCE)) {
                return false;
            }

            if (!(claims.get(TOKEN_VERSION_CLAIM) instanceof Number)) {
                return false;
            }

            Long.valueOf(subject);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
