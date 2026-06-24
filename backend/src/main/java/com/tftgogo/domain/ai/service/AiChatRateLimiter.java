package com.tftgogo.domain.ai.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiChatRateLimiter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<Long, UserBucket> buckets = new ConcurrentHashMap<>();

    public boolean tryAcquire(Long userId) {
        UserBucket bucket = buckets.computeIfAbsent(userId, k -> new UserBucket());
        return bucket.tryAcquire();
    }

    private static class UserBucket {
        private int count;
        private long windowStart;

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (windowStart == 0 || now - windowStart >= WINDOW_MS) {
                count = 0;
                windowStart = now;
            }
            if (count >= MAX_REQUESTS) {
                return false;
            }
            count++;
            return true;
        }
    }
}
