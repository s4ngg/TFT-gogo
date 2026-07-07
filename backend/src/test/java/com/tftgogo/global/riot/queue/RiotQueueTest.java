package com.tftgogo.global.riot.queue;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.config.RiotProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiotQueueTest {

    private static final int WORKER_CONCURRENCY = 2;

    private RiotQueue riotQueue;

    @BeforeEach
    void setUp() {
        riotQueue = createQueue(WORKER_CONCURRENCY, WORKER_CONCURRENCY);
    }

    private RiotQueue createQueue(int workerConcurrency, int executorPoolSize) {
        RiotProperties props = new RiotProperties();
        props.setApiKey("test-key");
        props.setQueueWorkerConcurrency(workerConcurrency);
        props.setMaxForegroundStreak(5);
        props.setForegroundTaskTtlMs(5_000L);
        props.setBackgroundTaskTtlMs(10_000L);

        ThreadPoolTaskExecutorAdapter executor = new ThreadPoolTaskExecutorAdapter(executorPoolSize);
        return new RiotQueue(props, executor, new SimpleMeterRegistry());
    }

    @AfterEach
    void tearDown() {
        riotQueue.destroy();
    }

    @Test
    void 동일_dedupKey_동시_제출시_하나만_실행된다() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskRelease = new CountDownLatch(1);

        CompletableFuture<String> future1 = riotQueue.submitForeground("key-1", () -> {
            callCount.incrementAndGet();
            taskStarted.countDown();
            try { taskRelease.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return "result";
        });

        taskStarted.await(3, TimeUnit.SECONDS);

        CompletableFuture<String> future2 = riotQueue.submitForeground("key-1", () -> {
            callCount.incrementAndGet();
            return "result2";
        });

        assertThat(future1).isSameAs(future2);

        taskRelease.countDown();
        assertThat(future1.get(5, TimeUnit.SECONDS)).isEqualTo("result");
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void background_동일_dedupKey_제출시_중복_실행되지_않는다() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskRelease = new CountDownLatch(1);

        CompletableFuture<String> future1 = riotQueue.submit("bg-key-1", () -> {
            callCount.incrementAndGet();
            taskStarted.countDown();
            try { taskRelease.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return "bg-result";
        });

        taskStarted.await(3, TimeUnit.SECONDS);

        CompletableFuture<String> future2 = riotQueue.submit("bg-key-1", () -> {
            callCount.incrementAndGet();
            return "bg-result2";
        });

        assertThat(future1).isSameAs(future2);

        taskRelease.countDown();
        assertThat(future1.get(5, TimeUnit.SECONDS)).isEqualTo("bg-result");
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void 큐_포화시_RIOT_QUEUE_FULL_예외가_발생한다() {
        RiotQueue saturatedQueue = createQueue(0, 1);

        try {
            primeSchedulerBlock(saturatedQueue);
            CompletableFuture<String> overflowFuture = fillForegroundQueueUntilRejected(saturatedQueue);
            assertRiotQueueFull(overflowFuture);
        } finally {
            saturatedQueue.destroy();
        }
    }

    @Test
    void 큐_포화시_dedupKey_제출해도_IllegalStateException이_발생하지_않는다() {
        RiotQueue saturatedQueue = createQueue(0, 1);

        try {
            primeSchedulerBlock(saturatedQueue);
            fillForegroundQueueUntilRejected(saturatedQueue);
            CompletableFuture<String> dedupFuture = saturatedQueue.submitForeground("saturated-key", () -> "overflow");
            assertRiotQueueFull(dedupFuture);
        } finally {
            saturatedQueue.destroy();
        }
    }

    // 스케줄러가 태스크 1개를 dequeue한 직후 semaphore.acquire()로 영구 블록되는 시점을 먼저 확정해,
    // 이후 fillForegroundQueueUntilRejected/dedup 제출이 스케줄러 dequeue 타이밍과 경합하지 않게 한다.
    private void primeSchedulerBlock(RiotQueue targetQueue) {
        targetQueue.submitForeground(() -> "prime-block");
        waitUntil(() -> targetQueue.getForegroundQueueSize() == 0);
    }

    @Test
    void destroy_호출시_대기중인_작업이_예외로_완료된다() throws Exception {
        CountDownLatch workersRelease = blockForegroundWorkers(riotQueue);

        try {
            CompletableFuture<String> schedulerBlockedFuture = blockForegroundScheduler(riotQueue);
            CompletableFuture<String> pendingFuture = riotQueue.submit(() -> "pending");

            assertThat(riotQueue.getBackgroundQueueSize()).isEqualTo(1);

            riotQueue.destroy();

            waitUntil(schedulerBlockedFuture::isDone);
            waitUntil(pendingFuture::isDone);
            assertRiotQueueFull(schedulerBlockedFuture);
            assertRiotQueueFull(pendingFuture);
        } finally {
            workersRelease.countDown();
        }
    }

    @Test
    void foreground_작업이_background보다_우선_처리된다() throws Exception {
        RiotProperties props = new RiotProperties();
        props.setApiKey("test-key");
        props.setQueueWorkerConcurrency(1);
        props.setMaxForegroundStreak(5);
        props.setForegroundTaskTtlMs(5_000L);
        props.setBackgroundTaskTtlMs(10_000L);
        RiotQueue serialQueue = new RiotQueue(props, new ThreadPoolTaskExecutorAdapter(1), new SimpleMeterRegistry());

        try {
            List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch blockerStarted = new CountDownLatch(1);
            CountDownLatch blockerRelease = new CountDownLatch(1);

            serialQueue.submitForeground(() -> {
                blockerStarted.countDown();
                try { blockerRelease.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return "blocker";
            });
            blockerStarted.await(3, TimeUnit.SECONDS);

            serialQueue.submitForeground(() -> "sentinel");
            Thread.sleep(50);

            CompletableFuture<String> bgFuture = serialQueue.submit(() -> {
                executionOrder.add("bg");
                return "bg";
            });
            CompletableFuture<String> fgFuture = serialQueue.submitForeground(() -> {
                executionOrder.add("fg");
                return "fg";
            });

            blockerRelease.countDown();

            fgFuture.get(5, TimeUnit.SECONDS);
            bgFuture.get(5, TimeUnit.SECONDS);

            assertThat(executionOrder).containsExactly("fg", "bg");
        } finally {
            serialQueue.destroy();
        }
    }

    private static class ThreadPoolTaskExecutorAdapter implements Executor {
        private final ThreadPoolExecutor delegate;

        ThreadPoolTaskExecutorAdapter(int poolSize) {
            this.delegate = new ThreadPoolExecutor(
                    poolSize, poolSize, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(50),
                    new ThreadPoolExecutor.AbortPolicy());
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(command);
        }
    }

    private CompletableFuture<String> fillForegroundQueueUntilRejected(RiotQueue targetQueue) {
        for (int i = 0; i < 300; i++) {
            CompletableFuture<String> future = targetQueue.submitForeground(() -> "blocked");
            if (future.isCompletedExceptionally()) {
                return future;
            }
        }
        throw new AssertionError("큐 포화 상태를 만들지 못했습니다.");
    }

    private CountDownLatch blockForegroundWorkers(RiotQueue targetQueue) throws InterruptedException {
        CountDownLatch workersStarted = new CountDownLatch(WORKER_CONCURRENCY);
        CountDownLatch workersRelease = new CountDownLatch(1);

        for (int i = 0; i < WORKER_CONCURRENCY; i++) {
            targetQueue.submitForeground(() -> {
                workersStarted.countDown();
                try { workersRelease.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return "worker-blocked";
            });
        }

        assertThat(workersStarted.await(3, TimeUnit.SECONDS)).isTrue();
        return workersRelease;
    }

    private CompletableFuture<String> blockForegroundScheduler(RiotQueue targetQueue) {
        CompletableFuture<String> schedulerBlockedFuture = targetQueue.submitForeground(() -> "scheduler-blocked");

        waitUntil(() -> targetQueue.getForegroundQueueSize() == 0);
        assertThat(schedulerBlockedFuture.isDone()).isFalse();
        return schedulerBlockedFuture;
    }

    private void waitUntil(BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("대기 중 인터럽트가 발생했습니다.", e);
            }
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private void assertRiotQueueFull(CompletableFuture<String> future) {
        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(future::get)
                .hasCauseInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e.getCause();
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.RIOT_QUEUE_FULL);
                });
    }
}
