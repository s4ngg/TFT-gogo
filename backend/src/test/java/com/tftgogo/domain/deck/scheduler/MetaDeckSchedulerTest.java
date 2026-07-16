package com.tftgogo.domain.deck.scheduler;

import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.repository.MetaDeckRepository;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.global.config.MetaDeckProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaDeckSchedulerTest {

    @Mock
    private MetaDeckService metaDeckService;

    @Mock
    private MetaDeckRepository metaDeckRepository;

    @Mock
    private MetaDeckSchedulerLock schedulerLock;

    @Mock
    private Executor aggregationExecutor;

    private MetaDeckProperties properties;
    private MetaDeckScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new MetaDeckProperties();
        scheduler = new MetaDeckScheduler(
                metaDeckService,
                metaDeckRepository,
                properties,
                schedulerLock,
                aggregationExecutor
        );
    }

    @Test
    void startup_aggregate가_false이면_아무것도_실행하지_않는다() {
        // given
        properties.setStartupAggregate(false);

        // when
        scheduler.aggregateOnStartupIfMissing();

        // then
        verifyNoInteractions(aggregationExecutor, schedulerLock, metaDeckRepository, metaDeckService);
    }

    @Test
    void startup_aggregate가_true이고_미집계_상태이면_bounded_executor를_통해_집계한다() {
        // given
        properties.setStartupAggregate(true);
        givenExecutorRunsInline();
        givenSchedulerLockRunsTask();
        when(metaDeckRepository.countAggregatedRankFiltersByDataStartDate(any(LocalDate.class))).thenReturn(0L);

        // when
        scheduler.aggregateOnStartupIfMissing();

        // then
        verify(aggregationExecutor).execute(any(Runnable.class));
        verify(schedulerLock).runWithLock(any(), any());
        verify(metaDeckService).aggregateAndSave(any(LocalDate.class));
    }

    @Test
    void 이미_전체_랭크가_집계된_날짜는_락을_잡아도_재집계하지_않는다() {
        // given
        properties.setStartupAggregate(true);
        givenExecutorRunsInline();
        givenSchedulerLockRunsTask();
        when(metaDeckRepository.countAggregatedRankFiltersByDataStartDate(any(LocalDate.class)))
                .thenReturn((long) RankFilter.values().length);

        // when
        scheduler.aggregateOnStartupIfMissing();

        // then
        verify(metaDeckService, never()).aggregateAndSave(any(LocalDate.class));
    }

    @Test
    void DB_락을_얻지_못하면_집계하지_않는다() {
        // given
        properties.setStartupAggregate(true);
        givenExecutorRunsInline();
        when(schedulerLock.runWithLock(any(), any())).thenReturn(false);

        // when
        scheduler.aggregateOnStartupIfMissing();

        // then
        verifyNoInteractions(metaDeckRepository, metaDeckService);
    }

    @Test
    void executor_큐가_가득차면_집계를_skip하고_예외를_전파하지_않는다() {
        // given
        properties.setStartupAggregate(true);
        doThrow(new RejectedExecutionException("queue full")).when(aggregationExecutor).execute(any(Runnable.class));

        // when, then
        assertThatCode(() -> scheduler.aggregateOnStartupIfMissing()).doesNotThrowAnyException();
        verifyNoInteractions(schedulerLock, metaDeckRepository, metaDeckService);
    }

    @Test
    void 정시_스케줄도_동일하게_bounded_executor와_DB_락을_거쳐_집계한다() {
        // given
        givenExecutorRunsInline();
        givenSchedulerLockRunsTask();
        when(metaDeckRepository.countAggregatedRankFiltersByDataStartDate(any(LocalDate.class))).thenReturn(0L);

        // when
        scheduler.scheduledAggregate();

        // then
        verify(aggregationExecutor).execute(any(Runnable.class));
        verify(schedulerLock).runWithLock(any(), any());
        verify(metaDeckService).aggregateAndSave(any(LocalDate.class));
    }

    @Test
    void 집계_중_예외가_발생해도_스케줄러_실행을_막지_않는다() {
        // given
        properties.setStartupAggregate(true);
        givenExecutorRunsInline();
        givenSchedulerLockRunsTask();
        when(metaDeckRepository.countAggregatedRankFiltersByDataStartDate(any(LocalDate.class))).thenReturn(0L);
        doThrow(new RuntimeException("riot api unavailable")).when(metaDeckService).aggregateAndSave(any(LocalDate.class));

        // when, then
        assertThatCode(() -> scheduler.aggregateOnStartupIfMissing()).doesNotThrowAnyException();
    }

    private void givenExecutorRunsInline() {
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(aggregationExecutor).execute(any(Runnable.class));
    }

    private void givenSchedulerLockRunsTask() {
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return true;
        }).when(schedulerLock).runWithLock(any(), any());
    }
}
