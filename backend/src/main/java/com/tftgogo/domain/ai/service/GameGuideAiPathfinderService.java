package com.tftgogo.domain.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public GameGuideAiPathfinderService(
            AiServerClient aiServerClient,
            GuideAugmentRepository guideAugmentRepository,
            GuideChampionRepository guideChampionRepository,
            GuideItemRepository guideItemRepository,
            GuideTraitRepository guideTraitRepository,
            ObjectMapper objectMapper
    ) {
        this.aiServerClient = aiServerClient;
        this.guideAugmentRepository = guideAugmentRepository;
        this.guideChampionRepository = guideChampionRepository;
        this.guideItemRepository = guideItemRepository;
        this.guideTraitRepository = guideTraitRepository;
        this.objectMapper = objectMapper;
    }

    public GameGuideAiPathfinderResponse pathfind(GameGuideAiPathfinderRequest request) {
        validate(request);
        validateGuideRefs(request);
        Set<String> allowedRefKeys = allowedRefKeys(request);
        List<AiServerClient.GameGuideSelectedEntry> selectedEntries = selectedEntries(request);

        try {
            GameGuideAiPathfinderResponse aiResponse = aiServerClient.pathfindGameGuide(request, selectedEntries);
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
                "GameGuide AI 서버 연결 전이라 현재 가이드 화면 기준의 기본 안내를 표시합니다.",
                List.of(
                        "질문 키워드를 현재 가이드 탭에서 먼저 검색해 관련 항목을 확인하세요.",
                        "시너지, 챔피언, 아이템, 증강체를 하나씩 연결해 운영 흐름을 좁히는 방식이 안전합니다."
                ),
                List.of(
                        "현재 선택한 가이드 항목과 화면 후보만 기준으로 안내합니다."
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
                        "아직 GameGuide AI 서버가 연결되지 않아 기본 안내를 표시합니다.",
                        "질문: %s".formatted(request.getQuestion().trim())
                ),
                true
        );
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

    private String championSummary(GuideChampion champion) {
        String summary = "%d코스트 %s %s".formatted(
                champion.getCost(),
                nullToBlank(champion.getRole()),
                nullToBlank(champion.getPosition())
        ).trim();
        return summary.replaceAll("\\s+", " ");
    }

    private Set<String> allowedRefKeys(GameGuideAiPathfinderRequest request) {
        Set<String> keys = new HashSet<>();
        addAllowedRefKeys(keys, request.getSelectedRefs());
        addAllowedRefKeys(keys, request.getCandidateRefs());
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
}
