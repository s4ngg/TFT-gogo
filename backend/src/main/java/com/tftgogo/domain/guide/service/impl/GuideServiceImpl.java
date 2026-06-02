package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.response.GuideEntryResponse;
import com.tftgogo.domain.guide.dto.response.GuidePageResponse;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.GuideRepository;
import com.tftgogo.domain.guide.service.GuideService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuideServiceImpl implements GuideService {

    private static final Logger logger = LogManager.getLogger(GuideServiceImpl.class);
    private static final int DEFAULT_PAGE = 1;
    private static final int MAX_PAGE = 10_000;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORT_KEYS = Set.of("avgPlace", "pickRate", "top4", "winRate");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private final GuideRepository guideRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<GuideEntryResponse> getGuideCatalog() {
        return guideRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public GuidePageResponse<GuideEntryResponse> getGuideTabItems(
            String tab,
            String patchVersion,
            String query,
            Integer page,
            Integer pageSize,
            String sortKey,
            String sortDir,
            Integer cost
    ) {
        GuideType guideType = GuideType.fromTab(tab);
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        validateSort(sortKey, sortDir);
        validateCost(cost);

        List<GuideItem> filteredItems = guideRepository
                .findFilteredGuides(
                        guideType.name(),
                        normalizeText(patchVersion),
                        normalizeText(query),
                        cost
                )
                .stream()
                .map(this::toGuideItem)
                .sorted(buildComparator(sortKey, sortDir))
                .toList();

        long totalItems = filteredItems.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / normalizedPageSize));
        long fromIndexLong = Math.min((long) (normalizedPage - 1) * normalizedPageSize, filteredItems.size());
        int fromIndex = (int) fromIndexLong;
        int toIndex = (int) Math.min(fromIndexLong + normalizedPageSize, filteredItems.size());

        List<GuideEntryResponse> responses = filteredItems.subList(fromIndex, toIndex).stream()
                .map(item -> GuideEntryResponse.from(item.guide(), item.dataJson()))
                .toList();

        return GuidePageResponse.of(responses, normalizedPage, normalizedPageSize, totalItems, totalPages);
    }

    private GuideEntryResponse toResponse(Guide guide) {
        return GuideEntryResponse.from(guide, parseDataJson(guide));
    }

    private GuideItem toGuideItem(Guide guide) {
        return new GuideItem(guide, parseDataJson(guide));
    }

    private JsonNode parseDataJson(Guide guide) {
        try {
            JsonNode dataJson = objectMapper.readTree(guide.getDataJson());
            if (dataJson == null || !dataJson.isObject()) {
                throw new BusinessException(ErrorCode.GUIDE_INVALID_DATA);
            }
            return dataJson;
        } catch (JsonProcessingException e) {
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

    private void validateSort(String sortKey, String sortDir) {
        if (hasText(sortKey) && !SORT_KEYS.contains(sortKey)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (hasText(sortDir) && !sortDir.equals("asc") && !sortDir.equals("desc")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateCost(Integer cost) {
        if (cost != null && (cost < 1 || cost > 5)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private Comparator<GuideItem> buildComparator(String sortKey, String sortDir) {
        if (!hasText(sortKey)) {
            return Comparator.comparingInt((GuideItem item) -> item.guide().getSortOrder())
                    .thenComparing(item -> item.guide().getId());
        }

        boolean ascending = "asc".equals(sortDir);
        return (left, right) -> {
            Double leftMetric = readMetric(left.dataJson(), sortKey);
            Double rightMetric = readMetric(right.dataJson(), sortKey);

            if (leftMetric == null && rightMetric == null) {
                return compareDefault(left, right);
            }
            if (leftMetric == null) {
                return 1;
            }
            if (rightMetric == null) {
                return -1;
            }

            int metricResult = ascending
                    ? Double.compare(leftMetric, rightMetric)
                    : Double.compare(rightMetric, leftMetric);

            return metricResult != 0 ? metricResult : compareDefault(left, right);
        };
    }

    private int compareDefault(GuideItem left, GuideItem right) {
        int sortOrderResult = Integer.compare(left.guide().getSortOrder(), right.guide().getSortOrder());
        if (sortOrderResult != 0) {
            return sortOrderResult;
        }
        return Long.compare(left.guide().getId(), right.guide().getId());
    }

    private Double readMetric(JsonNode dataJson, String sortKey) {
        JsonNode value = dataJson.get(sortKey);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asDouble();
        }
        if (!value.isTextual()) {
            return null;
        }

        String normalized = value.asText().replaceAll("\\s+", "").replace(',', '.');
        Matcher matcher = NUMBER_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Double.parseDouble(matcher.group());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeText(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record GuideItem(Guide guide, JsonNode dataJson) {
    }
}
