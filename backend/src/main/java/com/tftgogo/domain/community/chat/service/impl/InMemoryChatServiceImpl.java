package com.tftgogo.domain.community.chat.service.impl;

import com.tftgogo.domain.community.chat.dto.request.ChatMessageCreateRequest;
import com.tftgogo.domain.community.chat.dto.response.ChatMessageResponse;
import com.tftgogo.domain.community.chat.model.ChatMessage;
import com.tftgogo.domain.community.chat.service.ChatService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;

@Service
public class InMemoryChatServiceImpl implements ChatService {

    private static final Logger logger = LogManager.getLogger(InMemoryChatServiceImpl.class);
    private static final int MAX_MESSAGES_PER_ROOM = 100;
    private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;
    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,80}$");

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<ChatMessage>> roomMessages = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<SseEmitter>> roomEmitters = new ConcurrentHashMap<>();

    @Override
    public void ensureRoom(String roomId) {
        String normalizedRoomId = normalizeRoomId(roomId);
        roomMessages.computeIfAbsent(normalizedRoomId, ignored -> new ConcurrentLinkedDeque<>());
    }

    @Override
    public List<ChatMessageResponse> getRecentMessages(String roomId) {
        String normalizedRoomId = normalizeRoomId(roomId);
        ConcurrentLinkedDeque<ChatMessage> messages = roomMessages.get(normalizedRoomId);

        if (messages == null) {
            return List.of();
        }

        synchronized (messages) {
            return messages.stream()
                    .map(ChatMessageResponse::from)
                    .toList();
        }
    }

    @Override
    public ChatMessageResponse sendMessage(ChatMessageCreateRequest request) {
        String roomId = normalizeRoomId(request.getRoomId());
        String senderName = normalizeText(request.getSenderName(), 40);
        String tier = normalizeOptionalText(request.getTier(), 40);
        String content = normalizeText(request.getContent(), 500);
        ChatMessage message = new ChatMessage(
                UUID.randomUUID().toString(),
                roomId,
                senderName,
                tier,
                content,
                Instant.now()
        );
        ConcurrentLinkedDeque<ChatMessage> messages = roomMessages.computeIfAbsent(
                roomId,
                ignored -> new ConcurrentLinkedDeque<>()
        );

        synchronized (messages) {
            messages.addLast(message);
            while (messages.size() > MAX_MESSAGES_PER_ROOM) {
                messages.pollFirst();
            }
        }

        ChatMessageResponse response = ChatMessageResponse.from(message);
        broadcast(roomId, response);
        return response;
    }

    @Override
    public SseEmitter subscribe(String roomId) {
        String normalizedRoomId = normalizeRoomId(roomId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        Set<SseEmitter> emitters = roomEmitters.computeIfAbsent(
                normalizedRoomId,
                ignored -> ConcurrentHashMap.newKeySet()
        );

        emitters.add(emitter);
        emitter.onCompletion(() -> removeEmitter(normalizedRoomId, emitter));
        emitter.onTimeout(() -> removeEmitter(normalizedRoomId, emitter));
        emitter.onError(error -> removeEmitter(normalizedRoomId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("snapshot")
                    .data(getRecentMessages(normalizedRoomId)));
        } catch (IOException | IllegalStateException e) {
            logger.warn("Failed to send chat snapshot. roomId={}", normalizedRoomId);
            removeEmitter(normalizedRoomId, emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void broadcast(String roomId, ChatMessageResponse message) {
        Set<SseEmitter> emitters = roomEmitters.get(roomId);

        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> failedEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(message));
            } catch (IOException | IllegalStateException e) {
                failedEmitters.add(emitter);
            }
        }
        failedEmitters.forEach(emitter -> removeEmitter(roomId, emitter));
    }

    private void removeEmitter(String roomId, SseEmitter emitter) {
        Set<SseEmitter> emitters = roomEmitters.get(roomId);

        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            roomEmitters.remove(roomId, emitters);
        }
    }

    private String normalizeRoomId(String roomId) {
        String normalizedRoomId = normalizeText(roomId, 80);

        if (!ROOM_ID_PATTERN.matcher(normalizedRoomId).matches()) {
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

    private String normalizeOptionalText(String value, int maxLength) {
        if (value == null) {
            return "Unranked";
        }

        String trimmedValue = value.trim();

        if (trimmedValue.isEmpty()) {
            return "Unranked";
        }

        if (trimmedValue.length() > maxLength) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return trimmedValue;
    }
}
