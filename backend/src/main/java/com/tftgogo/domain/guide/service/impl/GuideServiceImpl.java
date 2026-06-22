package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tftgogo.domain.guide.dto.response.AugmentGuidePlanResponse;
import com.tftgogo.domain.guide.dto.response.GuideCatalogResponse;
import com.tftgogo.domain.guide.dto.response.GuideEntryResponse;
import com.tftgogo.domain.guide.dto.response.GuidePageResponse;
import com.tftgogo.domain.guide.entity.AugmentGuidePlan;
import com.tftgogo.domain.guide.entity.GuideAugment;
import com.tftgogo.domain.guide.entity.GuideChampion;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideItem;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.AugmentGuidePlanRepository;
import com.tftgogo.domain.guide.repository.GuideAugmentRepository;
import com.tftgogo.domain.guide.repository.GuideChampionRepository;
import com.tftgogo.domain.guide.repository.GuideItemRepository;
import com.tftgogo.domain.guide.repository.GuideRepository;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.domain.guide.service.GuideService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    private static final Pattern PATCH_VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)([A-Za-z]*)$");
    private static final String LIKE_ESCAPE = "\\";

    private final GuideRepository guideRepository;
    private final GuideChampionRepository guideChampionRepository;
    private final GuideTraitRepository guideTraitRepository;
    private final GuideItemRepository guideItemRepository;
    private final GuideAugmentRepository guideAugmentRepository;
    private final AugmentGuidePlanRepository augmentGuidePlanRepository;
    private final ObjectMapper objectMapper;

    @Override
    public GuideCatalogResponse getGuideCatalog() {
        return resolvePatchVersion(null)
                .map(patchVersion -> {
                    List<GuideEntryResponse> entries = findCatalogEntries(patchVersion);
                    return GuideCatalogResponse.of(
                            patchVersion,
                            entries,
                            findAugmentPlans(patchVersion)
                    );
                })
                .orElseGet(() -> GuideCatalogResponse.of("", List.of(), List.of()));
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

        Optional<String> resolvedPatchVersion = resolvePatchVersion(patchVersion);
        if (resolvedPatchVersion.isEmpty()) {
            return GuidePageResponse.of(List.of(), normalizedPage, normalizedPageSize, 0, 1);
        }

        List<GuideEntryResponse> splitItems = findSplitTabEntries(guideType, resolvedPatchVersion.get());
        if (!splitItems.isEmpty()) {
            List<GuideEntryResponse> filteredSplitItems = splitItems.stream()
                    .filter(item -> matchesSearch(item, query))
                    .filter(item -> matchesCost(item, guideType, cost))
                    .sorted(buildResponseComparator(sortKey, sortDir))
                    .toList();

            return toPageResponse(filteredSplitItems, normalizedPage, normalizedPageSize);
        }

        List<LegacyGuideItem> filteredItems = guideRepository
                .findFilteredGuides(
                        guideType.name(),
                        resolvedPatchVersion.get(),
                        normalizeSearchQuery(query),
                        cost
                )
                .stream()
                .map(this::toLegacyGuideItem)
                .filter(item -> isDisplayableEntry(guideType, item.dataJson()))
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

    private GuidePageResponse<GuideEntryResponse> toPageResponse(
            List<GuideEntryResponse> items,
            int normalizedPage,
            int normalizedPageSize
    ) {
        long totalItems = items.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / normalizedPageSize));
        long fromIndexLong = Math.min((long) (normalizedPage - 1) * normalizedPageSize, items.size());
        int fromIndex = (int) fromIndexLong;
        int toIndex = (int) Math.min(fromIndexLong + normalizedPageSize, items.size());

        return GuidePageResponse.of(
                items.subList(fromIndex, toIndex),
                normalizedPage,
                normalizedPageSize,
                totalItems,
                totalPages
        );
    }

    private List<GuideEntryResponse> findSplitCatalogEntries(String patchVersion) {
        List<GuideEntryResponse> entries = new ArrayList<>();
        entries.addAll(findSplitTabEntries(GuideType.TRAIT, patchVersion));
        entries.addAll(findSplitTabEntries(GuideType.ITEM, patchVersion));
        entries.addAll(findSplitTabEntries(GuideType.AUGMENT, patchVersion));
        entries.addAll(findSplitTabEntries(GuideType.CHAMPION, patchVersion));
        return entries;
    }

    private List<GuideEntryResponse> findCatalogEntries(String patchVersion) {
        List<GuideEntryResponse> splitEntries = findSplitCatalogEntries(patchVersion);
        if (!splitEntries.isEmpty()) {
            return splitEntries;
        }
        return guideRepository
                .findByPatchVersionAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc(patchVersion)
                .stream()
                .map(this::toResponse)
                .filter(this::isDisplayableResponse)
                .toList();
    }

    private List<AugmentGuidePlanResponse> findAugmentPlans(String patchVersion) {
        return augmentGuidePlanRepository.findByPatchVersionOrderByPlanKeyAscIdAsc(patchVersion)
                .stream()
                .map(this::toAugmentPlanResponse)
                .toList();
    }

    private AugmentGuidePlanResponse toAugmentPlanResponse(AugmentGuidePlan plan) {
        return AugmentGuidePlanResponse.from(
                plan,
                parseJson(plan.getStagesJson(), "augmentPlan.stages", plan.getId())
        );
    }

    private List<GuideEntryResponse> findSplitTabEntries(GuideType guideType, String patchVersion) {
        return switch (guideType) {
            case CHAMPION -> toChampionResponses(
                    guideChampionRepository.findByPatchVersionOrderByCostAscNameAscIdAsc(patchVersion)
            );
            case TRAIT -> toTraitResponses(
                    guideTraitRepository.findByPatchVersionOrderByNameAscIdAsc(patchVersion)
            );
            case ITEM -> toItemResponses(
                    guideItemRepository.findByPatchVersionOrderByNameAscIdAsc(patchVersion)
            );
            case AUGMENT -> toAugmentResponses(
                    guideAugmentRepository.findByPatchVersionOrderByNameAscIdAsc(patchVersion)
            );
        };
    }

    private List<GuideEntryResponse> toChampionResponses(List<GuideChampion> champions) {
        List<GuideEntryResponse> responses = new ArrayList<>();
        int sortOrder = 0;
        for (GuideChampion champion : champions) {
            ObjectNode dataJson = objectMapper.createObjectNode();
            dataJson.put("cost", champion.getCost());
            dataJson.put("role", champion.getRole());
            dataJson.put("position", champion.getPosition());
            dataJson.set("stats", parseJson(champion.getStatsJson(), "champion.stats", champion.getId()));
            dataJson.set("traits", parseJson(champion.getTraitsJson(), "champion.traits", champion.getId()));
            dataJson.set("bestItems", parseJson(champion.getBestItemsJson(), "champion.bestItems", champion.getId()));

            responses.add(buildResponse(
                    champion.getId(),
                    GuideType.CHAMPION,
                    champion.getChampionKey(),
                    champion.getName(),
                    "",
                    champion.getImageUrl(),
                    champion.getPatchVersion(),
                    sortOrder++,
                    dataJson
            ));
        }
        return responses;
    }

    private List<GuideEntryResponse> toTraitResponses(List<GuideTrait> traits) {
        List<GuideEntryResponse> responses = new ArrayList<>();
        int sortOrder = 0;
        for (GuideTrait trait : traits) {
            JsonNode levels = parseJson(trait.getLevelsJson(), "trait.levels", trait.getId());
            JsonNode champions = parseJson(trait.getChampionsJson(), "trait.champions", trait.getId());
            if (!hasArrayItems(champions)) {
                logger.debug(
                        "Guide trait response skipped because champions_json is empty. id={}, traitKey={}, name={}",
                        trait.getId(),
                        trait.getTraitKey(),
                        trait.getName()
                );
                continue;
            }

            ObjectNode dataJson = objectMapper.createObjectNode();
            dataJson.put("count", maxTraitLevel(levels));
            dataJson.put("type", trait.getType());
            dataJson.put("summary", trait.getSummary());
            dataJson.put("tone", trait.getTone());
            dataJson.set("levels", levels);
            dataJson.set("tierEffects", parseJson(trait.getTierEffectsJson(), "trait.tierEffects", trait.getId()));
            dataJson.set("champions", champions);
            dataJson.set("tips", parseJson(trait.getTipsJson(), "trait.tips", trait.getId()));

            responses.add(buildResponse(
                    trait.getId(),
                    GuideType.TRAIT,
                    trait.getTraitKey(),
                    trait.getName(),
                    trait.getSummary(),
                    trait.getIconUrl(),
                    trait.getPatchVersion(),
                    sortOrder++,
                    dataJson
            ));
        }
        return responses;
    }

    private List<GuideEntryResponse> toItemResponses(List<GuideItem> items) {
        List<GuideEntryResponse> responses = new ArrayList<>();
        int sortOrder = 0;
        for (GuideItem item : items) {
            ObjectNode dataJson = objectMapper.createObjectNode();
            dataJson.put("category", item.getCategory());
            dataJson.put("description", item.getDescription() == null ? "" : item.getDescription());
            dataJson.set("bestUsers", parseJson(item.getBestUsersJson(), "item.bestUsers", item.getId()));
            dataJson.set("combinations", parseJson(item.getCombinationsJson(), "item.combinations", item.getId()));

            responses.add(buildResponse(
                    item.getId(),
                    GuideType.ITEM,
                    item.getItemKey(),
                    item.getName(),
                    item.getDescription(),
                    item.getImageUrl(),
                    item.getPatchVersion(),
                    sortOrder++,
                    dataJson
            ));
        }
        return responses;
    }

    private List<GuideEntryResponse> toAugmentResponses(List<GuideAugment> augments) {
        List<GuideEntryResponse> responses = new ArrayList<>();
        int sortOrder = 0;
        for (GuideAugment augment : augments) {
            ObjectNode dataJson = objectMapper.createObjectNode();
            dataJson.put("description", augment.getDescription());
            dataJson.set("tags", parseJson(augment.getTagsJson(), "augment.tags", augment.getId()));

            responses.add(buildResponse(
                    augment.getId(),
                    GuideType.AUGMENT,
                    augment.getAugmentKey(),
                    augment.getName(),
                    augment.getDescription(),
                    augment.getIconUrl(),
                    augment.getPatchVersion(),
                    sortOrder++,
                    dataJson
            ));
        }
        return responses;
    }

    private GuideEntryResponse buildResponse(
            Long id,
            GuideType guideType,
            String targetKey,
            String name,
            String summary,
            String imageUrl,
            String patchVersion,
            int sortOrder,
            JsonNode dataJson
    ) {
        return GuideEntryResponse.builder()
                .id(id)
                .guideType(guideType)
                .targetKey(targetKey)
                .name(name)
                .summary(summary)
                .imageUrl(imageUrl)
                .patchVersion(patchVersion)
                .sortOrder(sortOrder)
                .dataJson(dataJson)
                .build();
    }

    private GuideEntryResponse toResponse(Guide guide) {
        return GuideEntryResponse.from(guide, parseDataJson(guide));
    }

    private LegacyGuideItem toLegacyGuideItem(Guide guide) {
        return new LegacyGuideItem(guide, parseDataJson(guide));
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

    private JsonNode parseJson(String value, String fieldName, Long id) {
        try {
            JsonNode dataJson = objectMapper.readTree(value);
            if (dataJson == null || dataJson.isNull()) {
                throw new BusinessException(ErrorCode.GUIDE_INVALID_DATA);
            }
            return dataJson;
        } catch (JsonProcessingException e) {
            logger.error(
                    "Invalid split guide JSON. field={}, id={}, error={}",
                    fieldName,
                    id,
                    e.getMessage(),
                    e
            );
            throw new BusinessException(ErrorCode.GUIDE_INVALID_DATA);
        }
    }

    private int maxTraitLevel(JsonNode levels) {
        int maxLevel = 0;
        if (!levels.isArray()) {
            return maxLevel;
        }
        for (JsonNode level : levels) {
            String normalized = level.asText().replace("+", "").trim();
            try {
                maxLevel = Math.max(maxLevel, Integer.parseInt(normalized));
            } catch (NumberFormatException ignored) {
                // Non-numeric trait levels are ignored for display count.
            }
        }
        return maxLevel;
    }

    private boolean matchesSearch(GuideEntryResponse item, String query) {
        if (!hasText(query)) {
            return true;
        }
        String normalizedQuery = query.trim().toLowerCase();
        return containsIgnoreCase(item.getName(), normalizedQuery)
                || containsIgnoreCase(item.getSummary(), normalizedQuery)
                || containsIgnoreCase(item.getTargetKey(), normalizedQuery);
    }

    private boolean matchesCost(GuideEntryResponse item, GuideType guideType, Integer cost) {
        return cost == null
                || guideType != GuideType.CHAMPION
                || item.getDataJson().path("cost").asInt() == cost;
    }

    private boolean isDisplayableResponse(GuideEntryResponse response) {
        return isDisplayableEntry(response.getGuideType(), response.getDataJson());
    }

    private boolean isDisplayableEntry(GuideType guideType, JsonNode dataJson) {
        return guideType != GuideType.TRAIT || hasArrayItems(dataJson.path("champions"));
    }

    private boolean hasArrayItems(JsonNode value) {
        return value != null && value.isArray() && value.size() > 0;
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        return hasText(value) && value.toLowerCase().contains(normalizedQuery);
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

    private Comparator<LegacyGuideItem> buildComparator(String sortKey, String sortDir) {
        if (!hasText(sortKey)) {
            return Comparator.comparingInt((LegacyGuideItem item) -> item.guide().getSortOrder())
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

    private Comparator<GuideEntryResponse> buildResponseComparator(String sortKey, String sortDir) {
        if (!hasText(sortKey)) {
            return Comparator.comparingInt(GuideEntryResponse::getSortOrder)
                    .thenComparing(GuideEntryResponse::getId);
        }

        boolean ascending = "asc".equals(sortDir);
        return (left, right) -> {
            Double leftMetric = readMetric(left.getDataJson(), sortKey);
            Double rightMetric = readMetric(right.getDataJson(), sortKey);

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

    private int compareDefault(LegacyGuideItem left, LegacyGuideItem right) {
        int sortOrderResult = Integer.compare(left.guide().getSortOrder(), right.guide().getSortOrder());
        if (sortOrderResult != 0) {
            return sortOrderResult;
        }
        return Long.compare(left.guide().getId(), right.guide().getId());
    }

    private int compareDefault(GuideEntryResponse left, GuideEntryResponse right) {
        int sortOrderResult = Integer.compare(left.getSortOrder(), right.getSortOrder());
        if (sortOrderResult != 0) {
            return sortOrderResult;
        }
        return Long.compare(left.getId(), right.getId());
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

    private Optional<String> resolvePatchVersion(String patchVersion) {
        if (hasText(patchVersion)) {
            return Optional.of(patchVersion.trim());
        }
        List<String> patchVersions = new ArrayList<>();
        guideRepository.findLatestPatchVersion().ifPresent(patchVersions::add);
        guideChampionRepository.findLatestPatchVersion().ifPresent(patchVersions::add);
        guideTraitRepository.findLatestPatchVersion().ifPresent(patchVersions::add);
        guideItemRepository.findLatestPatchVersion().ifPresent(patchVersions::add);
        guideAugmentRepository.findLatestPatchVersion().ifPresent(patchVersions::add);

        return patchVersions.stream().max(this::comparePatchVersion);
    }

    private int comparePatchVersion(String left, String right) {
        PatchVersionParts leftParts = parsePatchVersion(left);
        PatchVersionParts rightParts = parsePatchVersion(right);

        int majorResult = Integer.compare(leftParts.major(), rightParts.major());
        if (majorResult != 0) {
            return majorResult;
        }

        int minorResult = Integer.compare(leftParts.minor(), rightParts.minor());
        if (minorResult != 0) {
            return minorResult;
        }

        int suffixResult = leftParts.suffix().compareTo(rightParts.suffix());
        if (suffixResult != 0) {
            return suffixResult;
        }

        return left.compareTo(right);
    }

    private PatchVersionParts parsePatchVersion(String patchVersion) {
        if (!hasText(patchVersion)) {
            return new PatchVersionParts(0, 0, "");
        }
        Matcher matcher = PATCH_VERSION_PATTERN.matcher(patchVersion.trim());
        if (!matcher.matches()) {
            return new PatchVersionParts(0, 0, patchVersion);
        }
        return new PatchVersionParts(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                matcher.group(3)
        );
    }

    private String normalizeSearchQuery(String value) {
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

    private record LegacyGuideItem(Guide guide, JsonNode dataJson) {
    }

    private record PatchVersionParts(int major, int minor, String suffix) {
    }
}
