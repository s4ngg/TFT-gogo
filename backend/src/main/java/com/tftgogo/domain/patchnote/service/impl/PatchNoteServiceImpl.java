package com.tftgogo.domain.patchnote.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.patchnote.dto.response.PatchChangePageResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchChangeResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchChangeStatsResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;
import com.tftgogo.domain.patchnote.entity.PatchChangeCategory;
import com.tftgogo.domain.patchnote.entity.PatchChange;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;
import com.tftgogo.domain.patchnote.entity.PatchChangeImpact;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository.PatchChangeCount;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository.PatchChangeStatsCount;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.domain.patchnote.service.PatchNoteService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatchNoteServiceImpl implements PatchNoteService {

    private static final Logger logger = LogManager.getLogger(PatchNoteServiceImpl.class);
    private static final int DEFAULT_PAGE = 1;
    private static final int MAX_PAGE = 10_000;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 1000;
    private static final int PUBLIC_PATCH_HISTORY_MONTHS = 6;
    private static final String LIKE_ESCAPE = "\\";

    private final PatchNoteRepository patchNoteRepository;
    private final PatchChangeRepository patchChangeRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<PatchNoteResponse> getPatchNotes() {
        LocalDateTime historyCutoff = LocalDateTime.now().minusMonths(PUBLIC_PATCH_HISTORY_MONTHS);
        List<PatchNote> patchNotes = patchNoteRepository
                .findPublicHistorySinceIncludingCurrent(historyCutoff);
        Map<Long, Long> changeCounts = getChangeCounts(patchNotes);

        return patchNotes.stream()
                .map(patchNote -> toPatchNoteResponse(
                        patchNote,
                        changeCounts.getOrDefault(patchNote.getId(), 0L)
                ))
                .toList();
    }

    @Override
    public PatchChangePageResponse getPatchChanges(
            String version,
            String category,
            String type,
            String impact,
            String query,
            Integer page,
            Integer pageSize
    ) {
        PatchNote patchNote = patchNoteRepository.findByVersionAndDeletedAtIsNull(version)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATCH_NOTE_NOT_FOUND));
        PatchChangeCategory parsedCategory = parseCategory(category);
        PatchChangeType parsedType = parseType(type);
        PatchChangeImpact parsedImpact = parseImpact(impact);
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        String normalizedQuery = normalizeText(query);

        PatchChangeStatsResponse stats = buildStats(patchNote, parsedType, parsedImpact, normalizedQuery);

        Page<PatchChange> filteredChanges = patchChangeRepository.findFilteredChanges(
                patchNote,
                parsedCategory,
                parsedType,
                parsedImpact,
                normalizedQuery,
                PageRequest.of(normalizedPage - 1, normalizedPageSize)
        );

        long totalItems = filteredChanges.getTotalElements();
        int totalPages = Math.max(1, filteredChanges.getTotalPages());
        List<PatchChangeResponse> responses = filteredChanges.getContent().stream()
                .map(this::toPatchChangeResponse)
                .toList();

        return PatchChangePageResponse.of(
                responses,
                normalizedPage,
                normalizedPageSize,
                totalItems,
                totalPages,
                stats
        );
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

    private PatchChangeStatsResponse buildStats(
            PatchNote patchNote,
            PatchChangeType changeType,
            PatchChangeImpact impact,
            String query
    ) {
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        categoryCounts.put("ALL", 0L);
        for (PatchChangeCategory category : PatchChangeCategory.values()) {
            categoryCounts.put(category.name(), 0L);
        }

        Map<String, Long> typeCounts = new LinkedHashMap<>();
        for (PatchChangeType type : PatchChangeType.values()) {
            typeCounts.put(type.name(), 0L);
        }

        long totalChanges = 0L;
        long highImpactCount = 0L;
        for (PatchChangeStatsCount count : patchChangeRepository.countFilteredChangeStats(
                patchNote,
                changeType,
                impact,
                query
        )) {
            long changeCount = count.getChangeCount();
            String categoryName = count.getCategory().name();
            String typeName = count.getChangeType().name();

            categoryCounts.put(categoryName, categoryCounts.get(categoryName) + changeCount);
            typeCounts.put(typeName, typeCounts.get(typeName) + changeCount);
            totalChanges += changeCount;
            if (count.getImpact() == PatchChangeImpact.HIGH) {
                highImpactCount += changeCount;
            }
        }
        categoryCounts.put("ALL", totalChanges);

        return PatchChangeStatsResponse.of(
                totalChanges,
                categoryCounts,
                typeCounts,
                typeCounts.get(PatchChangeType.BUFF.name()),
                typeCounts.get(PatchChangeType.NERF.name()),
                highImpactCount
        );
    }

    private List<String> parseStringArray(String json, String fieldName, Long ownerId) {
        if (!hasText(json)) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isArray()) {
                logInvalidJson(fieldName, ownerId);
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
                    "Invalid patch note JSON. field={}, ownerId={}, error={}",
                    fieldName,
                    ownerId,
                    e.getMessage(),
                    e
            );
            throw new BusinessException(ErrorCode.PATCH_NOTE_INVALID_DATA);
        }
    }

    private void logInvalidJson(String fieldName, Long ownerId) {
        logger.error("Invalid patch note JSON. field={}, ownerId={}", fieldName, ownerId);
    }

    private PatchChangeCategory parseCategory(String category) {
        if (!hasText(category)) {
            return null;
        }
        return PatchChangeCategory.from(category);
    }

    private PatchChangeType parseType(String type) {
        if (!hasText(type)) {
            return null;
        }
        return PatchChangeType.from(type);
    }

    private PatchChangeImpact parseImpact(String impact) {
        if (!hasText(impact)) {
            return null;
        }
        return PatchChangeImpact.from(impact);
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 1 || page > MAX_PAGE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return pageSize;
    }

    private String normalizeText(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim()
                .replace(LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
