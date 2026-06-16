package com.tftgogo.domain.patchnote.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchChangeRequest;
import com.tftgogo.domain.patchnote.dto.request.AdminPatchNoteRequest;
import com.tftgogo.domain.patchnote.dto.response.PatchChangeResponse;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteResponse;
import com.tftgogo.domain.patchnote.entity.PatchChange;
import com.tftgogo.domain.patchnote.entity.PatchChangeCategory;
import com.tftgogo.domain.patchnote.entity.PatchChangeImpact;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository;
import com.tftgogo.domain.patchnote.repository.PatchChangeRepository.PatchChangeCount;
import com.tftgogo.domain.patchnote.repository.PatchNoteRepository;
import com.tftgogo.domain.patchnote.service.AdminPatchNoteService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminPatchNoteServiceImpl implements AdminPatchNoteService {

    private static final Logger logger = LogManager.getLogger(AdminPatchNoteServiceImpl.class);

    private final PatchNoteRepository patchNoteRepository;
    private final PatchChangeRepository patchChangeRepository;
    private final ObjectMapper objectMapper;

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
                .forEach(this::deletePatchChangeByAdmin);
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

    private PatchNote findPatchNote(Long patchNoteId) {
        return patchNoteRepository.findByIdAndDeletedAtIsNull(patchNoteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATCH_NOTE_NOT_FOUND));
    }

    private PatchChange findPatchChange(Long changeId) {
        return patchChangeRepository.findById(changeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PATCH_CHANGE_NOT_FOUND));
    }

    private void deletePatchChangeByAdmin(PatchChange patchChange) {
        patchChange.markManuallyEditedIfImported();
        patchChangeRepository.delete(patchChange);
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
        currentPatchNotes.forEach(PatchNote::markNotCurrent);
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
        return patchChangeRepository
                .findByPatchNoteOrderBySortOrderAscIdAsc(patchNote)
                .size();
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
}
