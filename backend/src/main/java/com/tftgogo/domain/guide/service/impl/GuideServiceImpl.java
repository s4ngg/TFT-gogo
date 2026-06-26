package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tftgogo.domain.guide.dto.response.GuideCatalogResponse;
import com.tftgogo.domain.guide.dto.response.GuideEntryResponse;
import com.tftgogo.domain.guide.dto.response.GuidePageResponse;
import com.tftgogo.domain.guide.entity.GuideAugment;
import com.tftgogo.domain.guide.entity.GuideChampion;
import com.tftgogo.domain.guide.entity.GuideItem;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.GuideAugmentRepository;
import com.tftgogo.domain.guide.repository.GuideChampionRepository;
import com.tftgogo.domain.guide.repository.GuideItemRepository;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.domain.guide.service.GuideService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private static final Pattern PATCH_VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)([A-Za-z]*)$");

    private final GuideChampionRepository guideChampionRepository;
    private final GuideTraitRepository guideTraitRepository;
    private final GuideItemRepository guideItemRepository;
    private final GuideAugmentRepository guideAugmentRepository;
    private final ObjectMapper objectMapper;

    @Override
    public GuideCatalogResponse getGuideCatalog() {
        return resolvePatchVersion(null)
                .map(patchVersion -> {
                    List<GuideEntryResponse> entries = findCatalogEntries(patchVersion);
                    return GuideCatalogResponse.of(
                            patchVersion,
                            entries
                    );
                })
                .orElseGet(() -> GuideCatalogResponse.of("", List.of()));
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

        Optional<String> resolvedPatchVersion = resolvePatchVersion(guideType, patchVersion);
        if (resolvedPatchVersion.isEmpty()) {
            return GuidePageResponse.of(List.of(), normalizedPage, normalizedPageSize, 0, 1);
        }

        return findSplitTabPage(
                guideType,
                resolvedPatchVersion.get(),
                query,
                normalizedPage,
                normalizedPageSize,
                cost
        );
    }

    private GuidePageResponse<GuideEntryResponse> findSplitTabPage(
            GuideType guideType,
            String patchVersion,
            String query,
            int normalizedPage,
            int normalizedPageSize,
            Integer cost
    ) {
        PageRequest pageRequest = PageRequest.of(normalizedPage - 1, normalizedPageSize);
        String normalizedQuery = normalizeQuery(query);

        return switch (guideType) {
            case CHAMPION -> {
                Page<GuideChampion> page = guideChampionRepository.searchPage(
                        patchVersion,
                        normalizedQuery,
                        cost,
                        pageRequest
                );
                yield toPageResponse(toChampionResponses(page.getContent()), page, normalizedPage, normalizedPageSize);
            }
            case TRAIT -> {
                boolean hideBaseStargazer = guideTraitRepository.countStargazerVariantsByPatchVersion(patchVersion) > 0;
                Page<GuideTrait> page = guideTraitRepository.searchPage(
                        patchVersion,
                        normalizedQuery,
                        hideBaseStargazer,
                        pageRequest
                );
                yield toPageResponse(toTraitResponses(page.getContent()), page, normalizedPage, normalizedPageSize);
            }
            case ITEM -> {
                Page<GuideItem> page = guideItemRepository.searchPage(
                        patchVersion,
                        normalizedQuery,
                        pageRequest
                );
                yield toPageResponse(toItemResponses(page.getContent()), page, normalizedPage, normalizedPageSize);
            }
            case AUGMENT -> {
                Page<GuideAugment> page = guideAugmentRepository.searchPage(
                        patchVersion,
                        normalizedQuery,
                        pageRequest
                );
                yield toPageResponse(toAugmentResponses(page.getContent()), page, normalizedPage, normalizedPageSize);
            }
        };
    }

    private GuidePageResponse<GuideEntryResponse> toPageResponse(
            List<GuideEntryResponse> items,
            Page<?> page,
            int normalizedPage,
            int normalizedPageSize
    ) {
        return GuidePageResponse.of(
                items,
                normalizedPage,
                normalizedPageSize,
                page.getTotalElements(),
                Math.max(1, page.getTotalPages())
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
        return findSplitCatalogEntries(patchVersion);
    }

    private List<GuideEntryResponse> findSplitTabEntries(GuideType guideType, String patchVersion) {
        return switch (guideType) {
            case CHAMPION -> toChampionResponses(
                    guideChampionRepository.findByPatchVersionOrderByNameAscIdAsc(patchVersion)
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
            JsonNode traits = parseJson(champion.getTraitsJson(), "champion.traits", champion.getId());
            if (!hasArrayItems(traits)) {
                logger.debug(
                        "Guide champion response skipped because traits_json is empty. id={}, championKey={}, name={}",
                        champion.getId(),
                        champion.getChampionKey(),
                        champion.getName()
                );
                continue;
            }

            ObjectNode dataJson = objectMapper.createObjectNode();
            dataJson.put("cost", champion.getCost());
            dataJson.put("role", champion.getRole());
            dataJson.put("position", champion.getPosition());
            dataJson.set("stats", parseJson(champion.getStatsJson(), "champion.stats", champion.getId()));
            dataJson.set("traits", traits);
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
        boolean hasStargazerVariants = traits.stream()
                .anyMatch(trait -> hasText(stargazerVariantFromTraitKey(trait.getTraitKey())));
        for (GuideTrait trait : traits) {
            if (hasStargazerVariants && isBaseStargazerTrait(trait.getTraitKey())) {
                continue;
            }

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
            String variant = stargazerVariantFromTraitKey(trait.getTraitKey());
            if (hasText(variant)) {
                dataJson.put("variant", variant);
            }
            dataJson.set("levels", levels);
            dataJson.set("tierEffects", parseJson(trait.getTierEffectsJson(), "trait.tierEffects", trait.getId()));
            dataJson.set("champions", champions);
            dataJson.set("specialUnits", parseJson(trait.getSpecialUnitsJson(), "trait.specialUnits", trait.getId()));
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

    private boolean isBaseStargazerTrait(String traitKey) {
        return hasText(traitKey) && traitKey.matches("TFT\\d+_Stargazer");
    }

    private String stargazerVariantFromTraitKey(String traitKey) {
        if (!hasText(traitKey)) {
            return "";
        }
        return switch (traitKey) {
            case "TFT17_Stargazer_Wolf" -> "멧돼지";
            case "TFT17_Stargazer_Medallion" -> "메달";
            case "TFT17_Stargazer_Huntress" -> "여사냥꾼";
            case "TFT17_Stargazer_Serpent" -> "뱀";
            case "TFT17_Stargazer_Shield" -> "제단";
            case "TFT17_Stargazer_Fountain" -> "우물";
            case "TFT17_Stargazer_Mountain" -> "산";
            default -> "";
        };
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

    private String normalizeQuery(String query) {
        if (!hasText(query)) {
            return null;
        }
        return query.trim().toLowerCase();
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
        // Split guide tables do not persist metric columns yet, so sort requests must fail explicitly.
        if (hasText(sortKey) || hasText(sortDir)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateCost(Integer cost) {
        if (cost != null && (cost < 1 || cost > 5)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private boolean hasArrayItems(JsonNode value) {
        return value != null && value.isArray() && value.size() > 0;
    }

    private Optional<String> resolvePatchVersion(String patchVersion) {
        if (hasText(patchVersion)) {
            return Optional.of(patchVersion.trim());
        }
        List<String> patchVersions = new ArrayList<>();
        guideChampionRepository.findLatestPatchVersion().ifPresent(patchVersions::add);
        guideTraitRepository.findLatestPatchVersion().ifPresent(patchVersions::add);
        guideItemRepository.findLatestPatchVersion().ifPresent(patchVersions::add);
        guideAugmentRepository.findLatestPatchVersion().ifPresent(patchVersions::add);

        return patchVersions.stream().max(this::comparePatchVersion);
    }

    private Optional<String> resolvePatchVersion(GuideType guideType, String patchVersion) {
        if (hasText(patchVersion)) {
            return Optional.of(patchVersion.trim());
        }
        return switch (guideType) {
            case CHAMPION -> guideChampionRepository.findLatestPatchVersion();
            case TRAIT -> guideTraitRepository.findLatestPatchVersion();
            case ITEM -> guideItemRepository.findLatestPatchVersion();
            case AUGMENT -> guideAugmentRepository.findLatestPatchVersion();
        };
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record PatchVersionParts(int major, int minor, String suffix) {
    }
}
