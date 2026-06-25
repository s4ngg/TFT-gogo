package com.tftgogo.domain.member.service.impl;

import com.tftgogo.domain.member.dto.command.SocialLoginCommand;
import com.tftgogo.domain.member.dto.request.LoginRequest;
import com.tftgogo.domain.member.dto.request.SignupRequest;
import com.tftgogo.domain.member.dto.response.AuthResponse;
import com.tftgogo.domain.member.dto.response.MemberResponse;
import com.tftgogo.domain.member.entity.Member;
import com.tftgogo.domain.member.repository.MemberRepository;
import com.tftgogo.domain.member.service.MemberService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final SocialMemberCreationService socialMemberCreationService;
    private final AuthTokenService authTokenService;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member;

        try {
            member = memberRepository.saveAndFlush(request.toEntity(encodedPassword));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        return authTokenService.issue(member);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

        if (member.getPasswordHash() == null || member.getPasswordHash().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        if (!passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        return authTokenService.issue(member);

    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthResponse socialLogin(SocialLoginCommand command) {
        String provider = command.getProvider().registrationId();

        return memberRepository.findBySocialProviderAndSocialId(provider, command.getSocialId())
                .map(this::issueAuthResponse)
                .orElseGet(() -> createSocialMember(command));
    }

    @Override
    public MemberResponse getMe(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberResponse.from(member);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        return authTokenService.refresh(refreshToken);
    }

    @Override
    @Transactional
    public void logout(Long userId, String accessToken, String refreshToken) {
        authTokenService.logout(userId, accessToken, refreshToken);
    }

    private AuthResponse createSocialMember(SocialLoginCommand command) {
        String provider = command.getProvider().registrationId();

        if (memberRepository.existsByEmail(command.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        try {
            return issueAuthResponse(socialMemberCreationService.create(command));
        } catch (DataIntegrityViolationException e) {
            return memberRepository.findBySocialProviderAndSocialId(provider, command.getSocialId())
                    .map(this::issueAuthResponse)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED));
        }
    }

    private AuthResponse issueAuthResponse(Member member) {
        return authTokenService.issue(member);
    }
}
