package com.tftgogo.domain.community.chat.service.impl;

import com.tftgogo.domain.community.chat.dto.request.ChatMessageCreateRequest;
import com.tftgogo.domain.community.chat.dto.response.ChatMessageResponse;
import com.tftgogo.domain.member.entity.Member;
import com.tftgogo.domain.member.repository.MemberRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InMemoryChatServiceImplTest {

    private static final Long USER_ID = 1L;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private InMemoryChatServiceImpl chatService;

    @Test
    void 채팅방_준비는_기존_메시지를_지우지_않는다() {
        // given
        givenMember("소정");
        ChatMessageCreateRequest request = request("party-recruitment", "참여했습니다");
        ChatMessageResponse sentMessage = chatService.sendMessage(USER_ID, request);

        // when
        chatService.ensureRoom("party-recruitment");
        List<ChatMessageResponse> messages = chatService.getRecentMessages("party-recruitment");

        // then
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getId()).isEqualTo(sentMessage.getId());
    }

    @Test
    void 유효하지_않은_방_ID는_준비할_수_없다() {
        // when, then
        assertThatThrownBy(() -> chatService.ensureRoom("bad room"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 메시지를_전송하면_방별_최근_메시지로_조회된다() {
        // given
        givenMember("소정");
        ChatMessageCreateRequest request = request("party-recruitment", "  안녕하세요  ");

        // when
        ChatMessageResponse sentMessage = chatService.sendMessage(USER_ID, request);
        List<ChatMessageResponse> messages = chatService.getRecentMessages("party-recruitment");

        // then
        assertThat(sentMessage.getContent()).isEqualTo("안녕하세요");
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0))
                .extracting(
                        ChatMessageResponse::getId,
                        ChatMessageResponse::getRoomId,
                        ChatMessageResponse::getSenderName,
                        ChatMessageResponse::getTier,
                        ChatMessageResponse::getContent
                )
                .containsExactly(sentMessage.getId(), "party-recruitment", "소정", "Unranked", "안녕하세요");
    }

    @Test
    void 채팅_작성자는_요청값이_아니라_회원_닉네임을_사용한다() {
        // given
        givenMember("서버닉네임");
        ChatMessageCreateRequest request = request("party-recruitment", "안녕하세요");

        // when
        ChatMessageResponse response = chatService.sendMessage(USER_ID, request);

        // then
        assertThat(response.getSenderName()).isEqualTo("서버닉네임");
        assertThat(response.getTier()).isEqualTo("Unranked");
    }

    @Test
    void 방별_최근_메시지는_최대_100개만_유지한다() {
        // given
        givenMember("소정");
        for (int index = 1; index <= 101; index++) {
            chatService.sendMessage(USER_ID, request("general", "message-" + index));
        }

        // when
        List<ChatMessageResponse> messages = chatService.getRecentMessages("general");

        // then
        assertThat(messages).hasSize(100);
        assertThat(messages.get(0).getContent()).isEqualTo("message-2");
        assertThat(messages.get(99).getContent()).isEqualTo("message-101");
    }

    @Test
    void 유효하지_않은_방_ID는_INVALID_INPUT을_던진다() {
        // given
        ChatMessageCreateRequest request = request("bad room", "안녕하세요");

        // when, then
        assertThatThrownBy(() -> chatService.sendMessage(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(memberRepository);
    }

    @Test
    void 지원하지_않는_방_ID는_INVALID_INPUT을_던진다() {
        // given
        ChatMessageCreateRequest request = request("party-1", "안녕하세요");

        // when, then
        assertThatThrownBy(() -> chatService.sendMessage(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(memberRepository);
    }

    @Test
    void 빈_메시지는_INVALID_INPUT을_던진다() {
        // given
        ChatMessageCreateRequest request = request("general", "   ");

        // when, then
        assertThatThrownBy(() -> chatService.sendMessage(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(memberRepository);
    }

    @Test
    void 인증되지_않은_사용자는_메시지를_전송할_수_없다() {
        // given
        ChatMessageCreateRequest request = request("general", "안녕하세요");

        // when, then
        assertThatThrownBy(() -> chatService.sendMessage(null, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        verifyNoInteractions(memberRepository);
    }

    @Test
    void 요청이_없으면_메시지를_전송할_수_없다() {
        // when, then
        assertThatThrownBy(() -> chatService.sendMessage(USER_ID, null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(memberRepository);
    }

    @Test
    void 회원이_없으면_메시지를_전송할_수_없다() {
        // given
        ChatMessageCreateRequest request = request("general", "안녕하세요");
        when(memberRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> chatService.sendMessage(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    private ChatMessageCreateRequest request(String roomId, String content) {
        ChatMessageCreateRequest request = new ChatMessageCreateRequest();
        ReflectionTestUtils.setField(request, "roomId", roomId);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }

    private void givenMember(String nickname) {
        when(memberRepository.findById(USER_ID))
                .thenReturn(Optional.of(Member.builder()
                        .email("sojeong@example.com")
                        .passwordHash("encoded-password")
                        .nickname(nickname)
                        .build()));
    }
}
