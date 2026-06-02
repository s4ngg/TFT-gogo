package com.tftgogo.domain.deck.service.impl;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class AsyncAggregationRunner {

    @Async("aggregationExecutor")
    public CompletableFuture<Void> run(Runnable task) {
        task.run();
        return CompletableFuture.completedFuture(null);
    }
}
