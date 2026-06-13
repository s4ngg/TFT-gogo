package com.tftgogo.global.riot.queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class RiotQueue {

    private static final Logger logger = LogManager.getLogger(RiotQueue.class);

    private final LinkedBlockingQueue<RiotTask<?>> queue = new LinkedBlockingQueue<>();

    public RiotQueue() {
        Thread worker = new Thread(this::processLoop, "riot-queue-worker");
        worker.setDaemon(true);
        worker.start();
    }

    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        queue.offer(new RiotTask<>(task, future));
        return future;
    }

    private void processLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                RiotTask<?> task = queue.poll(1, TimeUnit.SECONDS);
                if (task != null) {
                    task.execute();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("RiotQueue 처리 중 예외 발생", e);
            }
        }
    }

    private record RiotTask<T>(Supplier<T> supplier, CompletableFuture<T> future) {
        void execute() {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }
    }
}
