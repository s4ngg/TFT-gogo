package com.tftgogo.domain.chat.service.impl;

import com.tftgogo.domain.chat.dto.request.ChatMessageRequest;
import com.tftgogo.domain.chat.dto.response.ChatMessageResponse;
import com.tftgogo.domain.chat.service.ChatService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.response.ApiResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class InMemoryChatService implements ChatService {

    private static final Logger logger = LogManager.getLogger(InMemoryChatService.class);
    private static final int MAX_MESSAGES = 100;
    private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;
    private static final String DEFAULT_TIER = "Unranked";

    private final ConcurrentMap<String, RoomState> rooms = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public List<ChatMessageResponse> getMessages(String roomId, int limit) {
        RoomState state = getRoomState(roomId);
        int boundedLimit = Math.max(1, Math.min(limit, MAX_MESSAGES));

        state.lock.lock();
        try {
            List<ChatMessageResponse> snapshot = new ArrayList<>(state.messages);
            int fromIndex = Math.max(0, snapshot.size() - boundedLimit);
            return List.copyOf(snapshot.subList(fromIndex, snapshot.size()));
        } finally {
            state.lock.unlock();
        }
    }

    @Override
    public ChatMessageResponse sendMessage(String roomId, ChatMessageRequest request) {
        RoomState state = getRoomState(roomId);
        ChatMessageResponse response = ChatMessageResponse.of(
                normalizeRoomId(roomId),
                normalizeRequired(request.getSenderName()),
                normalizeOptional(request.getSenderTier(), DEFAULT_TIER),
                normalizeRequired(request.getMessage()),
                LocalDateTime.now(),
                sequence.incrementAndGet()
        );

        state.lock.lock();
        try {
            state.messages.addLast(response);
            while (state.messages.size() > MAX_MESSAGES) {
                state.messages.removeFirst();
            }
        } finally {
            state.lock.unlock();
        }

        broadcast(state, response);
        return response;
    }

    @Override
    public SseEmitter subscribe(String roomId) {
        RoomState state = getRoomState(roomId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        state.emitters.add(emitter);

        Runnable removeEmitter = () -> state.emitters.remove(emitter);
        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(removeEmitter);
        emitter.onError(error -> removeEmitter.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(ApiResponse.success("채팅 스트림 연결 성공")));
        } catch (IOException e) {
            state.emitters.remove(emitter);
            logger.warn("Failed to send chat stream handshake. roomId={}", normalizeRoomId(roomId));
        }

        return emitter;
    }

    private RoomState getRoomState(String roomId) {
        String normalizedRoomId = normalizeRoomId(roomId);
        return rooms.computeIfAbsent(normalizedRoomId, ignored -> new RoomState());
    }

    private String normalizeRoomId(String roomId) {
        String normalized = roomId == null ? "" : roomId.trim();

        if (normalized.isEmpty() || normalized.length() > 80) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return normalized;
    }

    private String normalizeRequired(String value) {
        String normalized = value == null ? "" : value.trim();

        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return normalized;
    }

    private String normalizeOptional(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private void broadcast(RoomState state, ChatMessageResponse message) {
        for (SseEmitter emitter : state.emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(ApiResponse.success("채팅 메시지 수신", message)));
            } catch (IOException e) {
                state.emitters.remove(emitter);
                logger.warn("Removed disconnected chat stream emitter. messageId={}", message.getId());
            }
        }
    }

    private static class RoomState {
        private final ReentrantLock lock = new ReentrantLock();
        private final Deque<ChatMessageResponse> messages = new ArrayDeque<>();
        private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    }
}
