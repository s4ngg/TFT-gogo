package com.tftgogo.domain.member.service.impl;

import com.tftgogo.domain.member.dto.command.SocialLoginCommand;
import com.tftgogo.domain.member.entity.Member;
import com.tftgogo.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SocialMemberCreationService {

    private static final int MAX_NICKNAME_LENGTH = 50;
    private static final int NICKNAME_SUFFIX_LENGTH = 12;

    private final MemberRepository memberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Member create(SocialLoginCommand command) {
        return create(command, 0);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Member create(SocialLoginCommand command, int attempt) {
        Member member = Member.builder()
                .email(command.getEmail())
                .passwordHash(null)
                .nickname(resolveNickname(command, attempt))
                .profileImage(command.getProfileImage())
                .socialProvider(command.getProvider().registrationId())
                .socialId(command.getSocialId())
                .build();

        return memberRepository.saveAndFlush(member);
    }

    private String resolveNickname(SocialLoginCommand command, int attempt) {
        String baseNickname = command.getNickname()
                .substring(0, Math.min(command.getNickname().length(), MAX_NICKNAME_LENGTH));
        if (attempt == 0 && !memberRepository.existsByNickname(baseNickname)) {
            return baseNickname;
        }

        String suffix = buildNicknameSuffix(command, attempt);
        int prefixLength = MAX_NICKNAME_LENGTH - suffix.length();

        return baseNickname.substring(0, Math.min(baseNickname.length(), prefixLength)) + suffix;
    }

    private String buildNicknameSuffix(SocialLoginCommand command, int attempt) {
        String suffix = "-" + attempt + "-" + Long.toUnsignedString(
                Integer.toUnsignedLong(Objects.hash(
                        command.getProvider().registrationId(),
                        command.getSocialId(),
                        attempt
                )),
                36
        );

        if (suffix.length() > NICKNAME_SUFFIX_LENGTH) {
            suffix = suffix.substring(0, NICKNAME_SUFFIX_LENGTH);
        }

        return suffix;
    }
}
