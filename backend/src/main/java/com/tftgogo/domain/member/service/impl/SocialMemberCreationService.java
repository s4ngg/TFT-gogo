package com.tftgogo.domain.member.service.impl;

import com.tftgogo.domain.member.dto.command.SocialLoginCommand;
import com.tftgogo.domain.member.entity.Member;
import com.tftgogo.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialMemberCreationService {

    private final MemberRepository memberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Member create(SocialLoginCommand command) {
        Member member = Member.builder()
                .email(command.getEmail())
                .passwordHash(null)
                .nickname(command.getNickname())
                .profileImage(command.getProfileImage())
                .socialProvider(command.getProvider().registrationId())
                .socialId(command.getSocialId())
                .build();

        return memberRepository.saveAndFlush(member);
    }
}
