package com.tftgogo.domain.admin.security;

import com.tftgogo.domain.admin.entity.AdminRole;
import com.tftgogo.global.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class AdminJwtTokenProvider {

    public static final String TOKEN_TYPE = "admin";
    private static final String CLAIM_TYP = "typ";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_USERNAME = "username";
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 30 * 60 * 1000L;   // 30분

    private final JwtProperties jwtProperties;

    public String createAccessToken(Long adminId, String username, AdminRole role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .claim(CLAIM_TYP, TOKEN_TYPE)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION_MS))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return TOKEN_TYPE.equals(claims.get(CLAIM_TYP, String.class))
                    && claims.getSubject() != null
                    && claims.get(CLAIM_ROLE, String.class) != null;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isAdminToken(String token) {
        try {
            return TOKEN_TYPE.equals(parseClaims(token).get(CLAIM_TYP, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getAdminId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getUsername(String token) {
        return parseClaims(token).get(CLAIM_USERNAME, String.class);
    }

    public AdminRole getRole(String token) {
        return AdminRole.valueOf(parseClaims(token).get(CLAIM_ROLE, String.class));
    }

    private Claims parseClaims(String token) {
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
