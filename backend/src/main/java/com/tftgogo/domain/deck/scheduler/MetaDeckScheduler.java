package com.tftgogo.domain.deck.scheduler;

import com.tftgogo.domain.deck.service.MetaDeckService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class MetaDeckScheduler {

    private static final Logger logger = LogManager.getLogger(MetaDeckScheduler.class);
    private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Seoul");

    private final MetaDeckService metaDeckService;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void scheduledAggregate() {
        LocalDate targetDate = LocalDate.now(SCHEDULE_ZONE).minusDays(1);
        logger.info("메타 덱 일일 자동 집계 시작 - date={}", targetDate);
        try {
            metaDeckService.aggregateAndSave(targetDate);
        } catch (Exception e) {
            logger.error("메타 덱 일일 자동 집계 실패 - date={}", targetDate, e);
        }
    }
}
