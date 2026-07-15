package com.tftgogo.domain.community.chat.service;

import com.tftgogo.domain.community.chat.dto.response.ChatMessageResponse;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
@Profile("!local")
public class ChatSseHub {

    private static final Logger logger = LogManager.getLogger(ChatSseHub.class);
    private static final int MAX_SSE_CONNECTIONS_PER_ROOM = 100;
    private static final int MAX_SSE_CONNECTIONS_TOTAL = 300;
    private static final long SSE_TIMEOUT_MILLIS = 10L * 60L * 1000L;

    private final ConcurrentHashMap<String, Set<SseEmitter>> roomEmitters = new ConcurrentHashMap<>();
    private final Object globalEmitterLock = new Object();

    public SseEmitter subscribe(String roomId, Supplier<List<ChatMessageResponse>> snapshotSupplier) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        Set<SseEmitter> emitters = roomEmitters.computeIfAbsent(
                roomId,
                ignored -> ConcurrentHashMap.newKeySet()
        );

        emitter.onCompletion(() -> removeEmitter(roomId, emitter));
        emitter.onTimeout(() -> removeEmitter(roomId, emitter));
        emitter.onError(error -> removeEmitter(roomId, emitter));

        synchronized (globalEmitterLock) {
            if (emitters.size() >= MAX_SSE_CONNECTIONS_PER_ROOM || totalEmitterCount() >= MAX_SSE_CONNECTIONS_TOTAL) {
                throw new BusinessException(ErrorCode.CHAT_STREAM_CONNECTION_LIMIT_EXCEEDED);
            }

            emitters.add(emitter);
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("snapshot")
                    .data(snapshotSupplier.get()));
        } catch (IOException | IllegalStateException e) {
            logger.warn("Failed to send chat snapshot. roomId={}", roomId, e);
            removeEmitter(roomId, emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void broadcast(String roomId, ChatMessageResponse message) {
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

    private int totalEmitterCount() {
        return roomEmitters.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    private void removeEmitter(String roomId, SseEmitter emitter) {
        Set<SseEmitter> emitters = roomEmitters.get(roomId);

        if (emitters == null) {
            return;
        }

        synchronized (globalEmitterLock) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                roomEmitters.remove(roomId, emitters);
            }
        }
    }
}
