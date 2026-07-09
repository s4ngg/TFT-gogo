package com.tftgogo.domain.community.chat.service.impl;

import com.tftgogo.domain.community.chat.dto.request.ChatMessageCreateRequest;
import com.tftgogo.domain.community.chat.dto.response.ChatMessageResponse;
import com.tftgogo.domain.community.chat.model.ChatMessage;
import com.tftgogo.domain.community.chat.service.ChatMessageStore;
import com.tftgogo.domain.community.chat.service.ChatRealtimePublisher;
import com.tftgogo.domain.community.chat.service.ChatSseHub;
import com.tftgogo.domain.member.entity.Member;
import com.tftgogo.domain.member.repository.MemberRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistentChatServiceImplTest {

    private static final Long USER_ID = 1L;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatMessageStore chatMessageStore;

    @Mock
    private ChatRealtimePublisher chatRealtimePublisher;

    @Mock
    private ChatSseHub chatSseHub;

    @InjectMocks
    private PersistentChatServiceImpl chatService;

    @Test
    void 메시지를_저장한_뒤_Redis_fanout으로_발행한다() {
        // given
        Member member = member("소정");
        ChatMessageCreateRequest request = request("general", "  안녕하세요  ");
        ChatMessage savedMessage = chatMessage("message-1", "general", "소정", "안녕하세요");

        when(memberRepository.findById(USER_ID)).thenReturn(Optional.of(member));
        when(chatMessageStore.saveMessage("general", member, "안녕하세요")).thenReturn(savedMessage);

        // when
        ChatMessageResponse response = chatService.sendMessage(USER_ID, request);

        // then
        assertThat(response)
                .extracting(
                        ChatMessageResponse::getId,
                        ChatMessageResponse::getRoomId,
                        ChatMessageResponse::getSenderName,
                        ChatMessageResponse::getTier,
                        ChatMessageResponse::getContent
                )
                .containsExactly("message-1", "general", "소정", "Unranked", "안녕하세요");

        ArgumentCaptor<ChatMessageResponse> responseCaptor = ArgumentCaptor.forClass(ChatMessageResponse.class);
        verify(chatRealtimePublisher).publish(eq("general"), responseCaptor.capture());
        assertThat(responseCaptor.getValue().getContent()).isEqualTo("안녕하세요");
    }

    @Test
    void 지원하지_않는_방_ID는_저장하거나_발행하지_않는다() {
        // given
        ChatMessageCreateRequest request = request("party-1", "안녕하세요");

        // when, then
        assertThatThrownBy(() -> chatService.sendMessage(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(memberRepository, chatMessageStore, chatRealtimePublisher, chatSseHub);
    }

    @Test
    void SSE_구독은_DB_snapshot을_읽고_hub에_등록한다() {
        // given
        ChatMessage savedMessage = chatMessage("message-1", "general", "소정", "안녕하세요");
        SseEmitter emitter = new SseEmitter();

        when(chatMessageStore.getRecentMessages("general")).thenReturn(List.of(savedMessage));
        when(chatSseHub.subscribe(eq("general"), any())).thenReturn(emitter);

        // when
        SseEmitter response = chatService.subscribe("general");

        // then
        assertThat(response).isSameAs(emitter);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Supplier<List<ChatMessageResponse>>> snapshotCaptor =
                ArgumentCaptor.forClass((Class) Supplier.class);
        verify(chatSseHub).subscribe(eq("general"), snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().get())
                .hasSize(1)
                .first()
                .extracting(ChatMessageResponse::getContent)
                .isEqualTo("안녕하세요");
    }

    @Test
    void 채팅방_준비는_지원_방_ID만_저장소에_위임한다() {
        // when
        chatService.ensureRoom("party-recruitment");

        // then
        verify(chatMessageStore).ensureRoom("party-recruitment");
    }

    private ChatMessageCreateRequest request(String roomId, String content) {
        ChatMessageCreateRequest request = new ChatMessageCreateRequest();
        ReflectionTestUtils.setField(request, "roomId", roomId);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }

    private Member member(String nickname) {
        Member member = Member.builder()
                .email("sojung@example.com")
                .passwordHash("encoded-password")
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(member, "userId", USER_ID);
        return member;
    }

    private ChatMessage chatMessage(String id, String roomId, String senderName, String content) {
        return new ChatMessage(
                id,
                roomId,
                USER_ID,
                senderName,
                "Unranked",
                content,
                Instant.parse("2026-06-25T00:00:00Z")
        );
    }
}
