package com.tftgogo.domain.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.ai.client.AiServerClient;
import com.tftgogo.domain.ai.dto.GameGuideAiPathfinderRequest;
import com.tftgogo.domain.ai.dto.GameGuideAiPathfinderResponse;
import com.tftgogo.domain.guide.entity.GuideAugment;
import com.tftgogo.domain.guide.entity.GuideChampion;
import com.tftgogo.domain.guide.entity.GuideItem;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.repository.GuideAugmentRepository;
import com.tftgogo.domain.guide.repository.GuideChampionRepository;
import com.tftgogo.domain.guide.repository.GuideItemRepository;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class GameGuideAiPathfinderService {

    private static final Logger logger = LogManager.getLogger(GameGuideAiPathfinderService.class);
    private static final int MAX_CANDIDATE_REFS = 20;
    private static final int MAX_SELECTED_REFS = 5;

    private final AiServerClient aiServerClient;
    private final GuideAugmentRepository guideAugmentRepository;
    private final GuideChampionRepository guideChampionRepository;
    private final GuideItemRepository guideItemRepository;
    private final GuideTraitRepository guideTraitRepository;
    private final ObjectMapper objectMapper;
    private final AiChatRateLimiter rateLimiter;

    public GameGuideAiPathfinderService(
            AiServerClient aiServerClient,
            GuideAugmentRepository guideAugmentRepository,
            GuideChampionRepository guideChampionRepository,
            GuideItemRepository guideItemRepository,
            GuideTraitRepository guideTraitRepository,
            ObjectMapper objectMapper,
            AiChatRateLimiter rateLimiter
    ) {
        this.aiServerClient = aiServerClient;
        this.guideAugmentRepository = guideAugmentRepository;
        this.guideChampionRepository = guideChampionRepository;
        this.guideItemRepository = guideItemRepository;
        this.guideTraitRepository = guideTraitRepository;
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
    }

    public GameGuideAiPathfinderResponse pathfind(Long userId, GameGuideAiPathfinderRequest request) {
        validate(request);
        enforceRateLimit(userId);
        validateGuideRefs(request);
        List<AiServerClient.GameGuideSelectedEntry> selectedEntries = selectedEntries(request);
        List<GameGuideAiPathfinderRequest.GuideRefDto> candidateRefs = effectiveCandidateRefs(request);
        Set<String> allowedRefKeys = allowedRefKeys(request, candidateRefs);

        try {
            GameGuideAiPathfinderResponse aiResponse = aiServerClient.pathfindGameGuide(
                    request,
                    selectedEntries,
                    candidateRefs
            );
            if (aiResponse != null) {
                return filterResponseRefs(aiResponse, allowedRefKeys);
            }
        } catch (RuntimeException e) {
            logger.warn("GameGuide AI 응답 생성 실패, fallback 사용: {}", e.getMessage());
        }

        String activeTab = request.getActiveTab().trim();
        String tabLabel = resolveTabLabel(activeTab);
        List<GameGuideAiPathfinderResponse.GuideRefDto> sourceRefs = toSourceRefs(request.getSelectedRefs());

        return GameGuideAiPathfinderResponse.of(
                "%s 가이드 질문".formatted(tabLabel),
                "GameGuide AI 응답을 일시적으로 생성하지 못해 현재 가이드 화면 기준의 기본 안내를 표시합니다.",
                List.of(
                        "질문 키워드를 현재 가이드 탭에서 먼저 검색해 관련 항목을 확인하세요.",
                        "시너지, 챔피언, 아이템, 증강체를 하나씩 연결해 운영 흐름을 좁히는 방식이 안전합니다."
                ),
                List.of(
                        "현재 선택한 가이드 항목과 관련 후보만 기준으로 안내합니다."
                ),
                List.of(),
                List.of(
                        GameGuideAiPathfinderResponse.PhasePlanDto.of(
                                "ANY",
                                "가이드 항목 확인",
                                "현재 탭에서 질문 키워드와 가장 가까운 가이드 항목을 먼저 확인합니다.",
                                List.of()
                        ),
                        GameGuideAiPathfinderResponse.PhasePlanDto.of(
                                "ANY",
                                "연관 정보 비교",
                                "연관된 시너지, 챔피언, 아이템을 순서대로 비교하며 다음 질문을 좁힙니다.",
                                List.of()
                        )
                ),
                List.of(),
                List.of(
                        "현재 단계에서는 실제 메타 수치나 승률을 판단하지 않습니다.",
                        "가이드 데이터에 없는 아이템, 증강체, 전적 정보는 단정하지 않습니다."
                ),
                sourceRefs,
                List.of(
                        "일시적인 오류로 GameGuide AI 기본 안내를 표시합니다.",
                        "질문: %s".formatted(request.getQuestion().trim())
                ),
                true
        );
    }

    private void enforceRateLimit(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!rateLimiter.tryAcquire(userId)) {
            logger.warn("GameGuide AI rate limit 초과: userId={}", userId);
            throw new BusinessException(ErrorCode.AI_CHAT_RATE_LIMIT);
        }
    }

    private void validate(GameGuideAiPathfinderRequest request) {
        if (request == null || !hasText(request.getQuestion())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (!hasText(request.getPatchVersion()) || !hasText(request.getActiveTab()) || !hasText(request.getMode())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (!"AUTO".equals(request.getMode().trim())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (request.getSelectedRefs() != null && request.getSelectedRefs().size() > MAX_SELECTED_REFS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (request.getCandidateRefs() != null && request.getCandidateRefs().size() > MAX_CANDIDATE_REFS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateGuideRefs(GameGuideAiPathfinderRequest request) {
        String patchVersion = request.getPatchVersion().trim();
        validateGuideRefs(request.getSelectedRefs(), patchVersion);
        validateGuideRefs(request.getCandidateRefs(), patchVersion);
    }

    private void validateGuideRefs(List<GameGuideAiPathfinderRequest.GuideRefDto> refs, String patchVersion) {
        if (refs == null || refs.isEmpty()) {
            return;
        }

        for (GameGuideAiPathfinderRequest.GuideRefDto ref : refs) {
            if (ref == null || !hasText(ref.getGuideType()) || !hasText(ref.getTargetKey())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            if (!existsGuideRef(ref.getGuideType().trim(), ref.getTargetKey().trim(), patchVersion)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
        }
    }

    private boolean existsGuideRef(String guideType, String targetKey, String patchVersion) {
        return switch (guideType) {
            case "TRAIT" -> guideTraitRepository.findByTraitKeyAndPatchVersion(targetKey, patchVersion).isPresent();
            case "ITEM" -> guideItemRepository.findByItemKeyAndPatchVersion(targetKey, patchVersion).isPresent();
            case "AUGMENT" -> guideAugmentRepository.findByAugmentKeyAndPatchVersion(targetKey, patchVersion).isPresent();
            case "CHAMPION" -> guideChampionRepository.findByChampionKeyAndPatchVersion(targetKey, patchVersion).isPresent();
            default -> false;
        };
    }

    private List<AiServerClient.GameGuideSelectedEntry> selectedEntries(GameGuideAiPathfinderRequest request) {
        List<GameGuideAiPathfinderRequest.GuideRefDto> selectedRefs = safeList(request.getSelectedRefs());
        if (selectedRefs.isEmpty()) {
            return List.of();
        }

        String patchVersion = request.getPatchVersion().trim();
        return selectedRefs.stream()
                .map(ref -> selectedEntry(ref, patchVersion))
                .toList();
    }

    private AiServerClient.GameGuideSelectedEntry selectedEntry(
            GameGuideAiPathfinderRequest.GuideRefDto ref,
            String patchVersion
    ) {
        String guideType = ref.getGuideType().trim();
        String targetKey = ref.getTargetKey().trim();

        return switch (guideType) {
            case "TRAIT" -> traitSelectedEntry(targetKey, patchVersion);
            case "ITEM" -> itemSelectedEntry(targetKey, patchVersion);
            case "AUGMENT" -> augmentSelectedEntry(targetKey, patchVersion);
            case "CHAMPION" -> championSelectedEntry(targetKey, patchVersion);
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        };
    }

    private AiServerClient.GameGuideSelectedEntry traitSelectedEntry(String targetKey, String patchVersion) {
        GuideTrait trait = guideTraitRepository.findByTraitKeyAndPatchVersion(targetKey, patchVersion)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", trait.getType());
        data.put("tone", trait.getTone());
        data.put("levels", readJsonValue("trait.levels", trait.getLevelsJson()));
        data.put("tierEffects", readJsonValue("trait.tierEffects", trait.getTierEffectsJson()));
        data.put("champions", readJsonValue("trait.champions", trait.getChampionsJson()));
        data.put("specialUnits", readJsonValue("trait.specialUnits", trait.getSpecialUnitsJson()));
        data.put("tips", readJsonValue("trait.tips", trait.getTipsJson()));

        return new AiServerClient.GameGuideSelectedEntry(
                "TRAIT",
                trait.getTraitKey(),
                trait.getName(),
                trait.getSummary(),
                data
        );
    }

    private AiServerClient.GameGuideSelectedEntry itemSelectedEntry(String targetKey, String patchVersion) {
        GuideItem item = guideItemRepository.findByItemKeyAndPatchVersion(targetKey, patchVersion)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("category", item.getCategory());
        data.put("stats", readJsonValue("item.stats", item.getStatsJson()));
        data.put("bestUsers", readJsonValue("item.bestUsers", item.getBestUsersJson()));
        data.put("combinations", readJsonValue("item.combinations", item.getCombinationsJson()));

        return new AiServerClient.GameGuideSelectedEntry(
                "ITEM",
                item.getItemKey(),
                item.getName(),
                item.getDescription(),
                data
        );
    }

    private AiServerClient.GameGuideSelectedEntry augmentSelectedEntry(String targetKey, String patchVersion) {
        GuideAugment augment = guideAugmentRepository.findByAugmentKeyAndPatchVersion(targetKey, patchVersion)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tags", readJsonValue("augment.tags", augment.getTagsJson()));
        data.put("stats", readJsonValue("augment.stats", augment.getStatsJson()));

        return new AiServerClient.GameGuideSelectedEntry(
                "AUGMENT",
                augment.getAugmentKey(),
                augment.getName(),
                augment.getDescription(),
                data
        );
    }

    private AiServerClient.GameGuideSelectedEntry championSelectedEntry(String targetKey, String patchVersion) {
        GuideChampion champion = guideChampionRepository.findByChampionKeyAndPatchVersion(targetKey, patchVersion)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cost", champion.getCost());
        data.put("role", champion.getRole());
        data.put("position", champion.getPosition());
        data.put("stats", readJsonValue("champion.stats", champion.getStatsJson()));
        data.put("traits", readJsonValue("champion.traits", champion.getTraitsJson()));
        data.put("bestItems", readJsonValue("champion.bestItems", champion.getBestItemsJson()));

        return new AiServerClient.GameGuideSelectedEntry(
                "CHAMPION",
                champion.getChampionKey(),
                champion.getName(),
                championSummary(champion),
                data
        );
    }

    private Object readJsonValue(String fieldName, String json) {
        if (!hasText(json)) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            logger.warn("GameGuide AI 선택 항목 JSON 파싱 실패 field={}: {}", fieldName, e.getMessage());
            return json;
        }
    }

    private List<GameGuideAiPathfinderRequest.GuideRefDto> effectiveCandidateRefs(
            GameGuideAiPathfinderRequest request
    ) {
        List<GameGuideAiPathfinderRequest.GuideRefDto> selectedRefs = safeList(request.getSelectedRefs());
        List<GameGuideAiPathfinderRequest.GuideRefDto> requestCandidateRefs = safeList(request.getCandidateRefs());
        if (selectedRefs.isEmpty()) {
            return requestCandidateRefs;
        }

        String patchVersion = request.getPatchVersion().trim();
        CandidateRefLookup candidateRefLookup = new CandidateRefLookup(patchVersion);
        Set<String> selectedRefKeys = new HashSet<>();
        selectedRefs.forEach(ref -> selectedRefKeys.add(refKey(ref.getGuideType(), ref.getTargetKey())));

        Map<String, GameGuideAiPathfinderRequest.GuideRefDto> mergedRefs = new LinkedHashMap<>();
        for (GameGuideAiPathfinderRequest.GuideRefDto selectedRef : selectedRefs) {
            addCandidateRefs(
                    mergedRefs,
                    deriveCandidateRefs(selectedRef, candidateRefLookup),
                    selectedRefKeys
            );
            if (mergedRefs.size() >= MAX_CANDIDATE_REFS) {
                break;
            }
        }
        addCandidateRefs(mergedRefs, requestCandidateRefs, selectedRefKeys);
        return List.copyOf(mergedRefs.values());
    }

    private void addCandidateRefs(
            Map<String, GameGuideAiPathfinderRequest.GuideRefDto> mergedRefs,
            List<GameGuideAiPathfinderRequest.GuideRefDto> refs,
            Set<String> excludedRefKeys
    ) {
        for (GameGuideAiPathfinderRequest.GuideRefDto ref : safeList(refs)) {
            if (mergedRefs.size() >= MAX_CANDIDATE_REFS) {
                return;
            }
            String refKey = refKey(ref.getGuideType(), ref.getTargetKey());
            if (excludedRefKeys.contains(refKey)) {
                continue;
            }
            mergedRefs.putIfAbsent(refKey, ref);
        }
    }

    private List<GameGuideAiPathfinderRequest.GuideRefDto> deriveCandidateRefs(
            GameGuideAiPathfinderRequest.GuideRefDto selectedRef,
            CandidateRefLookup candidateRefLookup
    ) {
        String guideType = selectedRef.getGuideType().trim();
        String targetKey = selectedRef.getTargetKey().trim();

        return switch (guideType) {
            case "TRAIT" -> traitCandidateRefs(targetKey, candidateRefLookup);
            case "ITEM" -> itemCandidateRefs(targetKey, candidateRefLookup);
            case "CHAMPION" -> championCandidateRefs(targetKey, candidateRefLookup);
            case "AUGMENT" -> List.of();
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        };
    }

    private List<GameGuideAiPathfinderRequest.GuideRefDto> traitCandidateRefs(
            String targetKey,
            CandidateRefLookup candidateRefLookup
    ) {
        GuideTrait trait = guideTraitRepository.findByTraitKeyAndPatchVersion(
                        targetKey,
                        candidateRefLookup.patchVersion
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
        return championRefsFromRelatedRefs(
                readRelatedRefs("trait.champions", trait.getChampionsJson()),
                candidateRefLookup
        );
    }

    private List<GameGuideAiPathfinderRequest.GuideRefDto> itemCandidateRefs(
            String targetKey,
            CandidateRefLookup candidateRefLookup
    ) {
        GuideItem item = guideItemRepository.findByItemKeyAndPatchVersion(
                        targetKey,
                        candidateRefLookup.patchVersion
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));

        List<GameGuideAiPathfinderRequest.GuideRefDto> refs = new ArrayList<>();
        refs.addAll(championRefsFromRelatedRefs(
                readRelatedRefs("item.bestUsers", item.getBestUsersJson()),
                candidateRefLookup
        ));
        refs.addAll(itemRefsFromRelatedRefs(
                readCombinationItemRefs("item.combinations", item.getCombinationsJson()),
                candidateRefLookup
        ));
        return refs;
    }

    private List<GameGuideAiPathfinderRequest.GuideRefDto> championCandidateRefs(
            String targetKey,
            CandidateRefLookup candidateRefLookup
    ) {
        GuideChampion champion = guideChampionRepository.findByChampionKeyAndPatchVersion(
                        targetKey,
                        candidateRefLookup.patchVersion
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));

        List<GameGuideAiPathfinderRequest.GuideRefDto> refs = new ArrayList<>();
        refs.addAll(traitRefsFromRelatedRefs(
                readRelatedRefs("champion.traits", champion.getTraitsJson()),
                candidateRefLookup
        ));
        refs.addAll(itemRefsFromRelatedRefs(
                readRelatedRefs("champion.bestItems", champion.getBestItemsJson()),
                candidateRefLookup
        ));
        return refs;
    }

    private List<GameGuideAiPathfinderRequest.GuideRefDto> championRefsFromRelatedRefs(
            List<RelatedGuideRef> relatedRefs,
            CandidateRefLookup candidateRefLookup
    ) {
        if (relatedRefs.isEmpty()) {
            return List.of();
        }

        List<GameGuideAiPathfinderRequest.GuideRefDto> refs = new ArrayList<>();
        Set<String> addedTargetKeys = new HashSet<>();
        for (RelatedGuideRef relatedRef : relatedRefs) {
            Optional<GuideChampion> champion = candidateRefLookup.findChampion(relatedRef);
            if (champion.isEmpty()) {
                continue;
            }
            GuideChampion matchedChampion = champion.get();
            if (addedTargetKeys.add(matchedChampion.getChampionKey())) {
                refs.add(new GameGuideAiPathfinderRequest.GuideRefDto(
                        "CHAMPION",
                        matchedChampion.getChampionKey(),
                        matchedChampion.getName()
                ));
            }
        }
        return refs;
    }

    private List<GameGuideAiPathfinderRequest.GuideRefDto> traitRefsFromRelatedRefs(
            List<RelatedGuideRef> relatedRefs,
            CandidateRefLookup candidateRefLookup
    ) {
        if (relatedRefs.isEmpty()) {
            return List.of();
        }

        List<GameGuideAiPathfinderRequest.GuideRefDto> refs = new ArrayList<>();
        Set<String> addedTargetKeys = new HashSet<>();
        for (RelatedGuideRef relatedRef : relatedRefs) {
            Optional<GuideTrait> trait = candidateRefLookup.findTrait(relatedRef);
            if (trait.isEmpty()) {
                continue;
            }
            GuideTrait matchedTrait = trait.get();
            if (addedTargetKeys.add(matchedTrait.getTraitKey())) {
                refs.add(new GameGuideAiPathfinderRequest.GuideRefDto(
                        "TRAIT",
                        matchedTrait.getTraitKey(),
                        matchedTrait.getName()
                ));
            }
        }
        return refs;
    }

    private List<GameGuideAiPathfinderRequest.GuideRefDto> itemRefsFromRelatedRefs(
            List<RelatedGuideRef> relatedRefs,
            CandidateRefLookup candidateRefLookup
    ) {
        if (relatedRefs.isEmpty()) {
            return List.of();
        }

        List<GameGuideAiPathfinderRequest.GuideRefDto> refs = new ArrayList<>();
        Set<String> addedTargetKeys = new HashSet<>();
        for (RelatedGuideRef relatedRef : relatedRefs) {
            Optional<GuideItem> item = candidateRefLookup.findItem(relatedRef);
            if (item.isEmpty()) {
                continue;
            }
            GuideItem matchedItem = item.get();
            if (addedTargetKeys.add(matchedItem.getItemKey())) {
                refs.add(new GameGuideAiPathfinderRequest.GuideRefDto(
                        "ITEM",
                        matchedItem.getItemKey(),
                        matchedItem.getName()
                ));
            }
        }
        return refs;
    }

    private final class CandidateRefLookup {
        private final String patchVersion;
        private boolean championsLoaded;
        private boolean traitsLoaded;
        private boolean itemsLoaded;
        private Map<String, GuideChampion> championsByKey = Map.of();
        private Map<String, GuideChampion> championsByName = Map.of();
        private Map<String, GuideTrait> traitsByKey = Map.of();
        private Map<String, GuideTrait> traitsByName = Map.of();
        private Map<String, GuideItem> itemsByKey = Map.of();
        private Map<String, GuideItem> itemsByName = Map.of();

        private CandidateRefLookup(String patchVersion) {
            this.patchVersion = patchVersion;
        }

        private Optional<GuideChampion> findChampion(RelatedGuideRef relatedRef) {
            loadChampions();
            return firstPresent(
                    championsByKey.get(normalizeLookupValue(relatedRef.targetKey())),
                    championsByName.get(normalizeLookupValue(relatedRef.name())),
                    championsByName.get(normalizeLookupValue(relatedRef.targetKey()))
            );
        }

        private Optional<GuideTrait> findTrait(RelatedGuideRef relatedRef) {
            loadTraits();
            return firstPresent(
                    traitsByKey.get(normalizeLookupValue(relatedRef.targetKey())),
                    traitsByName.get(normalizeLookupValue(relatedRef.name())),
                    traitsByName.get(normalizeLookupValue(relatedRef.targetKey()))
            );
        }

        private Optional<GuideItem> findItem(RelatedGuideRef relatedRef) {
            loadItems();
            return firstPresent(
                    itemsByKey.get(normalizeLookupValue(relatedRef.targetKey())),
                    itemsByName.get(normalizeLookupValue(relatedRef.name())),
                    itemsByName.get(normalizeLookupValue(relatedRef.targetKey()))
            );
        }

        private void loadChampions() {
            if (championsLoaded) {
                return;
            }

            championsLoaded = true;
            Map<String, GuideChampion> nextByKey = new LinkedHashMap<>();
            Map<String, GuideChampion> nextByName = new LinkedHashMap<>();
            for (GuideChampion champion : guideChampionRepository.findByPatchVersionOrderByNameAscIdAsc(patchVersion)) {
                putNormalized(nextByKey, champion.getChampionKey(), champion);
                putNormalized(nextByName, champion.getName(), champion);
            }
            championsByKey = nextByKey;
            championsByName = nextByName;
        }

        private void loadTraits() {
            if (traitsLoaded) {
                return;
            }

            traitsLoaded = true;
            Map<String, GuideTrait> nextByKey = new LinkedHashMap<>();
            Map<String, GuideTrait> nextByName = new LinkedHashMap<>();
            for (GuideTrait trait : guideTraitRepository.findByPatchVersionOrderByNameAscIdAsc(patchVersion)) {
                putNormalized(nextByKey, trait.getTraitKey(), trait);
                putNormalized(nextByName, trait.getName(), trait);
            }
            traitsByKey = nextByKey;
            traitsByName = nextByName;
        }

        private void loadItems() {
            if (itemsLoaded) {
                return;
            }

            itemsLoaded = true;
            Map<String, GuideItem> nextByKey = new LinkedHashMap<>();
            Map<String, GuideItem> nextByName = new LinkedHashMap<>();
            for (GuideItem item : guideItemRepository.findByPatchVersionOrderByNameAscIdAsc(patchVersion)) {
                putNormalized(nextByKey, item.getItemKey(), item);
                putNormalized(nextByName, item.getName(), item);
            }
            itemsByKey = nextByKey;
            itemsByName = nextByName;
        }
    }

    private <T> Optional<T> firstPresent(T first, T second, T third) {
        if (first != null) {
            return Optional.of(first);
        }
        if (second != null) {
            return Optional.of(second);
        }
        return Optional.ofNullable(third);
    }

    private <T> void putNormalized(Map<String, T> index, String key, T value) {
        String normalizedKey = normalizeLookupValue(key);
        if (hasText(normalizedKey)) {
            index.putIfAbsent(normalizedKey, value);
        }
    }

    private String normalizeLookupValue(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    private List<RelatedGuideRef> readRelatedRefs(String fieldName, String json) {
        JsonNode root = readJsonNode(fieldName, json);
        if (!root.isArray()) {
            return List.of();
        }

        List<RelatedGuideRef> refs = new ArrayList<>();
        root.forEach(node -> addRelatedRef(refs, node));
        return refs;
    }

    private List<RelatedGuideRef> readCombinationItemRefs(String fieldName, String json) {
        JsonNode root = readJsonNode(fieldName, json);
        if (!root.isArray()) {
            return List.of();
        }

        List<RelatedGuideRef> refs = new ArrayList<>();
        for (JsonNode combination : root) {
            JsonNode itemsNode = combination.path("items");
            if (!itemsNode.isArray()) {
                continue;
            }
            itemsNode.forEach(node -> addRelatedRef(refs, node));
        }
        return refs;
    }

    private JsonNode readJsonNode(String fieldName, String json) {
        if (!hasText(json)) {
            return objectMapper.createArrayNode();
        }

        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            logger.warn("GameGuide AI 후보 ref JSON 파싱 실패 field={}: {}", fieldName, e.getMessage());
            return objectMapper.createArrayNode();
        }
    }

    private void addRelatedRef(List<RelatedGuideRef> refs, JsonNode node) {
        if (node.isTextual()) {
            String value = node.asText();
            if (hasText(value)) {
                refs.add(new RelatedGuideRef(value, value));
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        String targetKey = firstText(
                node,
                "targetKey",
                "target_key",
                "key",
                "apiName",
                "api_name",
                "championKey",
                "champion_key",
                "traitKey",
                "trait_key",
                "itemKey",
                "item_key"
        );
        String name = firstText(node, "name", "label");
        if (hasText(targetKey) || hasText(name)) {
            refs.add(new RelatedGuideRef(targetKey, name));
        }
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = node.path(fieldName).asText("");
            if (hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String championSummary(GuideChampion champion) {
        String summary = "%d코스트 %s %s".formatted(
                champion.getCost(),
                nullToBlank(champion.getRole()),
                nullToBlank(champion.getPosition())
        ).trim();
        return summary.replaceAll("\\s+", " ");
    }

    private Set<String> allowedRefKeys(
            GameGuideAiPathfinderRequest request,
            List<GameGuideAiPathfinderRequest.GuideRefDto> candidateRefs
    ) {
        Set<String> keys = new HashSet<>();
        addAllowedRefKeys(keys, request.getSelectedRefs());
        addAllowedRefKeys(keys, candidateRefs);
        return keys;
    }

    private void addAllowedRefKeys(Set<String> keys, List<GameGuideAiPathfinderRequest.GuideRefDto> refs) {
        if (refs == null || refs.isEmpty()) {
            return;
        }

        refs.forEach(ref -> keys.add(refKey(ref.getGuideType(), ref.getTargetKey())));
    }

    private GameGuideAiPathfinderResponse filterResponseRefs(
            GameGuideAiPathfinderResponse response,
            Set<String> allowedRefKeys
    ) {
        return GameGuideAiPathfinderResponse.of(
                response.getTitle(),
                response.getSummary(),
                safeList(response.getCoreConcepts()),
                safeList(response.getEvidenceNotes()),
                safeList(response.getCreativeSuggestions()),
                safeList(response.getPhasePlan()).stream()
                        .map(phase -> GameGuideAiPathfinderResponse.PhasePlanDto.of(
                                phase.getPhase(),
                                phase.getTitle(),
                                phase.getDescription(),
                                filterGuideRefs(phase.getGuideRefs(), allowedRefKeys)
                        ))
                        .toList(),
                safeList(response.getRecommendedRefs()).stream()
                        .filter(ref -> allowedRefKeys.contains(refKey(ref.getGuideType(), ref.getTargetKey())))
                        .toList(),
                safeList(response.getAvoidMistakes()),
                filterGuideRefs(response.getSourceRefs(), allowedRefKeys),
                safeList(response.getLimitations()),
                response.isFallback()
        );
    }

    private List<GameGuideAiPathfinderResponse.GuideRefDto> filterGuideRefs(
            List<GameGuideAiPathfinderResponse.GuideRefDto> refs,
            Set<String> allowedRefKeys
    ) {
        return safeList(refs).stream()
                .filter(ref -> allowedRefKeys.contains(refKey(ref.getGuideType(), ref.getTargetKey())))
                .toList();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String refKey(String guideType, String targetKey) {
        return "%s:%s".formatted(
                guideType == null ? "" : guideType.trim(),
                targetKey == null ? "" : targetKey.trim()
        );
    }

    private List<GameGuideAiPathfinderResponse.GuideRefDto> toSourceRefs(
            List<GameGuideAiPathfinderRequest.GuideRefDto> selectedRefs
    ) {
        if (selectedRefs == null || selectedRefs.isEmpty()) {
            return List.of();
        }

        return selectedRefs.stream()
                .map(ref -> GameGuideAiPathfinderResponse.GuideRefDto.of(
                        ref.getGuideType(),
                        ref.getTargetKey(),
                        ref.getName()
                ))
                .toList();
    }

    private String resolveTabLabel(String activeTab) {
        return switch (activeTab) {
            case "traits" -> "시너지";
            case "items" -> "아이템";
            case "augments" -> "증강체";
            case "champions" -> "챔피언";
            default -> "게임가이드";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private record RelatedGuideRef(String targetKey, String name) {
    }
}
