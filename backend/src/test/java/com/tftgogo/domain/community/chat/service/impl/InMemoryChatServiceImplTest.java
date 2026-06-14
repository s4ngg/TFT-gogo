package com.tftgogo.domain.community.chat.service.impl;

import com.tftgogo.domain.community.chat.dto.request.ChatMessageCreateRequest;
import com.tftgogo.domain.community.chat.dto.response.ChatMessageResponse;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class InMemoryChatServiceImplTest {

    private final InMemoryChatServiceImpl chatService = new InMemoryChatServiceImpl();

    @Test
    void 채팅방_준비는_기존_메시지를_지우지_않는다() {
        // given
        ChatMessageCreateRequest request = request("party-10", "소정", "Diamond", "참여했습니다");
        ChatMessageResponse sentMessage = chatService.sendMessage(request);

        // when
        chatService.ensureRoom("party-10");
        List<ChatMessageResponse> messages = chatService.getRecentMessages("party-10");

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
        ChatMessageCreateRequest request = request("party-1", "소정", "Diamond", "  안녕하세요  ");

        // when
        ChatMessageResponse sentMessage = chatService.sendMessage(request);
        List<ChatMessageResponse> messages = chatService.getRecentMessages("party-1");

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
                .containsExactly(sentMessage.getId(), "party-1", "소정", "Diamond", "안녕하세요");
    }

    @Test
    void 방별_최근_메시지는_최대_100개만_유지한다() {
        // given
        for (int index = 1; index <= 101; index++) {
            chatService.sendMessage(request("general", "소정", "Master", "message-" + index));
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
        ChatMessageCreateRequest request = request("bad room", "소정", "Master", "안녕하세요");

        // when, then
        assertThatThrownBy(() -> chatService.sendMessage(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 빈_메시지는_INVALID_INPUT을_던진다() {
        // given
        ChatMessageCreateRequest request = request("general", "소정", "Master", "   ");

        // when, then
        assertThatThrownBy(() -> chatService.sendMessage(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private ChatMessageCreateRequest request(String roomId, String senderName, String tier, String content) {
        ChatMessageCreateRequest request = new ChatMessageCreateRequest();
        ReflectionTestUtils.setField(request, "roomId", roomId);
        ReflectionTestUtils.setField(request, "senderName", senderName);
        ReflectionTestUtils.setField(request, "tier", tier);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }
}
