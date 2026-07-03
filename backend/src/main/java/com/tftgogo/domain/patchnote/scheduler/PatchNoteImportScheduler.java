package com.tftgogo.domain.patchnote.scheduler;

import com.tftgogo.domain.patchnote.config.PatchNoteImportSchedulerProperties;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlFetchedPage;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlListItem;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteImportRequest;
import com.tftgogo.domain.patchnote.dto.response.AdminPatchNoteImportResponse;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.domain.patchnote.service.AdminPatchNoteService;
import com.tftgogo.domain.patchnote.service.PatchNoteCrawlerFetchService;
import com.tftgogo.domain.patchnote.service.PatchNoteCrawlerParser;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PatchNoteImportScheduler {

    private static final Logger logger = LogManager.getLogger(PatchNoteImportScheduler.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d{1,2})[.-](\\d{1,2}[a-zA-Z]?)");

    private final AdminPatchNoteService adminPatchNoteService;
    private final PatchNoteCrawlerFetchService crawlerFetchService;
    private final PatchNoteCrawlerParser crawlerParser;
    private final PatchNoteRepository patchNoteRepository;
    private final PatchNoteImportSchedulerProperties properties;
    private final PatchNoteImportSchedulerLock schedulerLock;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void importOnStartupIfEnabled() {
        if (!properties.isEnabled()) {
            logger.info("Patch note scheduler disabled (app.patch-note.scheduler.enabled=false)");
            return;
        }
        if (!properties.isStartupImport()) {
            logger.info("Patch note startup import disabled (app.patch-note.scheduler.startup-import=false)");
            return;
        }
        runIfIdle("startup", this::importUnknownPatchNotesFromList);
    }

    @Scheduled(
            cron = "${app.patch-note.scheduler.list-cron:0 0 * * * *}",
            zone = "${app.patch-note.scheduler.zone:Asia/Seoul}"
    )
    public void importNewPatchNotesFromList() {
        runIfEnabledAndIdle("list-check", this::importUnknownPatchNotesFromList);
    }

    @Scheduled(
            cron = "${app.patch-note.scheduler.refresh-cron:0 30 6 * * *}",
            zone = "${app.patch-note.scheduler.zone:Asia/Seoul}"
    )
    public void refreshLatestPatchNote() {
        runIfEnabledAndIdle("daily-refresh", this::importUnknownPatchNotesFromList);
    }

    private void runIfEnabledAndIdle(String trigger, Runnable task) {
        if (!properties.isEnabled()) {
            return;
        }
        runIfIdle(trigger, task);
    }

    private void runIfIdle(String trigger, Runnable task) {
        if (!running.compareAndSet(false, true)) {
            logger.info("Patch note import skipped because another import is running. trigger={}", trigger);
            return;
        }

        try {
            schedulerLock.runWithLock(trigger, task);
        } catch (Exception e) {
            logger.error("Patch note scheduled import failed. trigger={}", trigger, e);
        } finally {
            running.set(false);
        }
    }

    private void importUnknownPatchNotesFromList() {
        String locale = normalizeLocale(properties.getLocale());
        PatchNoteCrawlFetchedPage listPage = crawlerFetchService.fetchTagPage(locale);
        List<PatchNoteCrawlListItem> listItems = crawlerParser.parseListPage(listPage);
        if (listItems.isEmpty()) {
            logger.info("Patch note list check skipped because Riot list is empty. locale={}", locale);
            return;
        }

        int scanLimit = Math.min(properties.getListScanLimit(), listItems.size());
        LocalDateTime historyCutoff = LocalDateTime.now().minusMonths(properties.getHistoryMonths());
        int imported = 0;
        for (int index = scanLimit - 1; index >= 0; index--) {
            PatchNoteCrawlListItem item = listItems.get(index);
            if (!hasText(item.detailUrl())) {
                continue;
            }
            if (!isWithinHistoryWindow(item, historyCutoff)) {
                continue;
            }
            if (isAlreadyImported(item)) {
                continue;
            }

            boolean markCurrent = properties.isCurrent() && index == 0;
            AdminPatchNoteImportRequest request = AdminPatchNoteImportRequest.of(
                    item.detailUrl(),
                    locale,
                    resolveVersion(item),
                    markCurrent
            );
            AdminPatchNoteImportResponse response = adminPatchNoteService.importRiotPatchNote(request);
            imported++;
            logger.info(
                    "Patch note imported from list. version={}, sourceUrl={}, current={}, created={}, updated={}, skipped={}",
                    response.getVersion(),
                    response.getSourceUrl(),
                    markCurrent,
                    response.isPatchNoteCreated(),
                    response.isPatchNoteUpdated(),
                    response.isPatchNoteSkipped()
            );
        }

        logger.info(
                "Patch note list check completed. scanned={}, historyMonths={}, historyCutoff={}, imported={}",
                scanLimit,
                properties.getHistoryMonths(),
                historyCutoff,
                imported
        );
    }

    private boolean isWithinHistoryWindow(PatchNoteCrawlListItem item, LocalDateTime historyCutoff) {
        return item.publishedAt() == null || !item.publishedAt().isBefore(historyCutoff);
    }

    private boolean isAlreadyImported(PatchNoteCrawlListItem item) {
        if (hasText(item.contentId()) && patchNoteRepository.findBySourceKey(item.contentId().trim()).isPresent()) {
            return true;
        }
        if (hasText(item.detailUrl()) && patchNoteRepository.findBySourceUrl(item.detailUrl().trim()).isPresent()) {
            return true;
        }
        String version = resolveVersion(item);
        return hasText(version) && patchNoteRepository.findByVersion(version).isPresent();
    }

    private String resolveVersion(PatchNoteCrawlListItem item) {
        String fromTitle = findVersion(item.title());
        if (hasText(fromTitle)) {
            return fromTitle;
        }
        return findVersion(item.detailUrl());
    }

    private String findVersion(String value) {
        if (!hasText(value)) {
            return null;
        }
        Matcher matcher = VERSION_PATTERN.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) + "." + matcher.group(2);
    }

    private String normalizeLocale(String locale) {
        return hasText(locale) ? locale.trim().toLowerCase(Locale.ROOT) : "ko-kr";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
