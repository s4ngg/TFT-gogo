package com.tftgogo.domain.member.service.impl;

import com.tftgogo.domain.member.dto.response.AuthResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    private static final long REFRESH_TOKEN_EXPIRATION_MILLIS = 1_209_600_000L;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Mock
    private AccessTokenBlocklistRepository accessTokenBlocklistRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthTokenService authTokenService;

    @Test
    void refresh는_기존_세션을_폐기하고_새_토큰을_발급한다() {
        // given
        Member member = member();
        when(jwtProperties.getRefreshTokenExpirationMillis()).thenReturn(REFRESH_TOKEN_EXPIRATION_MILLIS);
        when(jwtTokenProvider.createAccessToken(1L, 0L)).thenReturn("access-token-1", "access-token-2");
        when(refreshTokenSessionRepository.save(any(RefreshTokenSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse issued = authTokenService.issue(member);
        RefreshTokenSession issuedSession = savedRefreshSession();
        when(refreshTokenSessionRepository.findByTokenHashForUpdate(issuedSession.getTokenHash()))
                .thenReturn(Optional.of(issuedSession));

        // when
        AuthResponse refreshed = authTokenService.refresh(issued.getRefreshToken());

        // then
        assertThat(refreshed.getAccessToken()).isEqualTo("access-token-2");
        assertThat(issuedSession.isRevoked()).isTrue();
        verify(refreshTokenSessionRepository, times(2)).save(any(RefreshTokenSession.class));
    }

    @Test
    void 폐기된_refreshToken이_재사용되면_활성_세션을_모두_폐기한다() {
        // given
        Member member = member();
        when(jwtProperties.getRefreshTokenExpirationMillis()).thenReturn(REFRESH_TOKEN_EXPIRATION_MILLIS);
        when(jwtTokenProvider.createAccessToken(1L, 0L)).thenReturn("access-token");
        when(refreshTokenSessionRepository.save(any(RefreshTokenSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse issued = authTokenService.issue(member);
        RefreshTokenSession reusedSession = savedRefreshSession();
        RefreshTokenSession activeSession = RefreshTokenSession.of(
                member,
                "active-session-hash",
                LocalDateTime.now().plusDays(1)
        );
        reusedSession.revoke(LocalDateTime.now());

        when(refreshTokenSessionRepository.findByTokenHashForUpdate(reusedSession.getTokenHash()))
                .thenReturn(Optional.of(reusedSession));
        when(refreshTokenSessionRepository.findByMemberUserIdAndRevokedFalse(1L))
                .thenReturn(List.of(activeSession));

        // when, then
        assertThatThrownBy(() -> authTokenService.refresh(issued.getRefreshToken()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        assertThat(reusedSession.isReuseDetected()).isTrue();
        assertThat(activeSession.isRevoked()).isTrue();
    }

    @Test
    void 로그아웃은_accessToken을_차단하고_refreshToken_세션을_폐기한다() {
        // given
        Member member = member();
        when(jwtProperties.getRefreshTokenExpirationMillis()).thenReturn(REFRESH_TOKEN_EXPIRATION_MILLIS);
        when(jwtTokenProvider.createAccessToken(1L, 0L)).thenReturn("access-token");
        when(refreshTokenSessionRepository.save(any(RefreshTokenSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse issued = authTokenService.issue(member);
        RefreshTokenSession refreshSession = savedRefreshSession();
        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getTokenId("access-token")).thenReturn("token-id");
        when(jwtTokenProvider.getUserId("access-token")).thenReturn(1L);
        when(jwtTokenProvider.getExpiration("access-token"))
                .thenReturn(Date.from(Instant.now().plusSeconds(60)));
        when(refreshTokenSessionRepository.findByTokenHash(refreshSession.getTokenHash()))
                .thenReturn(Optional.of(refreshSession));

        // when
        authTokenService.logout(1L, "access-token", issued.getRefreshToken());

        // then
        ArgumentCaptor<AccessTokenBlocklist> blocklistCaptor = ArgumentCaptor.forClass(AccessTokenBlocklist.class);
        verify(accessTokenBlocklistRepository).save(blocklistCaptor.capture());
        assertThat(blocklistCaptor.getValue().getTokenId()).isEqualTo("token-id");
        assertThat(blocklistCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(refreshSession.isRevoked()).isTrue();
    }

    @Test
    void 차단되지_않고_활성_회원의_토큰이면_사용_가능하다() {
        // given
        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getTokenId("access-token")).thenReturn("token-id");
        when(jwtTokenProvider.getUserId("access-token")).thenReturn(1L);
        when(jwtTokenProvider.getAuthTokenVersion("access-token")).thenReturn(0L);
        when(accessTokenBlocklistRepository.existsByTokenIdAndExpiresAtAfter(eq("token-id"), any(LocalDateTime.class)))
                .thenReturn(false);
        when(memberRepository.existsByUserIdAndAuthTokenVersionAndDeletedAtIsNull(1L, 0L))
                .thenReturn(true);

        // when, then
        assertThat(authTokenService.isAccessTokenUsable("access-token")).isTrue();
    }

    @Test
    void 차단된_accessToken은_사용할_수_없다() {
        // given
        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getTokenId("access-token")).thenReturn("token-id");
        when(jwtTokenProvider.getUserId("access-token")).thenReturn(1L);
        when(jwtTokenProvider.getAuthTokenVersion("access-token")).thenReturn(0L);
        when(accessTokenBlocklistRepository.existsByTokenIdAndExpiresAtAfter(eq("token-id"), any(LocalDateTime.class)))
                .thenReturn(true);

        // when, then
        assertThat(authTokenService.isAccessTokenUsable("access-token")).isFalse();
        verify(memberRepository, never()).existsByUserIdAndAuthTokenVersionAndDeletedAtIsNull(anyLong(), anyLong());
    }

    @Test
    void accessToken_파싱_예외가_발생하면_사용할_수_없다() {
        // given
        when(jwtTokenProvider.validateToken("broken-token")).thenReturn(true);
        when(jwtTokenProvider.getTokenId("broken-token")).thenThrow(new JwtException("invalid jwt"));

        // when, then
        assertThat(authTokenService.isAccessTokenUsable("broken-token")).isFalse();
        verify(accessTokenBlocklistRepository, never()).existsByTokenIdAndExpiresAtAfter(anyString(), any(LocalDateTime.class));
        verify(memberRepository, never()).existsByUserIdAndAuthTokenVersionAndDeletedAtIsNull(anyLong(), anyLong());
    }

    private RefreshTokenSession savedRefreshSession() {
        ArgumentCaptor<RefreshTokenSession> captor = ArgumentCaptor.forClass(RefreshTokenSession.class);
        verify(refreshTokenSessionRepository).save(captor.capture());
        return captor.getValue();
    }

    private Member member() {
        Member member = Member.builder()
                .email("sojung@example.com")
                .passwordHash("encoded-password")
                .nickname("소정")
                .build();
        ReflectionTestUtils.setField(member, "userId", 1L);
        return member;
    }
}
