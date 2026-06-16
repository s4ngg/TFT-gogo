package com.tftgogo.domain.patchnote.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.patchnote.config.PatchNoteCrawlerProperties;
import com.tftgogo.domain.patchnote.dto.crawl.PatchChangeCrawlRow;
import com.tftgogo.domain.patchnote.dto.crawl.PatchChangeImportCandidate;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlDocument;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlFetchedPage;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlListItem;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteImportCandidate;
import com.tftgogo.domain.patchnote.dto.request.PatchNoteCrawlImportRequest;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteCrawlImportResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteCrawlRowErrorResponse;
import com.tftgogo.domain.patchnote.entity.PatchChange;
import com.tftgogo.domain.patchnote.entity.PatchChangeCategory;
import com.tftgogo.domain.patchnote.entity.PatchChangeImpact;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.entity.PatchNoteImportSource;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.domain.patchnote.service.PatchNoteCrawlerFetchService;
import com.tftgogo.domain.patchnote.service.PatchNoteCrawlerImportService;
import com.tftgogo.domain.patchnote.service.PatchNoteCrawlerParser;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PatchNoteCrawlerImportServiceImpl implements PatchNoteCrawlerImportService {

    private static final Logger logger = LogManager.getLogger(PatchNoteCrawlerImportServiceImpl.class);
    private static final int PATCH_VERSION_MAX_LENGTH = 20;
    private static final int TITLE_MAX_LENGTH = 150;
    private static final int SUMMARY_MAX_LENGTH = 500;
    private static final int TARGET_KEY_MAX_LENGTH = 100;
    private static final int TARGET_NAME_MAX_LENGTH = 100;
    private static final int BEFORE_AFTER_MAX_LENGTH = 300;

    private final PatchNoteCrawlerFetchService fetchService;
    private final PatchNoteCrawlerParser parser;
    private final PatchNoteRepository patchNoteRepository;
    private final PatchChangeRepository patchChangeRepository;
    private final PatchNoteCrawlerProperties properties;
    private final ObjectMapper objectMapper;
    private final TransactionOperations transactionOperations;

    @Override
    public PatchNoteCrawlImportResponse importPatchNote(PatchNoteCrawlImportRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String locale = request.resolveLocale(properties.getDefaultLocale());
        PatchNoteCrawlFetchedPage fetchedPage = fetchDetailPage(request, locale);
        PatchNoteCrawlDocument document = parser.parseDetailPage(
                fetchedPage,
                request.resolveVersion(),
                locale
        );

        ImportCandidates candidates = normalize(document, fetchedPage.fetchedAt());
        List<PatchNoteCrawlRowErrorResponse> rowErrors = validateCandidates(candidates);
        int reviewRequiredCount = (int) candidates.patchChanges().stream()
                .filter(PatchChangeImportCandidate::reviewRequired)
                .count();

        return transactionOperations.execute(status -> importCandidates(
                request,
                locale,
                document,
                candidates,
                rowErrors,
                reviewRequiredCount
        ));
    }

    private PatchNoteCrawlImportResponse importCandidates(
            PatchNoteCrawlImportRequest request,
            String locale,
            PatchNoteCrawlDocument document,
            ImportCandidates candidates,
            List<PatchNoteCrawlRowErrorResponse> rowErrors,
            int reviewRequiredCount
    ) {
        Optional<PatchNote> existingPatchNote = patchNoteRepository.findByVersion(candidates.patchNote().version());
        ImportCounter counter = new ImportCounter();
        counter.failedCount += rowErrors.size();
        counter.reviewRequiredCount += reviewRequiredCount;

        PatchNote patchNote = handlePatchNote(
                candidates.patchNote(),
                existingPatchNote,
                request.isDryRun(),
                request.isForceOverwrite(),
                counter
        );

        handlePatchChanges(
                patchNote,
                candidates.patchChanges(),
                request.isDryRun(),
                request.isForceOverwrite(),
                counter
        );

        return PatchNoteCrawlImportResponse.builder()
                .sourceUrl(candidates.patchNote().sourceUrl())
                .version(candidates.patchNote().version())
                .locale(locale)
                .dryRun(request.isDryRun())
                .patchNoteId(patchNote != null ? patchNote.getId() : existingPatchNote.map(PatchNote::getId).orElse(null))
                .createdCount(counter.createdCount)
                .updatedCount(counter.updatedCount)
                .skippedCount(counter.skippedCount)
                .reviewRequiredCount(counter.reviewRequiredCount)
                .failedCount(counter.failedCount)
                .parserWarnings(document.parserWarnings())
                .rowErrors(rowErrors)
                .build();
    }

    private PatchNoteCrawlFetchedPage fetchDetailPage(PatchNoteCrawlImportRequest request, String locale) {
        String sourceUrl = request.resolveSourceUrl();
        if (sourceUrl != null) {
            return fetchService.fetch(sourceUrl);
        }

        PatchNoteCrawlFetchedPage tagPage = fetchService.fetchTagPage(locale);
        List<PatchNoteCrawlListItem> items = parser.parseListPage(tagPage);
        return items.stream()
                .filter(item -> hasText(item.detailUrl()))
                .findFirst()
                .map(item -> fetchService.fetch(item.detailUrl()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA));
    }

    private ImportCandidates normalize(PatchNoteCrawlDocument document, LocalDateTime fetchedAt) {
        String version = trimToMax(document.version(), PATCH_VERSION_MAX_LENGTH);
        if (!hasText(version)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String title = trimToMax(defaultIfBlank(document.title(), "TFT patch " + version), TITLE_MAX_LENGTH);
        String summary = trimToMax(defaultIfBlank(document.summary(), title), SUMMARY_MAX_LENGTH);
        LocalDateTime importedAt = fetchedAt != null ? fetchedAt : LocalDateTime.now();
        PatchNoteImportCandidate patchNote = new PatchNoteImportCandidate(
                version,
                title,
                summary,
                summary,
                null,
                document.imageUrl(),
                document.publishedAt() != null ? document.publishedAt() : importedAt,
                false,
                List.of(),
                document.sourceUrl(),
                document.locale(),
                importedAt
        );

        List<PatchChangeImportCandidate> patchChanges = new ArrayList<>();
        for (PatchChangeCrawlRow row : document.rows()) {
            patchChanges.add(toPatchChangeCandidate(row, document));
        }
        return new ImportCandidates(patchNote, patchChanges);
    }

    private PatchChangeImportCandidate toPatchChangeCandidate(PatchChangeCrawlRow row, PatchNoteCrawlDocument document) {
        CategoryMatch categoryMatch = resolveCategory(row);
        TypeMatch typeMatch = resolveType(row);
        boolean reviewRequired = !categoryMatch.confident()
                || !typeMatch.confident()
                || !row.parserWarnings().isEmpty();
        List<String> tags = buildTags(document.locale(), reviewRequired);
        String targetName = trimToMax(resolveTargetName(row), TARGET_NAME_MAX_LENGTH);

        return new PatchChangeImportCandidate(
                row.sourceKeyHash(),
                document.sourceUrl(),
                row.headingPath(),
                row.sourceOrder(),
                document.locale(),
                LocalDateTime.now(),
                categoryMatch.category(),
                typeMatch.changeType(),
                PatchChangeImpact.MEDIUM,
                trimToMax(slug(categoryMatch.category().name() + "-" + targetName), TARGET_KEY_MAX_LENGTH),
                targetName,
                trimToMax(row.rowText(), SUMMARY_MAX_LENGTH),
                trimToMax(row.beforeText(), BEFORE_AFTER_MAX_LENGTH),
                trimToMax(row.afterText(), BEFORE_AFTER_MAX_LENGTH),
                null,
                tags,
                row.sourceOrder(),
                !reviewRequired,
                reviewRequired
        );
    }

    private PatchNote handlePatchNote(
            PatchNoteImportCandidate candidate,
            Optional<PatchNote> existingPatchNote,
            boolean dryRun,
            boolean forceOverwrite,
            ImportCounter counter) {
        if (existingPatchNote.isEmpty()) {
            counter.createdCount++;
            if (dryRun) {
                return null;
            }
            return patchNoteRepository.save(toPatchNote(candidate));
        }

        PatchNote patchNote = existingPatchNote.get();
        if (patchNote.isManuallyEdited() && !forceOverwrite) {
            counter.skippedCount++;
            return patchNote;
        }

        counter.updatedCount++;
        if (!dryRun) {
            patchNote.updateImported(
                    candidate.title(),
                    candidate.summary(),
                    candidate.description(),
                    candidate.focus(),
                    candidate.imageUrl(),
                    candidate.publishedAt(),
                    candidate.current(),
                    toJsonArray(candidate.highlights()),
                    candidate.sourceUrl(),
                    candidate.sourceLocale(),
                    PatchNoteImportSource.RIOT_OFFICIAL,
                    candidate.importedAt()
            );
        }
        return patchNote;
    }

    private void handlePatchChanges(
            PatchNote patchNote,
            List<PatchChangeImportCandidate> candidates,
            boolean dryRun,
            boolean forceOverwrite,
            ImportCounter counter) {
        Set<String> seenSourceKeys = new LinkedHashSet<>();
        for (PatchChangeImportCandidate candidate : candidates) {
            if (!hasText(candidate.sourceKey()) || !seenSourceKeys.add(candidate.sourceKey())) {
                continue;
            }

            Optional<PatchChange> existingChange = patchNote != null
                    ? patchChangeRepository.findByPatchNoteAndSourceKey(patchNote, candidate.sourceKey())
                    : Optional.empty();
            if (existingChange.isEmpty()) {
                counter.createdCount++;
                if (!dryRun && patchNote != null) {
                    patchChangeRepository.save(toPatchChange(patchNote, candidate));
                }
                continue;
            }

            PatchChange patchChange = existingChange.get();
            if (patchChange.isManuallyEdited() && !forceOverwrite) {
                counter.skippedCount++;
                continue;
            }

            counter.updatedCount++;
            if (!dryRun) {
                patchChange.updateImported(
                        patchNote,
                        candidate.sourceKey(),
                        candidate.sourceUrl(),
                        candidate.sourceHeadingPath(),
                        candidate.sourceOrder(),
                        candidate.sourceLocale(),
                        PatchNoteImportSource.RIOT_OFFICIAL,
                        candidate.importedAt(),
                        candidate.category(),
                        candidate.changeType(),
                        candidate.impact(),
                        candidate.targetKey(),
                        candidate.targetName(),
                        candidate.summary(),
                        candidate.beforeValue(),
                        candidate.afterValue(),
                        candidate.imageUrl(),
                        toJsonArray(candidate.tags()),
                        candidate.sortOrder(),
                        candidate.active()
                );
            }
        }
    }

    private List<PatchNoteCrawlRowErrorResponse> validateCandidates(ImportCandidates candidates) {
        List<PatchNoteCrawlRowErrorResponse> rowErrors = new ArrayList<>();
        Set<String> sourceKeys = new LinkedHashSet<>();
        for (PatchChangeImportCandidate candidate : candidates.patchChanges()) {
            if (!hasText(candidate.sourceKey())) {
                rowErrors.add(rowError(candidate, "sourceKey is missing"));
                continue;
            }
            if (!sourceKeys.add(candidate.sourceKey())) {
                rowErrors.add(rowError(candidate, "sourceKey is duplicated in parsed document"));
            }
        }
        return rowErrors;
    }

    private PatchNoteCrawlRowErrorResponse rowError(PatchChangeImportCandidate candidate, String reason) {
        return PatchNoteCrawlRowErrorResponse.builder()
                .sourceKey(candidate.sourceKey())
                .headingPath(candidate.sourceHeadingPath())
                .sourceOrder(candidate.sourceOrder())
                .rowTextPreview(trimToMax(candidate.summary(), 120))
                .reason(reason)
                .build();
    }

    private PatchNote toPatchNote(PatchNoteImportCandidate candidate) {
        return PatchNote.builder()
                .version(candidate.version())
                .title(candidate.title())
                .summary(candidate.summary())
                .description(candidate.description())
                .focus(candidate.focus())
                .imageUrl(candidate.imageUrl())
                .sourceUrl(candidate.sourceUrl())
                .sourceLocale(candidate.sourceLocale())
                .importSource(PatchNoteImportSource.RIOT_OFFICIAL)
                .importedAt(candidate.importedAt())
                .publishedAt(candidate.publishedAt())
                .current(candidate.current())
                .highlightsJson(toJsonArray(candidate.highlights()))
                .active(true)
                .build();
    }

    private PatchChange toPatchChange(PatchNote patchNote, PatchChangeImportCandidate candidate) {
        return PatchChange.builder()
                .patchNote(patchNote)
                .sourceKey(candidate.sourceKey())
                .sourceUrl(candidate.sourceUrl())
                .sourceHeadingPath(candidate.sourceHeadingPath())
                .sourceOrder(candidate.sourceOrder())
                .sourceLocale(candidate.sourceLocale())
                .importSource(PatchNoteImportSource.RIOT_OFFICIAL)
                .importedAt(candidate.importedAt())
                .category(candidate.category())
                .changeType(candidate.changeType())
                .impact(candidate.impact())
                .targetKey(candidate.targetKey())
                .targetName(candidate.targetName())
                .summary(candidate.summary())
                .beforeValue(candidate.beforeValue())
                .afterValue(candidate.afterValue())
                .imageUrl(candidate.imageUrl())
                .tagsJson(toJsonArray(candidate.tags()))
                .sortOrder(candidate.sortOrder())
                .active(candidate.active())
                .build();
    }

    private CategoryMatch resolveCategory(PatchChangeCrawlRow row) {
        String context = (defaultString(row.headingPath()) + " " + defaultString(row.rowText()))
                .toLowerCase(Locale.ROOT);
        if (containsAny(context, "챔피언", "유닛", "champion", "unit")) {
            return new CategoryMatch(PatchChangeCategory.CHAMPION, true);
        }
        if (containsAny(context, "특성", "계열", "직업", "시너지", "trait", "origin", "class")) {
            return new CategoryMatch(PatchChangeCategory.TRAIT, true);
        }
        if (containsAny(context, "아이템", "상징", "유물", "찬란한", "지원", "item", "emblem", "artifact", "radiant", "support")) {
            return new CategoryMatch(PatchChangeCategory.ITEM, true);
        }
        if (containsAny(context, "증강", "augment", "anomaly")) {
            return new CategoryMatch(PatchChangeCategory.AUGMENT, true);
        }
        if (containsAny(context, "시스템", "버그", "모드", "체계", "랭크", "system", "bug", "mode", "ranked", "loot")) {
            return new CategoryMatch(PatchChangeCategory.SYSTEM, true);
        }
        return new CategoryMatch(PatchChangeCategory.SYSTEM, false);
    }

    private TypeMatch resolveType(PatchChangeCrawlRow row) {
        String text = row.rowText().toLowerCase(Locale.ROOT);
        if (containsAny(text, "신규", "추가", "새로운", "new", "added")) {
            return new TypeMatch(PatchChangeType.NEW, true);
        }
        if (containsAny(text, "상향", "강화", "증가", "향상", "buff", "increased", "improved")) {
            return new TypeMatch(PatchChangeType.BUFF, true);
        }
        if (containsAny(text, "하향", "약화", "감소", "nerf", "reduced", "decreased")) {
            return new TypeMatch(PatchChangeType.NERF, true);
        }
        return new TypeMatch(PatchChangeType.ADJUST, false);
    }

    private String resolveTargetName(PatchChangeCrawlRow row) {
        String rowText = defaultString(row.rowText());
        int colonIndex = rowText.indexOf(':');
        if (colonIndex > 0) {
            return rowText.substring(0, colonIndex).trim();
        }
        if (hasText(row.groupTitle())) {
            return row.groupTitle().trim();
        }
        if (hasText(row.sectionTitle())) {
            return row.sectionTitle().trim();
        }
        return "unknown";
    }

    private List<String> buildTags(String locale, boolean reviewRequired) {
        List<String> tags = new ArrayList<>();
        tags.add("import:riot");
        tags.add("locale:" + locale);
        if (reviewRequired) {
            tags.add("review:required");
        }
        return tags;
    }

    private String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize patch note crawler JSON array.", e);
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(defaultString(value), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^0-9a-z가-힣]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return hasText(normalized) ? normalized : "unknown";
    }

    private String trimToMax(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private String defaultIfBlank(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record ImportCandidates(
            PatchNoteImportCandidate patchNote,
            List<PatchChangeImportCandidate> patchChanges
    ) {
    }

    private record CategoryMatch(PatchChangeCategory category, boolean confident) {
    }

    private record TypeMatch(PatchChangeType changeType, boolean confident) {
    }

    private static class ImportCounter {
        private int createdCount;
        private int updatedCount;
        private int skippedCount;
        private int reviewRequiredCount;
        private int failedCount;
    }
}
