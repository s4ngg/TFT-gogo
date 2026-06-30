package com.tftgogo.domain.member.service.impl;

import com.tftgogo.domain.member.dto.command.SocialLoginCommand;
import com.tftgogo.domain.member.dto.request.LoginRequest;
import com.tftgogo.domain.member.dto.request.SignupRequest;
import com.tftgogo.domain.member.dto.response.AuthResponse;
import com.tftgogo.domain.member.dto.response.MemberResponse;
import com.tftgogo.domain.member.entity.Member;
import com.tftgogo.domain.member.entity.SocialProvider;
import com.tftgogo.domain.member.repository.MemberRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
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
import static org.mockito.ArgumentMatchers.anyInt;
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
    private AuthTokenService authTokenService;

    @Mock
    private SocialMemberCreationService socialMemberCreationService;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Test
    void 회원가입은_비밀번호를_암호화하고_토큰과_사용자정보를_반환한다() {
        // given
        SignupRequest request = signupRequest("sojung@example.com", "password123", "소정");
        Member savedMember = member("sojung@example.com", "encoded-password", "소정");
        ReflectionTestUtils.setField(savedMember, "userId", 1L);

        when(memberRepository.existsByEmail("sojung@example.com")).thenReturn(false);
        when(memberRepository.existsByNickname("소정")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(memberRepository.saveAndFlush(any(Member.class))).thenReturn(savedMember);
        when(authTokenService.issue(savedMember)).thenReturn(authResponse(savedMember));

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
        verify(authTokenService).issue(savedMember);
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
    void 이미_사용중인_닉네임이면_비밀번호_암호화나_저장을_하지_않는다() {
        // given
        SignupRequest request = signupRequest("sojung@example.com", "password123", "소정");

        when(memberRepository.existsByEmail("sojung@example.com")).thenReturn(false);
        when(memberRepository.existsByNickname("소정")).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NICKNAME_ALREADY_EXISTS));

        verify(passwordEncoder, never()).encode(anyString());
        verify(memberRepository, never()).saveAndFlush(any(Member.class));
    }

    @Test
    void 저장_중_중복_이메일_제약조건이_터져도_회원가입_중복_예외로_변환한다() {
        // given
        SignupRequest request = signupRequest("sojung@example.com", "password123", "소정");

        when(memberRepository.existsByEmail("sojung@example.com")).thenReturn(false);
        when(memberRepository.existsByNickname("소정")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(memberRepository.saveAndFlush(any(Member.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate email"));

        // when, then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
    }

    @Test
    void 저장_중_중복_닉네임_제약조건이_터지면_닉네임_중복_예외로_변환한다() {
        // given
        SignupRequest request = signupRequest("sojung@example.com", "password123", "소정");

        when(memberRepository.existsByEmail("sojung@example.com")).thenReturn(false);
        when(memberRepository.existsByNickname("소정")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(memberRepository.saveAndFlush(any(Member.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'nickname'"));

        // when, then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NICKNAME_ALREADY_EXISTS));
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
        when(authTokenService.issue(member)).thenReturn(authResponse(member));

        // when
        AuthResponse response = memberService.login(request);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser())
                .extracting(MemberResponse::getEmail, MemberResponse::getNickname)
                .containsExactly("sojung@example.com", "소정");
    }

    @Test
    void 일반_로그인은_소셜회원의_null_비밀번호를_공통_인증실패로_처리한다() {
        // given
        LoginRequest request = loginRequest("social@example.com", "password123");
        Member member = socialMember("social@example.com", "소셜회원", "google", "google-1");

        when(memberRepository.findByEmail("social@example.com")).thenReturn(Optional.of(member));

        // when, then
        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void 소셜로그인은_기존_소셜회원이면_토큰과_사용자정보를_반환한다() {
        // given
        SocialLoginCommand command = socialLoginCommand("social@example.com", "소셜회원");
        Member member = socialMember("social@example.com", "소셜회원", "google", "google-1");
        ReflectionTestUtils.setField(member, "userId", 1L);

        when(memberRepository.findBySocialProviderAndSocialId("google", "google-1"))
                .thenReturn(Optional.of(member));
        when(authTokenService.issue(member)).thenReturn(authResponse(member));

        // when
        AuthResponse response = memberService.socialLogin(command);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser())
                .extracting(MemberResponse::getEmail, MemberResponse::getNickname)
                .containsExactly("social@example.com", "소셜회원");
        verify(memberRepository, never()).saveAndFlush(any(Member.class));
    }

    @Test
    void 소셜로그인은_신규회원이면_비밀번호없이_소셜정보로_저장한다() {
        // given
        SocialLoginCommand command = socialLoginCommand("social@example.com", "소셜회원");
        Member savedMember = socialMember("social@example.com", "소셜회원", "google", "google-1");
        ReflectionTestUtils.setField(savedMember, "userId", 1L);

        when(memberRepository.findBySocialProviderAndSocialId("google", "google-1"))
                .thenReturn(Optional.empty());
        when(memberRepository.existsByEmail("social@example.com")).thenReturn(false);
        when(socialMemberCreationService.create(command, 0)).thenReturn(savedMember);
        when(authTokenService.issue(savedMember)).thenReturn(authResponse(savedMember));

        // when
        AuthResponse response = memberService.socialLogin(command);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(socialMemberCreationService).create(command, 0);
    }

    @Test
    void 소셜로그인은_같은_이메일의_일반회원이_있으면_자동연결하지_않는다() {
        // given
        SocialLoginCommand command = socialLoginCommand("sojung@example.com", "소셜회원");

        when(memberRepository.findBySocialProviderAndSocialId("google", "google-1"))
                .thenReturn(Optional.empty());
        when(memberRepository.existsByEmail("sojung@example.com")).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> memberService.socialLogin(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));

        verify(socialMemberCreationService, never()).create(any(SocialLoginCommand.class), anyInt());
    }

    @Test
    void 소셜로그인_신규저장중_소셜제약조건이_깨지면_재조회한_회원으로_토큰을_반환한다() {
        // given
        SocialLoginCommand command = socialLoginCommand("social@example.com", "소셜회원");
        Member existingMember = socialMember("social@example.com", "소셜회원", "google", "google-1");
        ReflectionTestUtils.setField(existingMember, "userId", 1L);

        when(memberRepository.findBySocialProviderAndSocialId("google", "google-1"))
                .thenReturn(Optional.empty(), Optional.of(existingMember));
        when(memberRepository.existsByEmail("social@example.com")).thenReturn(false);
        when(socialMemberCreationService.create(command, 0))
                .thenThrow(new DataIntegrityViolationException("duplicate social provider"));
        when(authTokenService.issue(existingMember)).thenReturn(authResponse(existingMember));

        // when
        AuthResponse response = memberService.socialLogin(command);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser())
                .extracting(MemberResponse::getEmail, MemberResponse::getNickname)
                .containsExactly("social@example.com", "소셜회원");
    }

    @Test
    void 소셜로그인_신규저장중_제약조건이_깨지고_재조회도_실패하면_소셜로그인_실패로_처리한다() {
        // given
        SocialLoginCommand command = socialLoginCommand("social@example.com", "소셜회원");

        when(memberRepository.findBySocialProviderAndSocialId("google", "google-1"))
                .thenReturn(Optional.empty());
        when(memberRepository.existsByEmail("social@example.com")).thenReturn(false);
        when(socialMemberCreationService.create(command, 0))
                .thenThrow(new DataIntegrityViolationException("duplicate social provider"));

        // when, then
        assertThatThrownBy(() -> memberService.socialLogin(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SOCIAL_LOGIN_FAILED));
    }

    @Test
    void 소셜로그인_신규저장중_이메일_제약조건이_깨지면_이메일_중복으로_처리한다() {
        // given
        SocialLoginCommand command = socialLoginCommand("social@example.com", "소셜회원");

        when(memberRepository.findBySocialProviderAndSocialId("google", "google-1"))
                .thenReturn(Optional.empty());
        when(memberRepository.existsByEmail("social@example.com")).thenReturn(false);
        when(socialMemberCreationService.create(command, 0))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'email'"));

        // when, then
        assertThatThrownBy(() -> memberService.socialLogin(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
    }

    @Test
    void 소셜로그인_닉네임_충돌이면_새_닉네임_후보로_재시도한다() {
        // given
        SocialLoginCommand command = socialLoginCommand("social@example.com", "소셜회원");
        Member savedMember = socialMember("social@example.com", "소셜회원-1-abcd", "google", "google-1");
        ReflectionTestUtils.setField(savedMember, "userId", 1L);

        when(memberRepository.findBySocialProviderAndSocialId("google", "google-1"))
                .thenReturn(Optional.empty(), Optional.empty());
        when(memberRepository.existsByEmail("social@example.com")).thenReturn(false);
        when(socialMemberCreationService.create(command, 0))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'nickname'"));
        when(socialMemberCreationService.create(command, 1)).thenReturn(savedMember);
        when(authTokenService.issue(savedMember)).thenReturn(authResponse(savedMember));

        // when
        AuthResponse response = memberService.socialLogin(command);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser())
                .extracting(MemberResponse::getNickname)
                .isEqualTo("소셜회원-1-abcd");
        verify(socialMemberCreationService).create(command, 0);
        verify(socialMemberCreationService).create(command, 1);
    }

    @Test
    void 소셜로그인_닉네임_충돌이_최대_재시도까지_반복되면_소셜로그인_실패로_처리한다() {
        // given
        SocialLoginCommand command = socialLoginCommand("social@example.com", "소셜회원");
        DataIntegrityViolationException nicknameViolation =
                new DataIntegrityViolationException("Duplicate entry for key 'nickname'");

        when(memberRepository.findBySocialProviderAndSocialId("google", "google-1"))
                .thenReturn(Optional.empty());
        when(memberRepository.existsByEmail("social@example.com")).thenReturn(false);
        when(socialMemberCreationService.create(command, 0)).thenThrow(nicknameViolation);
        when(socialMemberCreationService.create(command, 1)).thenThrow(nicknameViolation);
        when(socialMemberCreationService.create(command, 2)).thenThrow(nicknameViolation);
        when(socialMemberCreationService.create(command, 3)).thenThrow(nicknameViolation);

        // when, then
        assertThatThrownBy(() -> memberService.socialLogin(command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SOCIAL_LOGIN_FAILED));

        verify(socialMemberCreationService).create(command, 0);
        verify(socialMemberCreationService).create(command, 1);
        verify(socialMemberCreationService).create(command, 2);
        verify(socialMemberCreationService).create(command, 3);
        verify(socialMemberCreationService, never()).create(command, 4);
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

    @Test
    void refresh는_AuthTokenService에_위임한다() {
        // given
        Member member = member("sojung@example.com", "encoded-password", "소정");
        ReflectionTestUtils.setField(member, "userId", 1L);
        when(authTokenService.refresh("refresh-token")).thenReturn(authResponse(member));

        // when
        AuthResponse response = memberService.refresh("refresh-token");

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(authTokenService).refresh("refresh-token");
    }

    @Test
    void logout은_AuthTokenService에_위임한다() {
        // when
        memberService.logout(1L, "access-token", "refresh-token");

        // then
        verify(authTokenService).logout(1L, "access-token", "refresh-token");
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

    private AuthResponse authResponse(Member member) {
        return AuthResponse.of("access-token", MemberResponse.from(member), "refresh-token");
    }

    private SocialLoginCommand socialLoginCommand(String email, String nickname) {
        return SocialLoginCommand.of(
                SocialProvider.GOOGLE,
                "google-1",
                email,
                nickname,
                "https://example.com/profile.png"
        );
    }

    private Member socialMember(String email, String nickname, String socialProvider, String socialId) {
        return Member.builder()
                .email(email)
                .passwordHash(null)
                .nickname(nickname)
                .profileImage("https://example.com/profile.png")
                .socialProvider(socialProvider)
                .socialId(socialId)
                .build();
    }
}
