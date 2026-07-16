package com.tftgogo.domain.content.scheduler;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentRefreshImportGuardTest {

    @Mock
    private ContentRefreshSchedulerLock schedulerLock;

    @Mock
    private Supplier<String> task;

    private ContentRefreshImportGuard importGuard;

    @BeforeEach
    void setUp() {
        importGuard = new ContentRefreshImportGuard(schedulerLock);
    }

    @Test
    void 락을_얻으면_task_결과를_반환한다() {
        // given
        givenSchedulerLockRunsTask();
        when(task.get()).thenReturn("imported");

        // when
        String result = importGuard.runWithLock("manual-patch-note", task);

        // then
        assertThat(result).isEqualTo("imported");
        verify(schedulerLock).runWithLock(eq("manual-patch-note"), any(Runnable.class));
        verify(task).get();
    }

    @Test
    void 락을_얻지_못하면_CONTENT_REFRESH_ALREADY_RUNNING_예외를_던진다() {
        // given
        when(schedulerLock.runWithLock(eq("manual-guide"), any(Runnable.class)))
                .thenReturn(false);

        // when, then
        assertThatThrownBy(() -> importGuard.runWithLock("manual-guide", task))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CONTENT_REFRESH_ALREADY_RUNNING));
        verifyNoInteractions(task);
    }

    @Test
    void task_예외는_호출자에게_그대로_전파한다() {
        // given
        givenSchedulerLockRunsTask();
        when(task.get()).thenThrow(new RuntimeException("import failed"));

        // when, then
        assertThatThrownBy(() -> importGuard.runWithLock("manual-patch-note", task))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("import failed");
    }

    private void givenSchedulerLockRunsTask() {
        doAnswer(invocation -> {
            Runnable lockedTask = invocation.getArgument(1);
            lockedTask.run();
            return true;
        }).when(schedulerLock).runWithLock(any(String.class), any(Runnable.class));
    }
}
