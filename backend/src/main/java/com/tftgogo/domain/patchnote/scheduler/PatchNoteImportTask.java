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
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PatchNoteImportTask {

    private static final Logger logger = LogManager.getLogger(PatchNoteImportTask.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d{1,2})[.-](\\d{1,2}[a-zA-Z]?)");

    private final AdminPatchNoteService adminPatchNoteService;
    private final PatchNoteCrawlerFetchService crawlerFetchService;
    private final PatchNoteCrawlerParser crawlerParser;
    private final PatchNoteRepository patchNoteRepository;
    private final PatchNoteImportSchedulerProperties properties;

    public PatchNoteRefreshResult importLatestPatchNoteThenUnknownPatchNotesFromList() {
        AdminPatchNoteImportResponse latestPatchNote = importLatestPatchNote();
        HistoryBackfillResult historyResult;
        try {
            historyResult = importUnknownPatchNotesFromList();
        } catch (Exception e) {
            logger.warn(
                    "Patch note history backfill failed after latest patch was committed. "
                            + "Guide import will continue with the committed latest version. version={}",
                    latestPatchNote.getVersion(),
                    e
            );
            historyResult = HistoryBackfillResult.failed();
        }
        return new PatchNoteRefreshResult(latestPatchNote, historyResult);
    }

    public AdminPatchNoteImportResponse importLatestPatchNote() {
        AdminPatchNoteImportRequest request = AdminPatchNoteImportRequest.of(
                null,
                normalizeLocale(properties.getLocale()),
                null,
                properties.isCurrent()
        );
        AdminPatchNoteImportResponse response = adminPatchNoteService.importRiotPatchNote(request);
        logger.info(
                "Latest patch note refreshed. version={}, sourceUrl={}, created={}, updated={}, skipped={}",
                response.getVersion(),
                response.getSourceUrl(),
                response.isPatchNoteCreated(),
                response.isPatchNoteUpdated(),
                response.isPatchNoteSkipped()
        );
        return response;
    }

    public HistoryBackfillResult importUnknownPatchNotesFromList() {
        String locale = normalizeLocale(properties.getLocale());
        PatchNoteCrawlFetchedPage listPage = crawlerFetchService.fetchTagPage(locale);
        List<PatchNoteCrawlListItem> listItems = crawlerParser.parseListPage(listPage);
        if (listItems.isEmpty()) {
            logger.info("Patch note list check skipped because Riot list is empty. locale={}", locale);
            return HistoryBackfillResult.empty();
        }

        int scanLimit = Math.min(properties.getListScanLimit(), listItems.size());
        LocalDateTime historyCutoff = LocalDateTime.now().minusMonths(properties.getHistoryMonths());
        int imported = 0;
        int failed = 0;
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
            String version = resolveVersion(item);
            AdminPatchNoteImportRequest request = AdminPatchNoteImportRequest.of(
                    item.detailUrl(),
                    locale,
                    version,
                    markCurrent
            );
            try {
                AdminPatchNoteImportResponse response = adminPatchNoteService.importRiotPatchNote(request);
                imported++;
                logger.info(
                        "Patch note imported from list. version={}, sourceUrl={}, current={}, "
                                + "created={}, updated={}, skipped={}",
                        response.getVersion(),
                        response.getSourceUrl(),
                        markCurrent,
                        response.isPatchNoteCreated(),
                        response.isPatchNoteUpdated(),
                        response.isPatchNoteSkipped()
                );
            } catch (Exception e) {
                failed++;
                logger.warn(
                        "Patch note import item failed. detailUrl={}, version={}, current={}",
                        item.detailUrl(),
                        version,
                        markCurrent,
                        e
                );
            }
        }

        logger.info(
                "Patch note list check completed. scanned={}, historyMonths={}, historyCutoff={}, imported={}, failed={}",
                scanLimit,
                properties.getHistoryMonths(),
                historyCutoff,
                imported,
                failed
        );
        return new HistoryBackfillResult(scanLimit, imported, failed);
    }

    private boolean isWithinHistoryWindow(PatchNoteCrawlListItem item, LocalDateTime historyCutoff) {
        return item.publishedAt() != null && !item.publishedAt().isBefore(historyCutoff);
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

    public record PatchNoteRefreshResult(
            AdminPatchNoteImportResponse latestPatchNote,
            HistoryBackfillResult historyBackfill
    ) {
        public boolean hasHistoryFailures() {
            return historyBackfill != null && historyBackfill.failedCount() > 0;
        }
    }

    public record HistoryBackfillResult(int scannedCount, int importedCount, int failedCount) {
        private static HistoryBackfillResult empty() {
            return new HistoryBackfillResult(0, 0, 0);
        }

        private static HistoryBackfillResult failed() {
            return new HistoryBackfillResult(0, 0, 1);
        }
    }
}
