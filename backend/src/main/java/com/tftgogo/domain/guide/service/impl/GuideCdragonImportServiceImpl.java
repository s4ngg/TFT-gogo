package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.GuideRepository;
import com.tftgogo.domain.guide.service.GuideCdragonImportService;
import com.tftgogo.global.cdragon.config.CommunityDragonProperties;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GuideCdragonImportServiceImpl implements GuideCdragonImportService {

    private static final Logger logger = LogManager.getLogger(GuideCdragonImportServiceImpl.class);
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("@[^@]+@");
    private static final int PATCH_VERSION_MAX_LENGTH = 20;

    private final GuideRepository guideRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CommunityDragonProperties communityDragonProperties;

    @Override
    @Transactional
    public GuideImportResponse importGuides(GuideCdragonImportRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (!request.shouldIncludeChampions() && !request.shouldIncludeTraits()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String patchVersion = normalizePatchVersion(request.getPatchVersion());
        JsonNode setData = findSetData(fetchCdragonData(), request.resolveSetNumber(), request.resolveMutator());
        List<JsonNode> champions = readShopChampions(setData, request.resolveSetNumber());

        List<GuideCandidate> candidates = new ArrayList<>();
        if (request.shouldIncludeChampions()) {
            candidates.addAll(toChampionCandidates(champions, patchVersion));
        }
        if (request.shouldIncludeTraits()) {
            candidates.addAll(toTraitCandidates(setData.path("traits"), champions, patchVersion));
        }

        ImportCounter counter = new ImportCounter();
        for (GuideCandidate candidate : candidates) {
            upsertGuide(candidate, counter);
        }

        return GuideImportResponse.builder()
                .createdCount(counter.createdCount)
                .updatedCount(counter.updatedCount)
                .skippedCount(counter.skippedCount)
                .championCount(countByType(candidates, GuideType.CHAMPION))
                .traitCount(countByType(candidates, GuideType.TRAIT))
                .build();
    }

    private JsonNode fetchCdragonData() {
        try {
            String response = restTemplate.getForObject(communityDragonProperties.getTftKoKrUrl(), String.class);
            if (response == null || response.isBlank()) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
            }
            return objectMapper.readTree(response);
        } catch (RestClientException | JsonProcessingException e) {
            logger.error("Failed to fetch or parse Community Dragon TFT data. error={}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    private JsonNode findSetData(JsonNode root, int setNumber, String mutator) {
        for (JsonNode setData : root.path("setData")) {
            if (setData.path("number").asInt() == setNumber
                    && mutator.equals(setData.path("mutator").asText())) {
                return setData;
            }
        }

        JsonNode fallback = root.path("sets").path(String.valueOf(setNumber));
        if (!fallback.isMissingNode() && fallback.has("champions") && fallback.has("traits")) {
            return fallback;
        }

        throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    private List<JsonNode> readShopChampions(JsonNode setData, int setNumber) {
        String championPrefix = "TFT" + setNumber + "_";
        List<JsonNode> champions = new ArrayList<>();
        for (JsonNode champion : setData.path("champions")) {
            String apiName = champion.path("apiName").asText();
            int cost = champion.path("cost").asInt(0);
            if (apiName.startsWith(championPrefix)
                    && cost >= 1
                    && cost <= 5
                    && hasText(champion.path("name").asText())) {
                champions.add(champion);
            }
        }
        champions.sort(Comparator
                .comparingInt((JsonNode champion) -> champion.path("cost").asInt())
                .thenComparing(champion -> champion.path("name").asText()));
        return champions;
    }

    private List<GuideCandidate> toChampionCandidates(List<JsonNode> champions, String patchVersion) {
        List<GuideCandidate> candidates = new ArrayList<>();
        int sortOrder = 0;
        for (JsonNode champion : champions) {
            ObjectNode dataJson = objectMapper.createObjectNode();
            dataJson.put("cost", champion.path("cost").asInt());
            dataJson.put("role", normalizeRole(champion.path("role").asText()));
            dataJson.put("position", champion.path("stats").path("range").asInt() >= 3 ? "후방" : "전방");
            dataJson.set("traits", toTextArray(champion.path("traits")));
            dataJson.set("bestItems", objectMapper.createArrayNode());
            dataJson.set("stats", toChampionStats(champion.path("stats")));
            dataJson.set("ability", toAbility(champion.path("ability")));

            String summary = buildChampionSummary(champion.path("ability"));
            candidates.add(new GuideCandidate(
                    GuideType.CHAMPION,
                    champion.path("apiName").asText(),
                    champion.path("name").asText(),
                    summary,
                    assetUrl(champion.path("squareIcon").asText(champion.path("icon").asText())),
                    dataJson,
                    patchVersion,
                    sortOrder++
            ));
        }
        return candidates;
    }

    private List<GuideCandidate> toTraitCandidates(JsonNode traits, List<JsonNode> champions, String patchVersion) {
        List<GuideCandidate> candidates = new ArrayList<>();
        int sortOrder = 0;
        for (JsonNode trait : traits) {
            String apiName = trait.path("apiName").asText();
            String name = trait.path("name").asText();
            if (!hasText(apiName) || !hasText(name)) {
                continue;
            }

            ObjectNode dataJson = objectMapper.createObjectNode();
            dataJson.put("count", maxTraitCount(trait.path("effects")));
            dataJson.put("type", "시너지");
            dataJson.put("summary", sanitizeText(trait.path("desc").asText()));
            dataJson.put("tone", traitTone(trait.path("effects")));
            dataJson.set("levels", traitLevels(trait.path("effects")));
            dataJson.set("tips", objectMapper.createArrayNode());
            dataJson.set("champions", traitChampionRefs(name, champions));

            candidates.add(new GuideCandidate(
                    GuideType.TRAIT,
                    apiName,
                    name,
                    sanitizeText(trait.path("desc").asText()),
                    assetUrl(trait.path("icon").asText()),
                    dataJson,
                    patchVersion,
                    sortOrder++
            ));
        }
        return candidates;
    }

    private ObjectNode toChampionStats(JsonNode stats) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("ad", stats.path("damage").asInt());
        data.put("armor", stats.path("armor").asInt());
        data.put("attackSpeed", String.format(Locale.ROOT, "%.2f", stats.path("attackSpeed").asDouble()));
        data.put("hp", stats.path("hp").asInt());
        data.put("mana", stats.path("initialMana").asInt() + "/" + stats.path("mana").asInt());
        data.put("mr", stats.path("magicResist").asInt());
        data.put("range", stats.path("range").asInt());
        return data;
    }

    private ObjectNode toAbility(JsonNode ability) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("name", ability.path("name").asText());
        data.put("description", sanitizeText(ability.path("desc").asText()));
        data.put("iconUrl", assetUrl(ability.path("icon").asText()));
        return data;
    }

    private ArrayNode traitChampionRefs(String traitName, List<JsonNode> champions) {
        ArrayNode refs = objectMapper.createArrayNode();
        for (JsonNode champion : champions) {
            if (!containsText(champion.path("traits"), traitName)) {
                continue;
            }
            ObjectNode ref = objectMapper.createObjectNode();
            ref.put("cost", champion.path("cost").asInt());
            ref.put("imageUrl", assetUrl(champion.path("squareIcon").asText(champion.path("icon").asText())));
            ref.put("name", champion.path("name").asText());
            refs.add(ref);
        }
        return refs;
    }

    private ArrayNode traitLevels(JsonNode effects) {
        ArrayNode levels = objectMapper.createArrayNode();
        for (JsonNode effect : effects) {
            int minUnits = effect.path("minUnits").asInt();
            int maxUnits = effect.path("maxUnits").asInt();
            if (minUnits <= 0) {
                continue;
            }
            levels.add(maxUnits >= 25000 ? minUnits + "+" : String.valueOf(minUnits));
        }
        return levels;
    }

    private int maxTraitCount(JsonNode effects) {
        int max = 0;
        for (JsonNode effect : effects) {
            max = Math.max(max, effect.path("minUnits").asInt());
        }
        return max;
    }

    private String traitTone(JsonNode effects) {
        int maxStyle = 0;
        for (JsonNode effect : effects) {
            maxStyle = Math.max(maxStyle, effect.path("style").asInt());
        }
        if (maxStyle >= 4) {
            return "prismatic";
        }
        if (maxStyle == 3) {
            return "gold";
        }
        if (maxStyle == 2) {
            return "silver";
        }
        return "bronze";
    }

    private ArrayNode toTextArray(JsonNode values) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (JsonNode value : values) {
            if (hasText(value.asText())) {
                arrayNode.add(value.asText());
            }
        }
        return arrayNode;
    }

    private boolean containsText(JsonNode values, String expected) {
        for (JsonNode value : values) {
            if (expected.equals(value.asText())) {
                return true;
            }
        }
        return false;
    }

    private void upsertGuide(GuideCandidate candidate, ImportCounter counter) {
        guideRepository
                .findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                        candidate.guideType(),
                        candidate.targetKey(),
                        candidate.patchVersion()
                )
                .ifPresentOrElse(
                        guide -> {
                            updateGuide(guide, candidate);
                            counter.updatedCount++;
                        },
                        () -> createOrSkipGuide(candidate, counter)
                );
    }

    private void createOrSkipGuide(GuideCandidate candidate, ImportCounter counter) {
        if (guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                candidate.guideType(),
                candidate.targetKey(),
                candidate.patchVersion()
        )) {
            counter.skippedCount++;
            return;
        }

        Guide guide = Guide.builder()
                .guideType(candidate.guideType())
                .targetKey(candidate.targetKey())
                .name(candidate.name())
                .summary(candidate.summary())
                .imageUrl(candidate.imageUrl())
                .dataJson(writeJson(candidate.dataJson()))
                .patchVersion(candidate.patchVersion())
                .sortOrder(candidate.sortOrder())
                .active(true)
                .build();

        try {
            guideRepository.saveAndFlush(guide);
            counter.createdCount++;
        } catch (DataIntegrityViolationException e) {
            handleConcurrentCreate(candidate, counter, e);
        }
    }

    private void handleConcurrentCreate(
            GuideCandidate candidate,
            ImportCounter counter,
            DataIntegrityViolationException exception
    ) {
        guideRepository
                .findByGuideTypeAndTargetKeyAndPatchVersionAndDeletedAtIsNull(
                        candidate.guideType(),
                        candidate.targetKey(),
                        candidate.patchVersion()
                )
                .ifPresentOrElse(
                        guide -> {
                            updateGuide(guide, candidate);
                            counter.updatedCount++;
                        },
                        () -> {
                            if (guideRepository.existsByGuideTypeAndTargetKeyAndPatchVersion(
                                    candidate.guideType(),
                                    candidate.targetKey(),
                                    candidate.patchVersion()
                            )) {
                                counter.skippedCount++;
                                return;
                            }
                            throw exception;
                        }
                );
    }

    private void updateGuide(Guide guide, GuideCandidate candidate) {
        guide.update(
                candidate.guideType(),
                candidate.targetKey(),
                candidate.name(),
                candidate.summary(),
                candidate.imageUrl(),
                writeJson(candidate.dataJson()),
                candidate.patchVersion(),
                candidate.sortOrder(),
                guide.isActive()
        );
    }

    private String writeJson(JsonNode dataJson) {
        try {
            return objectMapper.writeValueAsString(dataJson);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize imported guide dataJson. error={}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.GUIDE_INVALID_DATA);
        }
    }

    private int countByType(List<GuideCandidate> candidates, GuideType guideType) {
        int count = 0;
        for (GuideCandidate candidate : candidates) {
            if (candidate.guideType() == guideType) {
                count++;
            }
        }
        return count;
    }

    private String assetUrl(String assetPath) {
        if (!hasText(assetPath)) {
            return null;
        }
        return communityDragonProperties.getAssetBaseUrl()
                + "/"
                + assetPath.toLowerCase(Locale.ROOT).replace(".tex", ".png");
    }

    private String buildChampionSummary(JsonNode ability) {
        String abilityName = ability.path("name").asText();
        String description = sanitizeText(ability.path("desc").asText());
        if (!hasText(abilityName)) {
            return description;
        }
        if (!hasText(description)) {
            return abilityName;
        }
        return abilityName + " - " + description;
    }

    private String sanitizeText(String value) {
        if (!hasText(value)) {
            return "";
        }
        return PLACEHOLDER_PATTERN.matcher(TAG_PATTERN.matcher(value).replaceAll(" "))
                .replaceAll("")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeRole(String role) {
        if (!hasText(role)) {
            return "미분류";
        }
        return role;
    }

    private String normalizeRequired(String value) {
        if (!hasText(value)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return value.trim();
    }

    private String normalizePatchVersion(String value) {
        String normalized = normalizeRequired(value);
        if (normalized.length() > PATCH_VERSION_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record GuideCandidate(
            GuideType guideType,
            String targetKey,
            String name,
            String summary,
            String imageUrl,
            JsonNode dataJson,
            String patchVersion,
            int sortOrder
    ) {
    }

    private static class ImportCounter {
        private int createdCount;
        private int updatedCount;
        private int skippedCount;
    }
}
