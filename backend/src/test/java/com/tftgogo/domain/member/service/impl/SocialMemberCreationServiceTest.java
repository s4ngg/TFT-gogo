package com.tftgogo.domain.member.service.impl;

import com.tftgogo.domain.member.dto.command.SocialLoginCommand;
import com.tftgogo.domain.member.entity.Member;
import com.tftgogo.domain.member.entity.SocialProvider;
import com.tftgogo.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialMemberCreationServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private SocialMemberCreationService socialMemberCreationService;

    @Test
    void 소셜회원은_비밀번호없이_소셜정보로_저장한다() {
        // given
        SocialLoginCommand command = SocialLoginCommand.of(
                SocialProvider.GOOGLE,
                "google-1",
                "social@example.com",
                "소셜회원",
                "https://example.com/profile.png"
        );
        Member savedMember = Member.builder()
                .email("social@example.com")
                .passwordHash(null)
                .nickname("소셜회원")
                .profileImage("https://example.com/profile.png")
                .socialProvider("google")
                .socialId("google-1")
                .build();

        when(memberRepository.saveAndFlush(any(Member.class))).thenReturn(savedMember);
        when(memberRepository.existsByNickname("소셜회원")).thenReturn(false);

        // when
        Member member = socialMemberCreationService.create(command);

        // then
        assertThat(member).isSameAs(savedMember);
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).saveAndFlush(memberCaptor.capture());
        assertThat(memberCaptor.getValue())
                .extracting(
                        Member::getEmail,
                        Member::getPasswordHash,
                        Member::getNickname,
                        Member::getProfileImage,
                        Member::getSocialProvider,
                        Member::getSocialId
                )
                .containsExactly(
                        "social@example.com",
                        null,
                        "소셜회원",
                        "https://example.com/profile.png",
                        "google",
                        "google-1"
                );
    }

    @Test
    void 소셜회원_닉네임이_이미_있으면_식별_접미사를_붙여_저장한다() {
        // given
        SocialLoginCommand command = SocialLoginCommand.of(
                SocialProvider.GOOGLE,
                "google-1",
                "social@example.com",
                "소셜회원",
                "https://example.com/profile.png"
        );
        Member savedMember = Member.builder()
                .email("social@example.com")
                .passwordHash(null)
                .nickname("소셜회원-duplicate")
                .profileImage("https://example.com/profile.png")
                .socialProvider("google")
                .socialId("google-1")
                .build();

        when(memberRepository.existsByNickname("소셜회원")).thenReturn(true);
        when(memberRepository.saveAndFlush(any(Member.class))).thenReturn(savedMember);

        // when
        Member member = socialMemberCreationService.create(command);

        // then
        assertThat(member).isSameAs(savedMember);
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).saveAndFlush(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getNickname())
                .startsWith("소셜회원-")
                .hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void 소셜회원_닉네임_재시도는_시도_번호가_포함된_다른_후보로_저장한다() {
        // given
        SocialLoginCommand command = SocialLoginCommand.of(
                SocialProvider.GOOGLE,
                "google-2",
                "social2@example.com",
                "소셜회원",
                "https://example.com/profile.png"
        );
        Member savedMember = Member.builder()
                .email("social2@example.com")
                .passwordHash(null)
                .nickname("소셜회원-retry")
                .profileImage("https://example.com/profile.png")
                .socialProvider("google")
                .socialId("google-2")
                .build();

        when(memberRepository.saveAndFlush(any(Member.class))).thenReturn(savedMember);

        // when
        Member member = socialMemberCreationService.create(command, 1);

        // then
        assertThat(member).isSameAs(savedMember);
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).saveAndFlush(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getNickname())
                .startsWith("소셜회원-1-")
                .hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void 소셜회원_첫_닉네임_후보도_오십자로_제한한다() {
        // given
        String longNickname = "가".repeat(60);
        String truncatedNickname = "가".repeat(50);
        SocialLoginCommand command = SocialLoginCommand.of(
                SocialProvider.GOOGLE,
                "google-3",
                "social3@example.com",
                longNickname,
                "https://example.com/profile.png"
        );
        Member savedMember = Member.builder()
                .email("social3@example.com")
                .passwordHash(null)
                .nickname(truncatedNickname)
                .profileImage("https://example.com/profile.png")
                .socialProvider("google")
                .socialId("google-3")
                .build();

        when(memberRepository.existsByNickname(truncatedNickname)).thenReturn(false);
        when(memberRepository.saveAndFlush(any(Member.class))).thenReturn(savedMember);

        // when
        Member member = socialMemberCreationService.create(command);

        // then
        assertThat(member).isSameAs(savedMember);
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).saveAndFlush(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getNickname())
                .isEqualTo(truncatedNickname)
                .hasSize(50);
    }
}
