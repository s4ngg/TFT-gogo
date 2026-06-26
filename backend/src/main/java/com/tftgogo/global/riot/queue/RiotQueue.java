package com.tftgogo.global.riot.queue;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.config.RiotProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
public class RiotQueue implements DisposableBean {

    private static final Logger logger = LogManager.getLogger(RiotQueue.class);
    private static final int MAX_QUEUE_SIZE = 200;

    private final LinkedBlockingQueue<RiotTask<?>> foregroundQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final LinkedBlockingQueue<RiotTask<?>> backgroundQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

    private final Semaphore semaphore;
    private final Executor riotQueueExecutor;
    private final Thread scheduler;
    private final int maxForegroundStreak;
    private final long foregroundTaskTtlMs;
    private final long backgroundTaskTtlMs;
    private final AtomicInteger inflightCount = new AtomicInteger(0);
    private final ConcurrentHashMap<String, CompletableFuture<?>> deduplicationMap = new ConcurrentHashMap<>();
    private final Counter rejectedCounter;

    public RiotQueue(RiotProperties riotProperties,
                     @Qualifier("riotQueueExecutor") Executor riotQueueExecutor,
                     MeterRegistry meterRegistry) {
        this.semaphore = new Semaphore(riotProperties.getQueueWorkerConcurrency());
        this.riotQueueExecutor = riotQueueExecutor;
        this.maxForegroundStreak = riotProperties.getMaxForegroundStreak();
        this.foregroundTaskTtlMs = riotProperties.getForegroundTaskTtlMs();
        this.backgroundTaskTtlMs = riotProperties.getBackgroundTaskTtlMs();

        this.rejectedCounter = Counter.builder("riot.queue.rejected.count")
                .description("큐 포화로 거절된 작업 수")
                .register(meterRegistry);

        meterRegistry.gauge("riot.queue.foreground.size", foregroundQueue, LinkedBlockingQueue::size);
        meterRegistry.gauge("riot.queue.background.size", backgroundQueue, LinkedBlockingQueue::size);
        meterRegistry.gauge("riot.queue.inflight", inflightCount, AtomicInteger::get);

        scheduler = new Thread(this::scheduleLoop, "riot-queue-scheduler");
        scheduler.setDaemon(true);
        scheduler.start();
    }

    public <T> CompletableFuture<T> submitForeground(Supplier<T> task) {
        return enqueue(foregroundQueue, task, foregroundTaskTtlMs);
    }

    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> submitForeground(String dedupKey, Supplier<T> task) {
        if (dedupKey == null) {
            return enqueue(foregroundQueue, task, foregroundTaskTtlMs);
        }
        boolean[] created = {false};
        CompletableFuture<T> future = (CompletableFuture<T>) deduplicationMap.compute(dedupKey, (k, existing) -> {
            if (existing != null && !existing.isDone()) {
                return existing;
            }
            created[0] = true;
            return enqueue(foregroundQueue, task, foregroundTaskTtlMs);
        });
        if (created[0]) {
            future.whenComplete((r, ex) -> deduplicationMap.remove(dedupKey, future));
        }
        return future;
    }

    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        return enqueue(backgroundQueue, task, backgroundTaskTtlMs);
    }

    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> submit(String dedupKey, Supplier<T> task) {
        if (dedupKey == null) {
            return enqueue(backgroundQueue, task, backgroundTaskTtlMs);
        }
        boolean[] created = {false};
        CompletableFuture<T> future = (CompletableFuture<T>) deduplicationMap.compute(dedupKey, (k, existing) -> {
            if (existing != null && !existing.isDone()) {
                return existing;
            }
            created[0] = true;
            return enqueue(backgroundQueue, task, backgroundTaskTtlMs);
        });
        if (created[0]) {
            future.whenComplete((r, ex) -> deduplicationMap.remove(dedupKey, future));
        }
        return future;
    }

    public int getForegroundQueueSize() {
        return foregroundQueue.size();
    }

    public int getBackgroundQueueSize() {
        return backgroundQueue.size();
    }

    public int getInflightCount() {
        return inflightCount.get();
    }

    public int getPendingTaskCount() {
        return foregroundQueue.size() + backgroundQueue.size();
    }

    private <T> CompletableFuture<T> enqueue(LinkedBlockingQueue<RiotTask<?>> queue,
                                              Supplier<T> task, long ttlMs) {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (!queue.offer(new RiotTask<>(task, future, System.currentTimeMillis(), ttlMs))) {
            rejectedCounter.increment();
            future.completeExceptionally(new BusinessException(ErrorCode.RIOT_QUEUE_FULL));
        }
        return future;
    }

    @Override
    public void destroy() {
        scheduler.interrupt();
        drainWithException(foregroundQueue);
        drainWithException(backgroundQueue);
    }

    private void drainWithException(LinkedBlockingQueue<RiotTask<?>> queue) {
        RiotTask<?> task;
        while ((task = queue.poll()) != null) {
            task.future().completeExceptionally(new BusinessException(ErrorCode.RIOT_QUEUE_FULL));
        }
    }

    private void scheduleLoop() {
        int foregroundStreak = 0;

        while (!Thread.currentThread().isInterrupted()) {
            RiotTask<?> task = null;
            try {
                if (foregroundStreak >= maxForegroundStreak && !backgroundQueue.isEmpty()) {
                    task = backgroundQueue.poll();
                    foregroundStreak = 0;
                }

                if (task == null) {
                    task = foregroundQueue.poll();
                    if (task != null) {
                        foregroundStreak++;
                    }
                }

                if (task == null) {
                    task = backgroundQueue.poll(50, TimeUnit.MILLISECONDS);
                    if (task != null) {
                        foregroundStreak = 0;
                    }
                }

                if (task != null) {
                    semaphore.acquire();
                    dispatch(task);
                    task = null;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (task != null) {
                    task.future().completeExceptionally(new BusinessException(ErrorCode.RIOT_QUEUE_FULL));
                }
            } catch (Exception e) {
                logger.error("RiotQueue 스케줄러 예외", e);
                if (task != null) {
                    task.future().completeExceptionally(new BusinessException(ErrorCode.RIOT_QUEUE_FULL));
                }
            }
        }
    }

    private void dispatch(RiotTask<?> task) {
        inflightCount.incrementAndGet();
        try {
            riotQueueExecutor.execute(() -> {
                try {
                    task.execute();
                } finally {
                    inflightCount.decrementAndGet();
                    semaphore.release();
                }
            });
        } catch (RejectedExecutionException e) {
            inflightCount.decrementAndGet();
            semaphore.release();
            task.future().completeExceptionally(new BusinessException(ErrorCode.RIOT_QUEUE_FULL));
        }
    }

    private record RiotTask<T>(Supplier<T> supplier, CompletableFuture<T> future,
                                long createdAt, long ttlMs) {
        void execute() {
            if (ttlMs > 0 && System.currentTimeMillis() - createdAt > ttlMs) {
                future.completeExceptionally(new BusinessException(ErrorCode.RIOT_API_TIMEOUT));
                return;
            }
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }
    }
}
