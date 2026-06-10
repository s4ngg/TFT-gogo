package com.tftgogo.domain.deck.scheduler;

import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.repository.MetaDeckRepository;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.global.config.MetaDeckProperties;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
    private final MetaDeckRepository metaDeckRepository;
    private final MetaDeckProperties metaDeckProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void aggregateOnStartupIfMissing() {
        if (!metaDeckProperties.isStartupAggregate()) {
            logger.info("서버 시작 자동 집계 비활성화 (app.meta-deck.startup-aggregate=false)");
            return;
        }
        LocalDate targetDate = LocalDate.now(SCHEDULE_ZONE).minusDays(1);
        aggregateIfMissing(targetDate, "서버 시작");
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void scheduledAggregate() {
        LocalDate targetDate = LocalDate.now(SCHEDULE_ZONE).minusDays(1);
        aggregateIfMissing(targetDate, "스케줄러");
    }

    private void aggregateIfMissing(LocalDate targetDate, String trigger) {
        long aggregatedRankCount = metaDeckRepository.countAggregatedRankFiltersByDataStartDate(targetDate);
        if (aggregatedRankCount >= RankFilter.values().length) {
            logger.info("메타 덱 일일 집계 스킵 - trigger={}, date={}, 이미 집계됨", trigger, targetDate);
            return;
        }

        logger.info("메타 덱 일일 자동 집계 시작 - trigger={}, date={}, aggregatedRankCount={}",
                trigger, targetDate, aggregatedRankCount);
        try {
            metaDeckService.aggregateAndSave(targetDate);
            logger.info("메타 덱 일일 자동 집계 완료 - trigger={}, date={}", trigger, targetDate);
        } catch (Exception e) {
            logger.error("메타 덱 일일 자동 집계 실패 - trigger={}, date={}", trigger, targetDate, e);
        }
    }
}
