package com.tftgogo.domain.chat.service.impl;

import com.tftgogo.domain.chat.dto.request.ChatMessageRequest;
import com.tftgogo.domain.chat.dto.response.ChatMessageResponse;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryChatServiceTest {

    private final InMemoryChatService chatService = new InMemoryChatService();

    @Test
    void 빈_채팅방은_빈_목록을_반환한다() {
        // given
        String roomId = "party-recruitment";

        // when
        List<ChatMessageResponse> response = chatService.getMessages(roomId, 100);

        // then
        assertThat(response).isEmpty();
    }

    @Test
    void 메시지를_저장하고_공백을_정리해_조회한다() {
        // given
        ChatMessageRequest request = chatMessageRequest("  소정  ", " Diamond ", "  안녕하세요  ");

        // when
        ChatMessageResponse sentMessage = chatService.sendMessage(" party-recruitment ", request);
        List<ChatMessageResponse> response = chatService.getMessages("party-recruitment", 100);

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0)).isSameAs(sentMessage);
        assertThat(response.get(0))
                .extracting(
                        ChatMessageResponse::getRoomId,
                        ChatMessageResponse::getSenderName,
                        ChatMessageResponse::getSenderTier,
                        ChatMessageResponse::getMessage
                )
                .containsExactly("party-recruitment", "소정", "Diamond", "안녕하세요");
    }

    @Test
    void 최근_100개_메시지만_보관한다() {
        // given
        String roomId = "general";
        for (int index = 1; index <= 105; index++) {
            chatService.sendMessage(roomId, chatMessageRequest("소정", "Diamond", "메시지 " + index));
        }

        // when
        List<ChatMessageResponse> response = chatService.getMessages(roomId, 100);

        // then
        assertThat(response).hasSize(100);
        assertThat(response.get(0).getMessage()).isEqualTo("메시지 6");
        assertThat(response.get(99).getMessage()).isEqualTo("메시지 105");
    }

    @Test
    void 조회_limit은_1에서_100_사이로_보정한다() {
        // given
        String roomId = "qna";
        for (int index = 1; index <= 3; index++) {
            chatService.sendMessage(roomId, chatMessageRequest("소정", "Diamond", "메시지 " + index));
        }

        // when
        List<ChatMessageResponse> negativeLimitResponse = chatService.getMessages(roomId, -1);
        List<ChatMessageResponse> overflowLimitResponse = chatService.getMessages(roomId, 200);

        // then
        assertThat(negativeLimitResponse).hasSize(1);
        assertThat(negativeLimitResponse.get(0).getMessage()).isEqualTo("메시지 3");
        assertThat(overflowLimitResponse).hasSize(3);
    }

    @Test
    void 필수값이_비어있으면_INVALID_INPUT을_던진다() {
        // given
        ChatMessageRequest request = chatMessageRequest(" ", "Diamond", "안녕하세요");

        // when, then
        assertThatThrownBy(() -> chatService.sendMessage("general", request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 동시_전송에서도_최대_100개를_유지한다() throws InterruptedException {
        // given
        int messageCount = 120;
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(messageCount);

        // when
        for (int index = 1; index <= messageCount; index++) {
            int messageIndex = index;
            executorService.submit(() -> {
                try {
                    chatService.sendMessage(
                            "general",
                            chatMessageRequest("소정", "Diamond", "메시지 " + messageIndex)
                    );
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(3, TimeUnit.SECONDS);
        executorService.shutdownNow();
        List<ChatMessageResponse> response = chatService.getMessages("general", 100);

        // then
        assertThat(completed).isTrue();
        assertThat(response).hasSize(100);
    }

    private ChatMessageRequest chatMessageRequest(String senderName, String senderTier, String message) {
        ChatMessageRequest request = new ChatMessageRequest();
        ReflectionTestUtils.setField(request, "senderName", senderName);
        ReflectionTestUtils.setField(request, "senderTier", senderTier);
        ReflectionTestUtils.setField(request, "message", message);
        return request;
    }
}
