package com.tftgogo.domain.member.service.impl;

import com.tftgogo.domain.member.dto.request.LoginRequest;
import com.tftgogo.domain.member.dto.request.SignupRequest;
import com.tftgogo.domain.member.dto.response.AuthResponse;
import com.tftgogo.domain.member.dto.response.MemberResponse;
import com.tftgogo.domain.member.entity.Member;
import com.tftgogo.domain.member.repository.MemberRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Test
    void 회원가입은_비밀번호를_암호화하고_토큰과_사용자정보를_반환한다() {
        // given
        SignupRequest request = signupRequest("sojung@example.com", "password123", "소정");
        Member savedMember = member("sojung@example.com", "encoded-password", "소정");
        ReflectionTestUtils.setField(savedMember, "userId", 1L);

        when(memberRepository.existsByEmail("sojung@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(memberRepository.saveAndFlush(any(Member.class))).thenReturn(savedMember);
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("access-token");

        // when
        AuthResponse response = memberService.signup(request);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser())
                .extracting(MemberResponse::getEmail, MemberResponse::getNickname)
                .containsExactly("sojung@example.com", "소정");
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).saveAndFlush(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getPasswordHash())
                .isEqualTo("encoded-password")
                .isNotEqualTo("password123");
        verify(jwtTokenProvider).createAccessToken(1L);
    }

    @Test
    void 이미_가입된_이메일이면_비밀번호_암호화나_저장을_하지_않는다() {
        // given
        SignupRequest request = signupRequest("sojung@example.com", "password123", "소정");
        when(memberRepository.existsByEmail("sojung@example.com")).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));

        verify(passwordEncoder, never()).encode(anyString());
        verify(memberRepository, never()).saveAndFlush(any(Member.class));
    }

    @Test
    void 저장_중_중복_이메일_제약조건이_터져도_회원가입_중복_예외로_변환한다() {
        // given
        SignupRequest request = signupRequest("sojung@example.com", "password123", "소정");

        when(memberRepository.existsByEmail("sojung@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(memberRepository.saveAndFlush(any(Member.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));

        // when, then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
    }

    @Test
    void 로그인은_이메일_존재여부와_비밀번호_오류를_같은_예외로_처리한다() {
        // given
        LoginRequest wrongEmailRequest = loginRequest("none@example.com", "password123");
        LoginRequest wrongPasswordRequest = loginRequest("sojung@example.com", "wrong-password");
        Member member = member("sojung@example.com", "encoded-password", "소정");

        when(memberRepository.findByEmail("none@example.com")).thenReturn(Optional.empty());
        when(memberRepository.findByEmail("sojung@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        // when, then
        assertThatThrownBy(() -> memberService.login(wrongEmailRequest))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS));
        assertThatThrownBy(() -> memberService.login(wrongPasswordRequest))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS));
    }

    @Test
    void 로그인_성공시_토큰과_사용자정보를_반환한다() {
        // given
        LoginRequest request = loginRequest("sojung@example.com", "password123");
        Member member = member("sojung@example.com", "encoded-password", "소정");
        ReflectionTestUtils.setField(member, "userId", 1L);

        when(memberRepository.findByEmail("sojung@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("access-token");

        // when
        AuthResponse response = memberService.login(request);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser())
                .extracting(MemberResponse::getEmail, MemberResponse::getNickname)
                .containsExactly("sojung@example.com", "소정");
    }

    @Test
    void 내정보_조회_성공시_사용자정보를_반환한다() {
        // given
        Member member = member("sojung@example.com", "encoded-password", "소정");
        ReflectionTestUtils.setField(member, "userId", 1L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        // when
        MemberResponse response = memberService.getMe(1L);

        // then
        assertThat(response)
                .extracting(MemberResponse::getEmail, MemberResponse::getNickname)
                .containsExactly("sojung@example.com", "소정");
    }

    @Test
    void 내정보_조회는_회원이_없으면_MEMBER_NOT_FOUND를_던진다() {
        // given
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> memberService.getMe(99L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 내정보_조회는_인증정보가_없으면_UNAUTHORIZED를_던진다() {
        // when, then
        assertThatThrownBy(() -> memberService.getMe(null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));

        verify(memberRepository, never()).findById(any());
    }

    private SignupRequest signupRequest(String email, String password, String nickname) {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "nickname", nickname);
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private Member member(String email, String passwordHash, String nickname) {
        return Member.builder()
                .email(email)
                .passwordHash(passwordHash)
                .nickname(nickname)
                .build();
    }
}
