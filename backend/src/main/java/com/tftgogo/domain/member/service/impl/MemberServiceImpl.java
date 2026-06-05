package com.tftgogo.domain.member.service.impl;

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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member;
        try {
            member = memberRepository.save(request.toEntity(encodedPassword));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        String accessToken = jwtTokenProvider.createAccessToken(member.getUserId());

        return AuthResponse.of(accessToken, MemberResponse.from(member));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getUserId());

        return AuthResponse.of(accessToken, MemberResponse.from(member));

    }

    @Override
    public MemberResponse getMe(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberResponse.from(member);
    }
}
