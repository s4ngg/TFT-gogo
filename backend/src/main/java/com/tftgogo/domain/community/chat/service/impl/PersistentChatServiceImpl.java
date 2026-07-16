package com.tftgogo.domain.community.chat.service.impl;

import com.tftgogo.domain.community.chat.dto.request.ChatMessageCreateRequest;
import com.tftgogo.domain.community.chat.dto.response.ChatMessageResponse;
import com.tftgogo.domain.community.chat.model.ChatMessage;
import com.tftgogo.domain.community.chat.model.CommunityChatRoomIds;
import com.tftgogo.domain.community.chat.service.ChatMessageStore;
import com.tftgogo.domain.community.chat.service.ChatRealtimePublisher;
import com.tftgogo.domain.community.chat.service.ChatService;
import com.tftgogo.domain.community.chat.service.ChatSseHub;
import com.tftgogo.domain.member.entity.Member;
import com.tftgogo.domain.member.repository.MemberRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.regex.Pattern;

@Service
@Profile("!local")
@RequiredArgsConstructor
public class PersistentChatServiceImpl implements ChatService {

    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,80}$");

    private final MemberRepository memberRepository;
    private final ChatMessageStore chatMessageStore;
    private final ChatRealtimePublisher chatRealtimePublisher;
    private final ChatSseHub chatSseHub;

    @Override
    public void ensureRoom(String roomId) {
        chatMessageStore.ensureRoom(normalizeSupportedRoomId(roomId));
    }

    @Override
    public List<ChatMessageResponse> getRecentMessages(String roomId) {
        String normalizedRoomId = normalizeSupportedRoomId(roomId);

        return chatMessageStore.getRecentMessages(normalizedRoomId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Override
    public ChatMessageResponse sendMessage(Long userId, ChatMessageCreateRequest request) {
        validateAuthenticated(userId);
        validateRequest(request);

        String roomId = normalizeSupportedRoomId(request.getRoomId());
        String content = normalizeText(request.getContent(), 500);
        Member sender = memberRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        ChatMessage savedMessage = chatMessageStore.saveMessage(roomId, sender, content);
        ChatMessageResponse response = ChatMessageResponse.from(savedMessage);

        chatRealtimePublisher.publish(roomId, response);
        return response;
    }

    @Override
    public SseEmitter subscribe(String roomId) {
        String normalizedRoomId = normalizeSupportedRoomId(roomId);

        return chatSseHub.subscribe(normalizedRoomId, () -> getRecentMessages(normalizedRoomId));
    }

    private void validateAuthenticated(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateRequest(ChatMessageCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private String normalizeSupportedRoomId(String roomId) {
        String normalizedRoomId = normalizeText(roomId, 80);

        if (!ROOM_ID_PATTERN.matcher(normalizedRoomId).matches()
                || !CommunityChatRoomIds.isSupported(normalizedRoomId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return normalizedRoomId;
    }

    private String normalizeText(String value, int maxLength) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String trimmedValue = value.trim();

        if (trimmedValue.isEmpty() || trimmedValue.length() > maxLength) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return trimmedValue;
    }
}
