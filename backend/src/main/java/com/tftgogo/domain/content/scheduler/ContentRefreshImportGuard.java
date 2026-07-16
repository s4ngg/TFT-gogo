package com.tftgogo.domain.content.scheduler;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class ContentRefreshImportGuard {

    private final ContentRefreshSchedulerLock schedulerLock;

    public <T> T runWithLock(String trigger, Supplier<T> task) {
        AtomicReference<T> result = new AtomicReference<>();
        boolean acquired = schedulerLock.runWithLock(trigger, () -> result.set(task.get()));
        if (!acquired) {
            throw new BusinessException(ErrorCode.CONTENT_REFRESH_ALREADY_RUNNING);
        }
        return result.get();
    }
}
