package com.tftgogo.domain.member.service.impl;

import com.tftgogo.domain.member.dto.response.AuthResponse;
import com.tftgogo.domain.member.dto.response.MemberResponse;
import com.tftgogo.domain.member.entity.AccessTokenBlocklist;
import com.tftgogo.domain.member.entity.Member;
import com.tftgogo.domain.member.entity.RefreshTokenSession;
import com.tftgogo.domain.member.repository.AccessTokenBlocklistRepository;
import com.tftgogo.domain.member.repository.MemberRepository;
import com.tftgogo.domain.member.repository.RefreshTokenSessionRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.security.JwtProperties;
import com.tftgogo.global.security.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthTokenService {

    private static final int REFRESH_TOKEN_BYTE_LENGTH = 64;

    private final MemberRepository memberRepository;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final AccessTokenBlocklistRepository accessTokenBlocklistRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse issue(Member member) {
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getUserId(), member.getAuthTokenVersion());
        String refreshToken = generateRefreshToken();
        LocalDateTime expiresAt = LocalDateTime.now()
                .plus(java.time.Duration.ofMillis(jwtProperties.getRefreshTokenExpirationMillis()));

        refreshTokenSessionRepository.save(RefreshTokenSession.of(
                member,
                hashRefreshToken(refreshToken),
                expiresAt
        ));

        return AuthResponse.of(accessToken, MemberResponse.from(member), refreshToken);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshTokenSession session = findRefreshTokenSession(refreshToken);
        LocalDateTime now = LocalDateTime.now();

        if (session.isRevoked()) {
            session.markReuseDetected(now);
            session.getMember().incrementAuthTokenVersion();
            revokeActiveRefreshTokens(session.getMember().getUserId(), now);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (session.isExpired(now) || !session.getMember().isActive()) {
            session.revoke(now);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        session.revoke(now);
        return issue(session.getMember());
    }

    @Transactional
    public void logout(Long userId, String accessToken, String refreshToken) {
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            blockAccessToken(accessToken);
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenSessionRepository.findByTokenHash(hashRefreshToken(refreshToken))
                    .filter(session -> userId == null || session.getMember().getUserId().equals(userId))
                    .filter(session -> !session.isRevoked())
                    .ifPresent(session -> session.revoke(LocalDateTime.now()));
        }
    }

    public boolean isAccessTokenUsable(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            return false;
        }

        try {
            String tokenId = jwtTokenProvider.getTokenId(token);
            Long userId = jwtTokenProvider.getUserId(token);
            long authTokenVersion = jwtTokenProvider.getAuthTokenVersion(token);
            LocalDateTime now = LocalDateTime.now();

            if (accessTokenBlocklistRepository.existsByTokenIdAndExpiresAtAfter(tokenId, now)) {
                return false;
            }

            return memberRepository.existsByUserIdAndAuthTokenVersionAndDeletedAtIsNull(userId, authTokenVersion);
        } catch (IllegalArgumentException | JwtException e) {
            return false;
        }
    }

    private RefreshTokenSession findRefreshTokenSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return refreshTokenSessionRepository.findByTokenHashForUpdate(hashRefreshToken(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private void blockAccessToken(String accessToken) {
        String tokenId = jwtTokenProvider.getTokenId(accessToken);
        Long userId = jwtTokenProvider.getUserId(accessToken);
        Date expiration = jwtTokenProvider.getExpiration(accessToken);

        accessTokenBlocklistRepository.save(AccessTokenBlocklist.of(
                tokenId,
                userId,
                LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault())
        ));
    }

    private void revokeActiveRefreshTokens(Long userId, LocalDateTime now) {
        refreshTokenSessionRepository.findByMemberUserIdAndRevokedFalse(userId)
                .forEach(session -> session.revoke(now));
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
