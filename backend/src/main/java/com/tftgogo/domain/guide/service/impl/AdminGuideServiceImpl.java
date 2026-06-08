package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.request.AdminGuideRequest;
import com.tftgogo.domain.guide.dto.response.AdminGuideResponse;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.GuideRepository;
import com.tftgogo.domain.guide.service.AdminGuideService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminGuideServiceImpl implements AdminGuideService {

    private static final Logger logger = LogManager.getLogger(AdminGuideServiceImpl.class);

    private final GuideRepository guideRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AdminGuideResponse> getAdminGuides(GuideType guideType, String patchVersion, Boolean active) {
        return guideRepository.findAdminGuides(guideType, normalizeOptional(patchVersion), active).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AdminGuideResponse createGuide(AdminGuideRequest request) {
        validateRequest(request);
        String targetKey = normalizeRequired(request.getTargetKey());
        String patchVersion = normalizeRequired(request.getPatchVersion());
        String dataJson = serializeDataJson(request.getDataJson());
        validateDuplicate(request.getGuideType(), targetKey, patchVersion, null);

        Guide guide = Guide.builder()
                .guideType(request.getGuideType())
                .targetKey(targetKey)
                .name(normalizeRequired(request.getName()))
                .summary(normalizeOptional(request.getSummary()))
                .imageUrl(normalizeOptional(request.getImageUrl()))
                .dataJson(dataJson)
                .patchVersion(patchVersion)
                .sortOrder(normalizeSortOrder(request.getSortOrder()))
                .active(request.resolveActive(true))
                .build();

        try {
            return toResponse(guideRepository.saveAndFlush(guide));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.GUIDE_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional
    public AdminGuideResponse updateGuide(Long guideId, AdminGuideRequest request) {
        validateRequest(request);
        Guide guide = guideRepository.findByIdAndDeletedAtIsNull(guideId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GUIDE_NOT_FOUND));

        String targetKey = normalizeRequired(request.getTargetKey());
        String patchVersion = normalizeRequired(request.getPatchVersion());
        String dataJson = serializeDataJson(request.getDataJson());
        validateDuplicate(request.getGuideType(), targetKey, patchVersion, guideId);

        guide.update(
                request.getGuideType(),
                targetKey,
                normalizeRequired(request.getName()),
                normalizeOptional(request.getSummary()),
                normalizeOptional(request.getImageUrl()),
                dataJson,
                patchVersion,
                normalizeSortOrder(request.getSortOrder()),
                request.resolveActive(guide.isActive())
        );

        try {
            guideRepository.flush();
            return toResponse(guide);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.GUIDE_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional
    public void deleteGuide(Long guideId) {
        Guide guide = guideRepository.findByIdAndDeletedAtIsNull(guideId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GUIDE_NOT_FOUND));
        guide.softDelete();
    }

    private void validateDuplicate(GuideType guideType, String targetKey, String patchVersion, Long excludeId) {
        if (guideType == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        boolean duplicated = excludeId == null
                ? guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(guideType, targetKey, patchVersion)
                : guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersionAndIdNot(
                        guideType,
                        targetKey,
                        patchVersion,
                        excludeId
                );

        if (duplicated) {
            throw new BusinessException(ErrorCode.GUIDE_ALREADY_EXISTS);
        }
    }

    private void validateRequest(AdminGuideRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private AdminGuideResponse toResponse(Guide guide) {
        return AdminGuideResponse.from(guide, parseDataJson(guide));
    }

    private String serializeDataJson(JsonNode dataJson) {
        if (dataJson == null || !dataJson.isObject()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        try {
            return objectMapper.writeValueAsString(dataJson);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize guide dataJson. error={}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private JsonNode parseDataJson(Guide guide) {
        String rawDataJson = guide.getDataJson();
        if (rawDataJson == null || rawDataJson.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.GUIDE_INVALID_DATA);
        }

        try {
            JsonNode dataJson = objectMapper.readTree(rawDataJson);
            if (dataJson == null || !dataJson.isObject()) {
                throw new BusinessException(ErrorCode.GUIDE_INVALID_DATA);
            }
            return dataJson;
        } catch (JsonProcessingException | IllegalArgumentException e) {
            logger.error(
                    "Invalid guide dataJson. guideId={}, targetKey={}, error={}",
                    guide.getId(),
                    guide.getTargetKey(),
                    e.getMessage(),
                    e
            );
            throw new BusinessException(ErrorCode.GUIDE_INVALID_DATA);
        }
    }

    private String normalizeRequired(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private int normalizeSortOrder(Integer sortOrder) {
        if (sortOrder == null || sortOrder < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return sortOrder;
    }
}
