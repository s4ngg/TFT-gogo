package com.tftgogo.domain.patchnote.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.patchnote.dto.response.PatchChangePageResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchChangeResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchChangeStatsResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;
import com.tftgogo.domain.patchnote.entity.PatchCategory;
import com.tftgogo.domain.patchnote.entity.PatchChange;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;
import com.tftgogo.domain.patchnote.entity.PatchImpact;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository.PatchChangeCount;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.domain.patchnote.service.PatchNoteService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final int MAX_PAGE_SIZE = 100;
    private static final String LIKE_ESCAPE = "\\";

    private final PatchNoteRepository patchNoteRepository;
    private final PatchChangeRepository patchChangeRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<PatchNoteResponse> getPatchNotes() {
        List<PatchNote> patchNotes = patchNoteRepository
                .findByActiveTrueAndDeletedAtIsNullOrderByCurrentDescPublishedAtDescIdDesc();
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
        PatchNote patchNote = patchNoteRepository.findByVersionAndActiveTrueAndDeletedAtIsNull(version)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATCH_NOTE_NOT_FOUND));
        PatchCategory parsedCategory = parseCategory(category);
        PatchChangeType parsedType = parseType(type);
        PatchImpact parsedImpact = parseImpact(impact);
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);

        List<PatchChange> allChanges = patchChangeRepository
                .findByPatchNoteAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc(patchNote);
        PatchChangeStatsResponse stats = buildStats(allChanges);

        List<PatchChange> filteredChanges = patchChangeRepository.findFilteredChanges(
                patchNote,
                parsedCategory,
                parsedType,
                parsedImpact,
                normalizeText(query)
        );

        long totalItems = filteredChanges.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / normalizedPageSize));
        long fromIndexLong = Math.min((long) (normalizedPage - 1) * normalizedPageSize, filteredChanges.size());
        int fromIndex = (int) fromIndexLong;
        int toIndex = (int) Math.min(fromIndexLong + normalizedPageSize, filteredChanges.size());

        List<PatchChangeResponse> responses = filteredChanges.subList(fromIndex, toIndex).stream()
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

    private PatchChangeStatsResponse buildStats(List<PatchChange> changes) {
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        categoryCounts.put("ALL", (long) changes.size());
        for (PatchCategory category : PatchCategory.values()) {
            categoryCounts.put(category.name(), 0L);
        }

        Map<String, Long> typeCounts = new LinkedHashMap<>();
        for (PatchChangeType type : PatchChangeType.values()) {
            typeCounts.put(type.name(), 0L);
        }

        long highImpactCount = 0L;
        for (PatchChange change : changes) {
            categoryCounts.merge(change.getCategory().name(), 1L, Long::sum);
            typeCounts.merge(change.getChangeType().name(), 1L, Long::sum);
            if (change.getImpact() == PatchImpact.HIGH) {
                highImpactCount++;
            }
        }

        return PatchChangeStatsResponse.of(
                changes.size(),
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

    private PatchCategory parseCategory(String category) {
        if (!hasText(category)) {
            return null;
        }
        return PatchCategory.from(category);
    }

    private PatchChangeType parseType(String type) {
        if (!hasText(type)) {
            return null;
        }
        return PatchChangeType.from(type);
    }

    private PatchImpact parseImpact(String impact) {
        if (!hasText(impact)) {
            return null;
        }
        return PatchImpact.from(impact);
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
