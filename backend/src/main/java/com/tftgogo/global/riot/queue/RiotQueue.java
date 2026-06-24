package com.tftgogo.global.riot.queue;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class RiotQueue implements DisposableBean {

    private static final Logger logger = LogManager.getLogger(RiotQueue.class);
    private static final int MAX_QUEUE_SIZE = 200;

    // foreground: 사용자 직접 요청 (matchId 조회) — background보다 우선 처리
    private final LinkedBlockingQueue<RiotTask<?>> foregroundQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    // background: 매치 상세 수집
    private final LinkedBlockingQueue<RiotTask<?>> backgroundQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final Thread worker;

    public RiotQueue() {
        worker = new Thread(this::processLoop, "riot-queue-worker");
        worker.setDaemon(true);
        worker.start();
    }

    /** 사용자 요청 경로 — background 작업보다 우선 처리됨 */
    public <T> CompletableFuture<T> submitForeground(Supplier<T> task) {
        return enqueue(foregroundQueue, task);
    }

    /** 백그라운드 수집 경로 */
    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        return enqueue(backgroundQueue, task);
    }

    /** foreground + background 큐 합산 대기 작업 수 — 상태 모니터링용 */
    public int getPendingTaskCount() {
        return foregroundQueue.size() + backgroundQueue.size();
    }

    private <T> CompletableFuture<T> enqueue(LinkedBlockingQueue<RiotTask<?>> queue, Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (!queue.offer(new RiotTask<>(task, future))) {
            future.completeExceptionally(new BusinessException(ErrorCode.RIOT_API_ERROR));
        }
        return future;
    }

    @Override
    public void destroy() {
        worker.interrupt();
        drainWithException(foregroundQueue);
        drainWithException(backgroundQueue);
    }

    private void drainWithException(LinkedBlockingQueue<RiotTask<?>> queue) {
        RiotTask<?> task;
        while ((task = queue.poll()) != null) {
            task.future().completeExceptionally(new BusinessException(ErrorCode.RIOT_API_ERROR));
        }
    }

    private void processLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // foreground 우선 처리, 없으면 background 대기
                RiotTask<?> task = foregroundQueue.poll();
                if (task == null) {
                    task = backgroundQueue.poll(50, TimeUnit.MILLISECONDS);
                }
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
