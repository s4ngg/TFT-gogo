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
    private static final int NICKNAME_SUFFIX_LENGTH = 7;

    private final MemberRepository memberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Member create(SocialLoginCommand command) {
        Member member = Member.builder()
                .email(command.getEmail())
                .passwordHash(null)
                .nickname(resolveNickname(command))
                .profileImage(command.getProfileImage())
                .socialProvider(command.getProvider().registrationId())
                .socialId(command.getSocialId())
                .build();

        return memberRepository.saveAndFlush(member);
    }

    private String resolveNickname(SocialLoginCommand command) {
        String nickname = command.getNickname();
        if (!memberRepository.existsByNickname(nickname)) {
            return nickname;
        }

        String suffix = "-" + Long.toUnsignedString(
                Integer.toUnsignedLong(Objects.hash(command.getProvider().registrationId(), command.getSocialId())),
                36
        );
        if (suffix.length() > NICKNAME_SUFFIX_LENGTH) {
            suffix = suffix.substring(0, NICKNAME_SUFFIX_LENGTH);
        }

        int prefixLength = MAX_NICKNAME_LENGTH - suffix.length();
        return nickname.substring(0, Math.min(nickname.length(), prefixLength)) + suffix;
    }
}
