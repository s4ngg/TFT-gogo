package com.tftgogo.global.security.oauth;

import com.tftgogo.domain.member.dto.response.AuthResponse;
import com.tftgogo.domain.member.entity.SocialProvider;
import com.tftgogo.domain.member.service.MemberService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.security.RefreshTokenCookieService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SocialOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final MemberService memberService;
    private final OAuth2RedirectService redirectService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        try {
            if (!(authentication instanceof OAuth2AuthenticationToken oAuth2Token)) {
                throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
            }

            SocialProvider provider = SocialProvider.fromRegistrationId(oAuth2Token.getAuthorizedClientRegistrationId());
            AuthResponse authResponse = memberService.socialLogin(
                    SocialOAuth2UserInfo.toCommand(provider, oAuth2Token.getPrincipal().getAttributes())
            );

            clearOAuthSession(request);
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService
                    .createCookie(authResponse.getRefreshToken())
                    .toString());
            response.sendRedirect(redirectService.buildSuccessRedirectUri(authResponse.getAccessToken()));
        } catch (BusinessException e) {
            clearOAuthSession(request);
            response.sendRedirect(redirectService.buildFailureRedirectUri(toErrorCode(e)));
        } catch (Exception e) {
            clearOAuthSession(request);
            response.sendRedirect(redirectService.buildFailureRedirectUri(SocialOAuth2ErrorCode.PROVIDER_ERROR));
        }
    }

    private SocialOAuth2ErrorCode toErrorCode(BusinessException e) {
        if (e.getErrorCode() == ErrorCode.EMAIL_ALREADY_EXISTS) {
            return SocialOAuth2ErrorCode.EMAIL_EXISTS;
        }

        if (e.getErrorCode() == ErrorCode.SOCIAL_EMAIL_REQUIRED) {
            return SocialOAuth2ErrorCode.EMAIL_REQUIRED;
        }

        return SocialOAuth2ErrorCode.PROVIDER_ERROR;
    }

    private void clearOAuthSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
