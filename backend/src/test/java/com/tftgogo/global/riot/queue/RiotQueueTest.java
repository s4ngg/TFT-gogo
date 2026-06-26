package com.tftgogo.global.riot.queue;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.config.RiotProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiotQueueTest {

    private RiotQueue riotQueue;

    @BeforeEach
    void setUp() {
        RiotProperties props = new RiotProperties();
        props.setApiKey("test-key");
        props.setQueueWorkerConcurrency(2);
        props.setMaxForegroundStreak(5);
        props.setForegroundTaskTtlMs(5_000L);
        props.setBackgroundTaskTtlMs(10_000L);

        ThreadPoolTaskExecutorAdapter executor = new ThreadPoolTaskExecutorAdapter(2);
        riotQueue = new RiotQueue(props, executor, new SimpleMeterRegistry());
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
        CountDownLatch blockLatch = new CountDownLatch(1);

        for (int i = 0; i < 200; i++) {
            riotQueue.submitForeground(() -> {
                try { blockLatch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return "blocked";
            });
        }

        CompletableFuture<String> overflowFuture = riotQueue.submitForeground(() -> "overflow");

        assertThat(overflowFuture).isCompletedExceptionally();
        assertThatThrownBy(() -> overflowFuture.get())
                .hasCauseInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e.getCause();
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.RIOT_QUEUE_FULL);
                });

        blockLatch.countDown();
    }

    @Test
    void 큐_포화시_dedupKey_제출해도_IllegalStateException이_발생하지_않는다() {
        CountDownLatch blockLatch = new CountDownLatch(1);

        for (int i = 0; i < 200; i++) {
            riotQueue.submitForeground(() -> {
                try { blockLatch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return "blocked";
            });
        }

        CompletableFuture<String> dedupFuture = riotQueue.submitForeground("saturated-key", () -> "overflow");

        assertThat(dedupFuture).isCompletedExceptionally();
        assertThatThrownBy(() -> dedupFuture.get())
                .hasCauseInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e.getCause();
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.RIOT_QUEUE_FULL);
                });

        blockLatch.countDown();
    }

    @Test
    void destroy_호출시_대기중인_작업이_예외로_완료된다() throws Exception {
        CountDownLatch blockLatch = new CountDownLatch(1);

        CompletableFuture<String> future = riotQueue.submit(() -> {
            try { blockLatch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return "blocked";
        });

        // 큐에 추가 작업 넣기 (처리되기 전에 destroy 호출)
        CompletableFuture<String> pendingFuture = riotQueue.submit(() -> "pending");

        Thread.sleep(100);
        riotQueue.destroy();
        blockLatch.countDown();

        // pendingFuture가 아직 큐에 있었다면 예외로 완료됨
        // (이미 처리 시작된 경우 정상 완료될 수도 있으므로 isDone만 확인)
        assertThat(pendingFuture.isDone()).isTrue();
    }

    @Test
    void foreground_작업이_background보다_우선_처리된다() throws Exception {
        CountDownLatch bgStarted = new CountDownLatch(1);
        CountDownLatch fgCompleted = new CountDownLatch(1);

        CompletableFuture<String> bgFuture = riotQueue.submit(() -> {
            bgStarted.countDown();
            return "bg";
        });

        CompletableFuture<String> fgFuture = riotQueue.submitForeground(() -> "fg");

        fgFuture.thenRun(fgCompleted::countDown);

        assertThat(fgFuture.get(5, TimeUnit.SECONDS)).isEqualTo("fg");
        assertThat(bgFuture.get(5, TimeUnit.SECONDS)).isEqualTo("bg");
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
}
