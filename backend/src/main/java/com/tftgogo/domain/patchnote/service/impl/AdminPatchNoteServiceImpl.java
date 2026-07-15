package com.tftgogo.domain.patchnote.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.entity.GuideChampion;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.repository.GuideChampionRepository;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.domain.patchnote.config.PatchNoteCrawlerProperties;
import com.tftgogo.domain.patchnote.dto.crawl.PatchChangeCrawlRow;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlDocument;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlFetchedPage;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlListItem;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchChangeRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteImportRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteRequest;
import com.tftgogo.domain.patchnote.dto.response.AdminPatchNoteImportResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchChangeResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;
import com.tftgogo.domain.patchnote.entity.PatchChange;
import com.tftgogo.domain.patchnote.entity.PatchChangeCategory;
import com.tftgogo.domain.patchnote.entity.PatchChangeImpact;
import com.tftgogo.domain.patchnote.entity.PatchChangeTombstone;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.entity.PatchNoteImportSource;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository.PatchChangeCount;
import com.tftgogo.domain.patchnote.repository.PatchChangeTombstoneRepository;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.domain.patchnote.service.AdminPatchNoteService;
import com.tftgogo.domain.patchnote.service.PatchNoteCrawlerFetchService;
import com.tftgogo.domain.patchnote.service.PatchNoteCrawlerParser;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPatchNoteServiceImpl implements AdminPatchNoteService {

    private static final Logger logger = LogManager.getLogger(AdminPatchNoteServiceImpl.class);
    private static final Set<String> FATAL_PARSER_WARNINGS = Set.of("max detail rows reached");

    private final PatchNoteRepository patchNoteRepository;
    private final PatchChangeRepository patchChangeRepository;
    private final PatchChangeTombstoneRepository patchChangeTombstoneRepository;
    private final ObjectMapper objectMapper;
    private final PatchNoteCrawlerFetchService crawlerFetchService;
    private final PatchNoteCrawlerParser crawlerParser;
    private final PatchNoteCrawlerProperties crawlerProperties;
    private final GuideChampionRepository guideChampionRepository;
    private final GuideTraitRepository guideTraitRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PatchNoteResponse> getPatchNotes() {
        List<PatchNote> patchNotes = patchNoteRepository
                .findByDeletedAtIsNullOrderByCurrentDescPublishedAtDescIdDesc();
        Map<Long, Long> changeCounts = getChangeCounts(patchNotes);

        return patchNotes.stream()
                .map(patchNote -> toPatchNoteResponse(
                        patchNote,
                        changeCounts.getOrDefault(patchNote.getId(), 0L)
                ))
                .toList();
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PatchNoteResponse createPatchNote(AdminPatchNoteRequest request) {
        validateUniqueVersion(request.getVersion(), null);
        if (request.isCurrent()) {
            clearCurrentPatchNotes(null);
        }

        PatchNote patchNote = PatchNote.builder()
                .version(request.getVersion())
                .title(request.getTitle())
                .summary(request.getSummary())
                .description(resolveContent(request))
                .focus(request.getFocus())
                .imageUrl(request.getImageUrl())
                .publishedAt(request.getPublishedAt())
                .current(request.isCurrent())
                .highlightsJson(toJsonArray(request.getHighlights()))
                .build();

        PatchNote savedPatchNote = patchNoteRepository.save(patchNote);
        return toPatchNoteResponse(savedPatchNote, 0L);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AdminPatchNoteImportResponse importRiotPatchNote(AdminPatchNoteImportRequest request) {
        AdminPatchNoteImportRequest importRequest = request == null ? new AdminPatchNoteImportRequest() : request;
        String locale = normalizeLocale(importRequest.getLocale());
        String explicitVersion = normalizeText(importRequest.getVersion());
        PatchNoteCrawlFetchedPage detailPage = fetchImportDetailPage(importRequest, locale);
        PatchNoteCrawlDocument document = crawlerParser.parseDetailPage(detailPage, explicitVersion, locale);
        Set<String> incomingSourceKeys = validateImportDocument(document);
        String version = requireText(document.version(), ErrorCode.PATCH_NOTE_INVALID_DATA);
        String sourceUrl = requireText(document.sourceUrl(), ErrorCode.PATCH_NOTE_INVALID_DATA);
        String sourceKey = resolvePatchNoteSourceKey(document);

        Optional<PatchNote> existingPatchNote = findImportTarget(sourceKey, sourceUrl, version);
        List<PatchChange> existingPatchChanges = existingPatchNote
                .map(patchChangeRepository::findByPatchNoteOrderBySortOrderAscIdAsc)
                .orElseGet(List::of);
        validateRetainedRowRatio(version, incomingSourceKeys, existingPatchChanges);

        LocalDateTime importedAt = LocalDateTime.now();
        PatchNote patchNote;
        boolean patchNoteCreated = false;
        boolean patchNoteUpdated = false;
        boolean patchNoteSkipped = false;

        if (existingPatchNote.isPresent()) {
            patchNote = existingPatchNote.get();
            if (patchNote.isManuallyEdited()) {
                patchNoteSkipped = true;
                if (patchNote.getDeletedAt() == null && importRequest.shouldMarkCurrent()) {
                    clearCurrentPatchNotes(patchNote.getId());
                    patchNote.markCurrent();
                }
            } else {
                if (importRequest.shouldMarkCurrent()) {
                    clearCurrentPatchNotes(patchNote.getId());
                }
                patchNote.applyImportedData(
                        version,
                        resolvePatchNoteTitle(document, version),
                        normalizeText(document.summary()),
                        resolvePatchNoteContent(document),
                        resolvePatchNoteFocus(document),
                        normalizeText(document.imageUrl()),
                        sourceKey,
                        sourceUrl,
                        locale,
                        PatchNoteImportSource.RIOT_OFFICIAL,
                        importedAt,
                        resolvePublishedAt(document, importedAt),
                        importRequest.shouldMarkCurrent(),
                        toJsonArray(resolveHighlights(document))
                );
                patchNoteUpdated = true;
            }
        } else {
            if (importRequest.shouldMarkCurrent()) {
                clearCurrentPatchNotes(null);
            }
            patchNote = PatchNote.builder()
                    .version(version)
                    .title(resolvePatchNoteTitle(document, version))
                    .summary(normalizeText(document.summary()))
                    .description(resolvePatchNoteContent(document))
                    .focus(resolvePatchNoteFocus(document))
                    .imageUrl(normalizeText(document.imageUrl()))
                    .sourceKey(sourceKey)
                    .sourceUrl(sourceUrl)
                    .sourceLocale(locale)
                    .importSource(PatchNoteImportSource.RIOT_OFFICIAL)
                    .importedAt(importedAt)
                    .publishedAt(document.publishedAt() == null ? importedAt : document.publishedAt())
                    .current(importRequest.shouldMarkCurrent())
                    .highlightsJson(toJsonArray(resolveHighlights(document)))
                    .build();
            patchNote = patchNoteRepository.save(patchNote);
            patchNoteCreated = true;
        }

        ImportChangeStats changeStats = patchNote.getDeletedAt() != null
                ? ImportChangeStats.skipped(document.rows().size())
                : importChanges(
                        patchNote,
                        document.rows(),
                        importedAt,
                        existingPatchChanges,
                        !patchNoteCreated
                );

        return AdminPatchNoteImportResponse.of(
                patchNote.getId(),
                patchNote.getVersion(),
                sourceUrl,
                patchNoteCreated,
                patchNoteUpdated,
                patchNoteSkipped,
                changeStats.created(),
                changeStats.updated(),
                changeStats.skipped(),
                document.parserWarnings()
        );
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PatchNoteResponse updatePatchNote(Long patchNoteId, AdminPatchNoteRequest request) {
        PatchNote patchNote = findPatchNote(patchNoteId);
        validateUniqueVersion(request.getVersion(), patchNoteId);
        if (request.isCurrent()) {
            clearCurrentPatchNotes(patchNoteId);
        }

        patchNote.update(
                request.getVersion(),
                request.getTitle(),
                request.getSummary(),
                resolveContent(request),
                request.getFocus(),
                request.getImageUrl(),
                request.getPublishedAt(),
                request.isCurrent(),
                toJsonArray(request.getHighlights())
        );
        patchNote.markManuallyEditedIfImported();
        return toPatchNoteResponse(patchNote, countChanges(patchNote));
    }

    @Override
    @Transactional
    public void deletePatchNote(Long patchNoteId) {
        PatchNote patchNote = findPatchNote(patchNoteId);
        patchNote.markManuallyEditedIfImported();
        patchNote.softDelete();
        patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(patchNote)
                .forEach(patchChange -> {
                    patchChange.markManuallyEditedIfImported();
                    patchChangeRepository.delete(patchChange);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatchChangeResponse> getPatchChanges(Long patchNoteId) {
        PatchNote patchNote = findPatchNote(patchNoteId);
        return patchChangeRepository.findByPatchNoteOrderBySortOrderAscIdAsc(patchNote).stream()
                .map(this::toPatchChangeResponse)
                .toList();
    }

    @Override
    @Transactional
    public PatchChangeResponse createPatchChange(AdminPatchChangeRequest request) {
        PatchNote patchNote = findPatchNote(request.getPatchNoteId());
        PatchChange patchChange = PatchChange.builder()
                .patchNote(patchNote)
                .manuallyEdited(true)
                .category(PatchChangeCategory.from(request.getCategory()))
                .changeType(PatchChangeType.from(request.getType()))
                .impact(PatchChangeImpact.from(request.getImpact()))
                .targetKey(request.getTargetKey())
                .targetName(request.getTargetName())
                .summary(request.getSummary())
                .beforeValue(request.getBeforeValue())
                .afterValue(request.getAfterValue())
                .imageUrl(request.getImageUrl())
                .tagsJson(toJsonArray(request.getTags()))
                .sortOrder(request.getSortOrder())
                .build();

        return toPatchChangeResponse(patchChangeRepository.save(patchChange));
    }

    @Override
    @Transactional
    public PatchChangeResponse updatePatchChange(Long changeId, AdminPatchChangeRequest request) {
        PatchChange patchChange = findPatchChange(changeId);
        PatchNote patchNote = findPatchNote(request.getPatchNoteId());

        patchChange.update(
                patchNote,
                PatchChangeCategory.from(request.getCategory()),
                PatchChangeType.from(request.getType()),
                PatchChangeImpact.from(request.getImpact()),
                request.getTargetKey(),
                request.getTargetName(),
                request.getSummary(),
                request.getBeforeValue(),
                request.getAfterValue(),
                request.getImageUrl(),
                toJsonArray(request.getTags()),
                request.getSortOrder()
        );
        patchChange.markManuallyEditedIfImported();
        return toPatchChangeResponse(patchChange);
    }

    @Override
    @Transactional
    public void deletePatchChange(Long changeId) {
        deletePatchChangeByAdmin(findPatchChange(changeId));
    }

    private PatchNoteCrawlFetchedPage fetchImportDetailPage(AdminPatchNoteImportRequest request, String locale) {
        if (hasText(request.getSourceUrl())) {
            return crawlerFetchService.fetch(request.getSourceUrl());
        }

        PatchNoteCrawlFetchedPage listPage = crawlerFetchService.fetchTagPage(locale);
        List<PatchNoteCrawlListItem> listItems = crawlerParser.parseListPage(listPage);
        PatchNoteCrawlListItem latestItem = listItems.stream()
                .filter(item -> hasText(item.detailUrl()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA));
        return crawlerFetchService.fetch(latestItem.detailUrl());
    }

    private Optional<PatchNote> findImportTarget(String sourceKey, String sourceUrl, String version) {
        Optional<PatchNote> bySourceKey = patchNoteRepository.findBySourceKey(sourceKey);
        if (bySourceKey.isPresent()) {
            return bySourceKey;
        }

        Optional<PatchNote> bySourceUrl = patchNoteRepository.findBySourceUrl(sourceUrl);
        if (bySourceUrl.isPresent()) {
            return bySourceUrl;
        }

        return patchNoteRepository.findByVersion(version);
    }

    private Set<String> validateImportDocument(PatchNoteCrawlDocument document) {
        if (document == null || document.rows() == null || document.rows().isEmpty()) {
            logger.warn("Patch note import rejected because parsed rows are empty");
            throw new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA);
        }

        List<String> parserWarnings = document.parserWarnings() == null
                ? List.of()
                : document.parserWarnings();
        Optional<String> fatalWarning = parserWarnings.stream()
                .filter(this::hasText)
                .map(warning -> warning.trim().toLowerCase(Locale.ROOT))
                .filter(FATAL_PARSER_WARNINGS::contains)
                .findFirst();
        if (fatalWarning.isPresent()) {
            logger.warn(
                    "Patch note import rejected because parser result is incomplete. warning={}, sourceUrl={}",
                    fatalWarning.get(),
                    document.sourceUrl()
            );
            throw new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA);
        }

        Set<String> sourceKeys = new HashSet<>();
        for (PatchChangeCrawlRow row : document.rows()) {
            if (row == null) {
                throw new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA);
            }
            requireText(row.rowText(), ErrorCode.PATCH_NOTE_INVALID_DATA);
            if (!sourceKeys.add(resolveChangeSourceKey(row))) {
                logger.warn("Patch note import rejected because a duplicate source key was parsed");
                throw new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA);
            }
        }
        return sourceKeys;
    }

    private void validateRetainedRowRatio(
            String version,
            Set<String> incomingSourceKeys,
            List<PatchChange> existingPatchChanges
    ) {
        Set<String> existingImportedSourceKeys = existingPatchChanges.stream()
                .filter(patchChange -> patchChange.getImportedAt() != null)
                .filter(patchChange -> !patchChange.isManuallyEdited())
                .map(PatchChange::getSourceKey)
                .filter(this::hasText)
                .collect(Collectors.toSet());
        if (existingImportedSourceKeys.isEmpty()) {
            return;
        }

        long retainedSourceKeyCount = existingImportedSourceKeys.stream()
                .filter(incomingSourceKeys::contains)
                .count();
        double retainedRowRatio = retainedSourceKeyCount / (double) existingImportedSourceKeys.size();
        double minimumRetainedRowRatio = crawlerProperties.getMinRetainedRowRatio();
        if (retainedRowRatio >= minimumRetainedRowRatio) {
            return;
        }

        logger.warn(
                "Patch note import rejected because imported source key retention dropped abnormally. "
                        + "version={}, existingRows={}, incomingRows={}, retainedRows={}, "
                        + "retainedRatio={}, minimumRatio={}",
                version,
                existingImportedSourceKeys.size(),
                incomingSourceKeys.size(),
                retainedSourceKeyCount,
                retainedRowRatio,
                minimumRetainedRowRatio
        );
        throw new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA);
    }

    private ImportChangeStats importChanges(
            PatchNote patchNote,
            List<PatchChangeCrawlRow> rows,
            LocalDateTime importedAt,
            List<PatchChange> existingPatchChanges,
            boolean deleteStaleChanges
    ) {
        int created = 0;
        int updated = 0;
        int skipped = 0;
        Set<String> importedSourceKeys = new HashSet<>();
        Set<String> tombstonedSourceKeys = patchChangeTombstoneRepository.findSourceKeysByPatchNote(patchNote);
        PatchChangeGuideNameCatalog guideNameCatalog = buildGuideNameCatalog(patchNote.getVersion());

        for (PatchChangeCrawlRow row : rows) {
            String sourceKey = resolveChangeSourceKey(row);
            importedSourceKeys.add(sourceKey);
            if (tombstonedSourceKeys.contains(sourceKey)) {
                skipped++;
                continue;
            }
            PatchChangeCategory category = inferCategory(row, guideNameCatalog);
            PatchChangeType changeType = inferChangeType(row);
            PatchChangeImpact impact = PatchChangeImpact.MEDIUM;
            String targetName = resolveTargetName(row);
            String summary = requireText(row.rowText(), ErrorCode.PATCH_NOTE_INVALID_DATA);

            Optional<PatchChange> existingChange = patchChangeRepository.findByPatchNoteAndSourceKey(patchNote, sourceKey);
            if (existingChange.isPresent()) {
                PatchChange patchChange = existingChange.get();
                if (patchChange.isManuallyEdited()) {
                    skipped++;
                    continue;
                }
                patchChange.applyImportedData(
                        patchNote,
                        sourceKey,
                        normalizeText(row.headingPath()),
                        row.sourceOrder(),
                        importedAt,
                        category,
                        changeType,
                        impact,
                        null,
                        targetName,
                        summary,
                        normalizeText(row.beforeText()),
                        normalizeText(row.afterText()),
                        null,
                        null,
                        row.sourceOrder()
                );
                updated++;
                continue;
            }

            PatchChange patchChange = PatchChange.builder()
                    .patchNote(patchNote)
                    .sourceKey(sourceKey)
                    .sourceHeadingPath(normalizeText(row.headingPath()))
                    .sourceOrder(row.sourceOrder())
                    .importedAt(importedAt)
                    .category(category)
                    .changeType(changeType)
                    .impact(impact)
                    .targetKey(null)
                    .targetName(targetName)
                    .summary(summary)
                    .beforeValue(normalizeText(row.beforeText()))
                    .afterValue(normalizeText(row.afterText()))
                    .imageUrl(null)
                    .tagsJson(null)
                    .sortOrder(row.sourceOrder())
                    .build();
            patchChangeRepository.save(patchChange);
            created++;
        }

        if (deleteStaleChanges) {
            deleteStaleImportedChanges(existingPatchChanges, importedSourceKeys);
        }

        return new ImportChangeStats(created, updated, skipped);
    }

    private void deleteStaleImportedChanges(
            List<PatchChange> existingPatchChanges,
            Set<String> importedSourceKeys
    ) {
        List<PatchChange> staleChanges = existingPatchChanges.stream()
                .filter(patchChange -> patchChange.getImportedAt() != null)
                .filter(patchChange -> !patchChange.isManuallyEdited())
                .filter(patchChange -> !hasText(patchChange.getSourceKey())
                        || !importedSourceKeys.contains(patchChange.getSourceKey()))
                .toList();

        if (!staleChanges.isEmpty()) {
            patchChangeRepository.deleteAllInBatch(staleChanges);
        }
    }

    private PatchNote findPatchNote(Long patchNoteId) {
        return patchNoteRepository.findByIdAndDeletedAtIsNull(patchNoteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATCH_NOTE_NOT_FOUND));
    }

    private PatchChange findPatchChange(Long changeId) {
        return patchChangeRepository.findById(changeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATCH_CHANGE_NOT_FOUND));
    }

    private String resolvePatchNoteSourceKey(PatchNoteCrawlDocument document) {
        if (hasText(document.contentId())) {
            return truncate(document.contentId().trim(), 150);
        }
        return sha256(requireText(document.sourceUrl(), ErrorCode.PATCH_NOTE_INVALID_DATA));
    }

    private String resolveChangeSourceKey(PatchChangeCrawlRow row) {
        if (hasText(row.sourceKeyHash())) {
            return truncate(row.sourceKeyHash().trim(), 150);
        }
        return sha256(requireText(row.sourceKeyCandidate(), ErrorCode.PATCH_NOTE_INVALID_DATA));
    }

    private String resolvePatchNoteTitle(PatchNoteCrawlDocument document, String version) {
        if (hasText(document.title())) {
            return truncate(document.title().trim(), 200);
        }
        return truncate(version + " Patch Notes", 200);
    }

    private String resolvePatchNoteContent(PatchNoteCrawlDocument document) {
        if (hasText(document.summary())) {
            return document.summary().trim();
        }
        if (hasText(document.title())) {
            return document.title().trim();
        }
        return requireText(document.version(), ErrorCode.PATCH_NOTE_INVALID_DATA) + " Patch Notes";
    }

    private String resolvePatchNoteFocus(PatchNoteCrawlDocument document) {
        if (hasText(document.summary())) {
            return truncate(document.summary().trim(), 200);
        }
        if (hasText(document.title())) {
            return truncate(document.title().trim(), 200);
        }
        return null;
    }

    private List<String> resolveHighlights(PatchNoteCrawlDocument document) {
        List<String> highlights = document.sections().stream()
                .filter(this::hasText)
                .map(this::toDisplaySectionLabel)
                .filter(this::hasText)
                .distinct()
                .limit(5)
                .toList();
        if (!highlights.isEmpty()) {
            return highlights;
        }
        if (!hasText(document.summary())) {
            return List.of();
        }

        String summary = removeLeadingCountPrefix(document.summary());
        return hasText(summary) ? List.of(summary) : List.of();
    }

    private String toDisplaySectionLabel(String value) {
        String[] segments = value.trim().split(">");
        String label = segments.length == 0 ? value.trim() : segments[segments.length - 1].trim();
        String normalizedLabel = label
                .replaceAll("^\\d{1,2}월\\s+\\d{1,2}일\\s*", "")
                .replaceAll("^\\d{1,2}(?:[.-]\\d{1,2}[a-zA-Z]?)?\\s*(?:추가\\s*)?패치(?:\\s*노트)?\\s*", "")
                .trim();
        return hasText(normalizedLabel) ? removeLeadingCountPrefix(normalizedLabel) : removeLeadingCountPrefix(label);
    }

    private String removeLeadingCountPrefix(String value) {
        return value.replaceFirst("^\\s*\\(\\s*\\d+\\s*\\)\\s*", "").trim();
    }

    private LocalDateTime resolvePublishedAt(PatchNoteCrawlDocument document, LocalDateTime fallback) {
        return document.publishedAt() == null ? fallback : document.publishedAt();
    }

    private PatchChangeCategory inferCategory(PatchChangeCrawlRow row, PatchChangeGuideNameCatalog guideNameCatalog) {
        String text = normalizeLower(row.headingPath() + " " + row.sectionTitle() + " " + row.groupTitle());
        if (isBugFixRow(row)) {
            return PatchChangeCategory.SYSTEM;
        }
        if (text.contains("champion") || text.contains("챔피언")) {
            return PatchChangeCategory.CHAMPION;
        }
        if (text.contains("trait") || text.contains("시너지") || text.contains("특성")) {
            return PatchChangeCategory.TRAIT;
        }
        if (text.contains("item") || text.contains("아이템")) {
            return PatchChangeCategory.ITEM;
        }
        if (text.contains("augment") || text.contains("증강")) {
            return PatchChangeCategory.AUGMENT;
        }
        Optional<PatchChangeCategory> guideCategory = inferCategoryFromGuideNames(row, guideNameCatalog);
        if (guideCategory.isPresent()) {
            return guideCategory.get();
        }
        return PatchChangeCategory.SYSTEM;
    }

    private PatchChangeGuideNameCatalog buildGuideNameCatalog(String patchVersion) {
        if (!hasText(patchVersion)) {
            return PatchChangeGuideNameCatalog.empty();
        }

        return new PatchChangeGuideNameCatalog(
                collectGuideChampionNames(patchVersion),
                collectGuideTraitNames(patchVersion)
        );
    }

    private Set<String> collectGuideChampionNames(String patchVersion) {
        List<GuideChampion> champions = guideChampionRepository.findByPatchVersionOrderByNameAscIdAsc(patchVersion);
        if (champions == null || champions.isEmpty()) {
            return Set.of();
        }
        Set<String> names = new HashSet<>();
        champions.forEach(champion -> addGuideName(names, champion.getName()));
        return names;
    }

    private Set<String> collectGuideTraitNames(String patchVersion) {
        List<GuideTrait> traits = guideTraitRepository.findByPatchVersionOrderByNameAscIdAsc(patchVersion);
        if (traits == null || traits.isEmpty()) {
            return Set.of();
        }
        Set<String> names = new HashSet<>();
        traits.forEach(trait -> addGuideName(names, trait.getName()));
        return names;
    }

    private void addGuideName(Set<String> names, String name) {
        String normalizedName = normalizeGuideName(name);
        if (!hasText(normalizedName)) {
            return;
        }
        names.add(normalizedName);

        String compactName = normalizedName.replace(" ", "");
        if (compactName.length() >= 3) {
            names.add(compactName);
        }
    }

    private Optional<PatchChangeCategory> inferCategoryFromGuideNames(
            PatchChangeCrawlRow row,
            PatchChangeGuideNameCatalog guideNameCatalog
    ) {
        if (guideNameCatalog.isEmpty()) {
            return Optional.empty();
        }

        Optional<PatchChangeCategory> targetCategory = inferCategoryFromGuideText(
                row.groupTitle() + " " + row.sectionTitle(),
                guideNameCatalog
        );
        if (targetCategory.isPresent()) {
            return targetCategory;
        }

        Optional<PatchChangeCategory> rowTextCategory = inferCategoryFromGuideText(row.rowText(), guideNameCatalog);
        if (rowTextCategory.isPresent()) {
            return rowTextCategory;
        }

        return inferCategoryFromGuideText(row.headingPath(), guideNameCatalog);
    }

    private Optional<PatchChangeCategory> inferCategoryFromGuideText(
            String text,
            PatchChangeGuideNameCatalog guideNameCatalog
    ) {
        String normalizedText = normalizeGuideName(text);
        if (!hasText(normalizedText)) {
            return Optional.empty();
        }

        int championMatchLength = longestGuideNameMatchLength(normalizedText, guideNameCatalog.championNames());
        int traitMatchLength = longestGuideNameMatchLength(normalizedText, guideNameCatalog.traitNames());

        if (championMatchLength <= 0 && traitMatchLength <= 0) {
            return Optional.empty();
        }
        if (championMatchLength >= traitMatchLength) {
            return Optional.of(PatchChangeCategory.CHAMPION);
        }
        return Optional.of(PatchChangeCategory.TRAIT);
    }

    private int longestGuideNameMatchLength(String normalizedText, Set<String> guideNames) {
        int longestMatchLength = 0;
        for (String guideName : guideNames) {
            if (isGuideNameMatch(normalizedText, guideName)) {
                longestMatchLength = Math.max(longestMatchLength, guideName.length());
            }
        }
        return longestMatchLength;
    }

    private boolean isGuideNameMatch(String normalizedText, String guideName) {
        if (!hasText(normalizedText) || !hasText(guideName)) {
            return false;
        }
        if (normalizedText.equals(guideName)) {
            return true;
        }
        if (normalizedText.startsWith(guideName) && isGuideNameBoundary(normalizedText, guideName.length())) {
            return true;
        }
        if (guideName.length() < 4) {
            return false;
        }

        int matchIndex = normalizedText.indexOf(guideName);
        while (matchIndex >= 0) {
            int matchEndIndex = matchIndex + guideName.length();
            if (isGuideNameBoundary(normalizedText, matchIndex - 1)
                    && isGuideNameBoundary(normalizedText, matchEndIndex)) {
                return true;
            }
            matchIndex = normalizedText.indexOf(guideName, matchIndex + 1);
        }
        return false;
    }

    private boolean isGuideNameBoundary(String value, int index) {
        if (index < 0) {
            return true;
        }
        if (index >= value.length()) {
            return true;
        }
        char boundaryCandidate = value.charAt(index);
        return Character.isWhitespace(boundaryCandidate)
                || ":：,，.;·•/\\-–—()[]{}<>".indexOf(boundaryCandidate) >= 0;
    }

    private String normalizeGuideName(String value) {
        if (value == null) {
            return "";
        }
        return normalizeLower(value)
                .replaceAll("[\\s\\u00A0]+", " ")
                .replaceAll("^\\(\\s*\\d+\\s*\\)\\s*", "")
                .trim();
    }

    private PatchChangeType inferChangeType(PatchChangeCrawlRow row) {
        String text = normalizeLower(row.headingPath() + " " + row.rowText());
        if (isBugFixRow(row)) {
            return PatchChangeType.ADJUST;
        }
        if (text.contains("new") || text.contains("신규")) {
            return PatchChangeType.NEW;
        }
        return PatchChangeType.ADJUST;
    }

    private boolean isBugFixRow(PatchChangeCrawlRow row) {
        String text = normalizeLower(row.headingPath() + " "
                + row.sectionTitle() + " "
                + row.groupTitle() + " "
                + row.rowText());
        return text.contains("bug") || text.contains("버그");
    }

    private String resolveTargetName(PatchChangeCrawlRow row) {
        if (hasText(row.groupTitle())) {
            return truncate(row.groupTitle().trim(), 100);
        }
        if (hasText(row.sectionTitle())) {
            return truncate(row.sectionTitle().trim(), 100);
        }
        return truncate(requireText(row.rowText(), ErrorCode.PATCH_NOTE_INVALID_DATA), 100);
    }

    private void deletePatchChangeByAdmin(PatchChange patchChange) {
        recordPatchChangeTombstone(patchChange);
        patchChange.markManuallyEditedIfImported();
        patchChangeRepository.delete(patchChange);
    }

    private void recordPatchChangeTombstone(PatchChange patchChange) {
        if (!patchChange.isImported() || !hasText(patchChange.getSourceKey())) {
            return;
        }
        if (patchChangeTombstoneRepository.existsByPatchNoteAndSourceKey(
                patchChange.getPatchNote(),
                patchChange.getSourceKey()
        )) {
            return;
        }

        patchChangeTombstoneRepository.save(PatchChangeTombstone.builder()
                .patchNote(patchChange.getPatchNote())
                .sourceKey(patchChange.getSourceKey())
                .build());
    }

    private void validateUniqueVersion(String version, Long excludedPatchNoteId) {
        patchNoteRepository.findByVersion(version)
                .filter(patchNote -> !patchNote.getId().equals(excludedPatchNoteId))
                .ifPresent(patchNote -> {
                    throw new BusinessException(ErrorCode.INVALID_INPUT);
                });
    }

    private void clearCurrentPatchNotes(Long excludedPatchNoteId) {
        List<PatchNote> currentPatchNotes = excludedPatchNoteId == null
                ? patchNoteRepository.findByCurrentTrueAndDeletedAtIsNull()
                : patchNoteRepository.findByCurrentTrueAndDeletedAtIsNullAndIdNot(excludedPatchNoteId);
        if (currentPatchNotes.isEmpty()) {
            return;
        }
        currentPatchNotes.forEach(PatchNote::markNotCurrent);
        patchNoteRepository.flush();
    }

    private Map<Long, Long> getChangeCounts(List<PatchNote> patchNotes) {
        if (patchNotes.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> changeCounts = new LinkedHashMap<>();
        for (PatchChangeCount count : patchChangeRepository.countByPatchNotes(patchNotes)) {
            changeCounts.put(count.getPatchNoteId(), count.getChangeCount());
        }
        return changeCounts;
    }

    private long countChanges(PatchNote patchNote) {
        return patchChangeRepository.countByPatchNote(patchNote);
    }

    private PatchNoteResponse toPatchNoteResponse(PatchNote patchNote, long changeCount) {
        return PatchNoteResponse.from(
                patchNote,
                parseStringArray(patchNote.getHighlightsJson(), "highlightsJson", patchNote.getId()),
                changeCount
        );
    }

    private PatchChangeResponse toPatchChangeResponse(PatchChange patchChange) {
        return PatchChangeResponse.from(
                patchChange,
                parseStringArray(patchChange.getTagsJson(), "tagsJson", patchChange.getId())
        );
    }

    private String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        for (String value : values) {
            if (!hasText(value)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
        }

        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize patch note admin JSON array.", e);
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private String resolveContent(AdminPatchNoteRequest request) {
        return hasText(request.getDescription()) ? request.getDescription().trim() : request.getSummary();
    }

    private String normalizeLocale(String locale) {
        String resolvedLocale = hasText(locale) ? locale.trim() : crawlerProperties.getDefaultLocale();
        return resolvedLocale.toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String normalizeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, ErrorCode errorCode) {
        if (!hasText(value)) {
            throw new BusinessException(errorCode);
        }
        return value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }

    private List<String> parseStringArray(String json, String fieldName, Long ownerId) {
        if (!hasText(json)) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isArray()) {
                throw new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA);
            }

            List<String> values = new ArrayList<>();
            root.forEach(node -> {
                if (node.isTextual() && hasText(node.asText())) {
                    values.add(node.asText());
                }
            });
            return values;
        } catch (JsonProcessingException e) {
            logger.error(
                    "Invalid patch note admin JSON. field={}, ownerId={}, error={}",
                    fieldName,
                    ownerId,
                    e.getMessage(),
                    e
            );
            throw new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record ImportChangeStats(int created, int updated, int skipped) {
        private static ImportChangeStats skipped(int skipped) {
            return new ImportChangeStats(0, 0, skipped);
        }
    }

    private record PatchChangeGuideNameCatalog(Set<String> championNames, Set<String> traitNames) {
        private static PatchChangeGuideNameCatalog empty() {
            return new PatchChangeGuideNameCatalog(Set.of(), Set.of());
        }

        private boolean isEmpty() {
            return championNames.isEmpty() && traitNames.isEmpty();
        }
    }
}
