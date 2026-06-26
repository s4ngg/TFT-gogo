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
import com.tftgogo.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private static final int SOCIAL_MEMBER_CREATE_MAX_ATTEMPTS = 4;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SocialMemberCreationService socialMemberCreationService;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member;

        try {
            member = memberRepository.saveAndFlush(request.toEntity(encodedPassword));
        } catch (DataIntegrityViolationException e) {
            throw mapSignupConstraintViolation(e);
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getUserId());

        return AuthResponse.of(accessToken, MemberResponse.from(member));
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

        String accessToken = jwtTokenProvider.createAccessToken(member.getUserId());

        return AuthResponse.of(accessToken, MemberResponse.from(member));

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

    private AuthResponse createSocialMember(SocialLoginCommand command) {
        String provider = command.getProvider().registrationId();

        if (memberRepository.existsByEmail(command.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        for (int attempt = 0; attempt < SOCIAL_MEMBER_CREATE_MAX_ATTEMPTS; attempt++) {
            try {
                return issueAuthResponse(socialMemberCreationService.create(command, attempt));
            } catch (DataIntegrityViolationException e) {
                Optional<Member> existingMember = memberRepository.findBySocialProviderAndSocialId(
                        provider,
                        command.getSocialId()
                );
                if (existingMember.isPresent()) {
                    return issueAuthResponse(existingMember.get());
                }

                if (isNicknameConstraintViolation(e)) {
                    if (attempt == SOCIAL_MEMBER_CREATE_MAX_ATTEMPTS - 1) {
                        throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
                    }
                    continue;
                }

                if (isEmailConstraintViolation(e)) {
                    throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
                }

                throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
            }
        }

        throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
    }

    private AuthResponse issueAuthResponse(Member member) {
        String accessToken = jwtTokenProvider.createAccessToken(member.getUserId());

        return AuthResponse.of(accessToken, MemberResponse.from(member));
    }

    private BusinessException mapSignupConstraintViolation(DataIntegrityViolationException exception) {
        if (isNicknameConstraintViolation(exception)) {
            return new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        return new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    private boolean isNicknameConstraintViolation(DataIntegrityViolationException exception) {
        return containsConstraintToken(exception, "nickname");
    }

    private boolean isEmailConstraintViolation(DataIntegrityViolationException exception) {
        return containsConstraintToken(exception, "email");
    }

    private boolean containsConstraintToken(DataIntegrityViolationException exception, String token) {
        String message = exception.getMostSpecificCause().getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains(token);
    }
}
