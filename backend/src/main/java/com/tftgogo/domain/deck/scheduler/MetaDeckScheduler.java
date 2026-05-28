package com.tftgogo.domain.deck.scheduler;

import com.tftgogo.domain.deck.service.MetaDeckService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MetaDeckScheduler {

    private static final Logger logger = LogManager.getLogger(MetaDeckScheduler.class);

    private final MetaDeckService metaDeckService;

    // 매일 새벽 4시 실행 (트래픽 최소 시간대)
    @Scheduled(cron = "0 0 4 * * *")
    public void scheduledAggregate() {
        logger.info("메타 덱 스케줄 집계 시작");
        try {
            metaDeckService.aggregateAndSave();
        } catch (Exception e) {
            logger.error("메타 덱 스케줄 집계 실패", e);
        }
    }
}
