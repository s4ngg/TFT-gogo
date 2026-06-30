package com.tftgogo.domain.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.ai.client.AiServerClient;
import com.tftgogo.domain.ai.dto.GameGuideAiPathfinderRequest;
import com.tftgogo.domain.ai.dto.GameGuideAiPathfinderResponse;
import com.tftgogo.domain.guide.entity.GuideChampion;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.repository.GuideAugmentRepository;
import com.tftgogo.domain.guide.repository.GuideChampionRepository;
import com.tftgogo.domain.guide.repository.GuideItemRepository;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameGuideAiPathfinderServiceTest {

    @Mock private AiServerClient aiServerClient;
    @Mock private GuideAugmentRepository guideAugmentRepository;
    @Mock private GuideChampionRepository guideChampionRepository;
    @Mock private GuideItemRepository guideItemRepository;
    @Mock private GuideTraitRepository guideTraitRepository;
    @Mock private AiChatRateLimiter rateLimiter;

    @InjectMocks
    private GameGuideAiPathfinderService service;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(rateLimiter.tryAcquire(USER_ID)).thenReturn(true);
        lenient().when(guideTraitRepository.findByTraitKeyAndPatchVersion(anyString(), anyString()))
                .thenReturn(Optional.of(mock(GuideTrait.class)));
    }

    private GameGuideAiPathfinderResponse pathfind(GameGuideAiPathfinderRequest request) {
        return service.pathfind(USER_ID, request);
    }

    @Test
    void 질문만_있는_요청이면_fallback_응답을_반환한다() {
        // given
        GameGuideAiPathfinderRequest request = new GameGuideAiPathfinderRequest(
                "17.3",
                "traits",
                "AUTO",
                List.of(),
                "동물특공대 어떻게 운영해?"
        );

        // when
        GameGuideAiPathfinderResponse response = pathfind(request);

        // then
        assertThat(response.isFallback()).isTrue();
        assertThat(response.getTitle()).isEqualTo("시너지 가이드 질문");
        assertThat(response.getPhasePlan()).hasSize(2);
        assertThat(response.getEvidenceNotes()).containsExactly("현재 선택한 가이드 항목과 화면 후보만 기준으로 안내합니다.");
        assertThat(response.getCreativeSuggestions()).isEmpty();
        assertThat(response.getSourceRefs()).isEmpty();
        assertThat(response.getLimitations()).contains("질문: 동물특공대 어떻게 운영해?");
    }

    @Test
    void 선택한_가이드_ref가_있으면_sourceRefs에_반영한다() {
        // given
        GameGuideAiPathfinderRequest.GuideRefDto ref = new GameGuideAiPathfinderRequest.GuideRefDto(
                "TRAIT",
                "TFT17_AnimalSquad",
                "동물특공대"
        );
        GameGuideAiPathfinderRequest request = new GameGuideAiPathfinderRequest(
                "17.3",
                "traits",
                "AUTO",
                List.of(ref),
                "이 시너지랑 같이 볼 챔피언 알려줘"
        );

        // when
        GameGuideAiPathfinderResponse response = pathfind(request);

        // then
        assertThat(response.getSourceRefs()).hasSize(1);
        assertThat(response.getSourceRefs().get(0).getGuideType()).isEqualTo("TRAIT");
        assertThat(response.getSourceRefs().get(0).getTargetKey()).isEqualTo("TFT17_AnimalSquad");
        assertThat(response.getSourceRefs().get(0).getName()).isEqualTo("동물특공대");
    }

    @Test
    void AUTO가_아닌_mode면_INVALID_INPUT_예외를_던진다() {
        // given
        GameGuideAiPathfinderRequest request = new GameGuideAiPathfinderRequest(
                "17.3",
                "traits",
                "OPERATE",
                List.of(),
                "운영 알려줘"
        );

        // when & then
        assertThatThrownBy(() -> pathfind(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 질문이_비어있으면_INVALID_INPUT_예외를_던진다() {
        // given
        GameGuideAiPathfinderRequest request = new GameGuideAiPathfinderRequest(
                "17.3",
                "traits",
                "AUTO",
                List.of(),
                "   "
        );

        // when & then
        assertThatThrownBy(() -> pathfind(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void fallback_여부는_isFallback_필드명으로_직렬화된다() throws JsonProcessingException {
        // given
        GameGuideAiPathfinderRequest request = new GameGuideAiPathfinderRequest(
                "17.3",
                "traits",
                "AUTO",
                List.of(),
                "운영 알려줘"
        );

        // when
        String json = objectMapper.writeValueAsString(pathfind(request));

        // then
        assertThat(json).contains("\"isFallback\":true");
        assertThat(json).doesNotContain("\"fallback\":");
    }

    @Test
    void AI_server_snake_case_응답은_frontend_camelCase로_직렬화된다() throws JsonProcessingException {
        // given
        String aiServerJson = """
                {
                  "title": "동물특공대 운영 루트",
                  "summary": "앞라인을 먼저 갖추세요.",
                  "core_concepts": ["앞라인 유지"],
                  "evidence_notes": ["선택한 시너지 설명에 앞라인 유지가 필요합니다."],
                  "creative_suggestions": ["상황에 따라 후반 보강을 시도해볼 수 있습니다."],
                  "phase_plan": [
                    {
                      "phase": "ANY",
                      "title": "핵심 확인",
                      "description": "선택한 시너지를 확인합니다.",
                      "guide_refs": [
                        {
                          "guide_type": "TRAIT",
                          "target_key": "TFT17_AnimalSquad",
                          "name": "동물특공대"
                        }
                      ]
                    }
                  ],
                  "recommended_refs": [
                    {
                      "guide_type": "TRAIT",
                      "target_key": "TFT17_AnimalSquad",
                      "name": "동물특공대",
                      "reason": "선택한 시너지입니다."
                    }
                  ],
                  "avoid_mistakes": ["승률을 단정하지 마세요."],
                  "source_refs": [
                    {
                      "guide_type": "TRAIT",
                      "target_key": "TFT17_AnimalSquad",
                      "name": "동물특공대"
                    }
                  ],
                  "limitations": ["정적 가이드 기준입니다."],
                  "is_fallback": false
                }
                """;

        // when
        GameGuideAiPathfinderResponse response = objectMapper.readValue(
                aiServerJson,
                GameGuideAiPathfinderResponse.class
        );
        String frontendJson = objectMapper.writeValueAsString(response);

        // then
        assertThat(response.getCoreConcepts()).containsExactly("앞라인 유지");
        assertThat(response.getEvidenceNotes()).containsExactly("선택한 시너지 설명에 앞라인 유지가 필요합니다.");
        assertThat(response.getCreativeSuggestions()).containsExactly("상황에 따라 후반 보강을 시도해볼 수 있습니다.");
        assertThat(response.getPhasePlan()).hasSize(1);
        assertThat(response.getPhasePlan().get(0).getGuideRefs().get(0).getGuideType()).isEqualTo("TRAIT");
        assertThat(response.getRecommendedRefs().get(0).getTargetKey()).isEqualTo("TFT17_AnimalSquad");
        assertThat(response.isFallback()).isFalse();
        assertThat(frontendJson).contains("\"coreConcepts\"");
        assertThat(frontendJson).contains("\"evidenceNotes\"");
        assertThat(frontendJson).contains("\"creativeSuggestions\"");
        assertThat(frontendJson).contains("\"phasePlan\"");
        assertThat(frontendJson).contains("\"recommendedRefs\"");
        assertThat(frontendJson).contains("\"sourceRefs\"");
        assertThat(frontendJson).contains("\"isFallback\":false");
        assertThat(frontendJson).doesNotContain("\"core_concepts\"");
        assertThat(frontendJson).doesNotContain("\"evidence_notes\"");
        assertThat(frontendJson).doesNotContain("\"creative_suggestions\"");
        assertThat(frontendJson).doesNotContain("\"is_fallback\"");
        assertThat(frontendJson).doesNotContain("\"fallback\":");
    }

    @Test
    void AI_server_response_is_returned_when_available() {
        // given
        GameGuideAiPathfinderRequest request = new GameGuideAiPathfinderRequest(
                "17.3",
                "traits",
                "AUTO",
                List.of(),
                "?숇Ъ?밴났? ?대뼸寃??댁쁺??"
        );
        GameGuideAiPathfinderResponse expected = GameGuideAiPathfinderResponse.of(
                "AI title",
                "AI summary",
                List.of("core"),
                List.of("evidence"),
                List.of("creative"),
                List.of(GameGuideAiPathfinderResponse.PhasePlanDto.of(
                        "ANY",
                        "plan",
                        "description",
                        List.of()
                )),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false
        );
        when(aiServerClient.pathfindGameGuide(any(), any())).thenReturn(expected);

        // when
        GameGuideAiPathfinderResponse response = pathfind(request);

        // then
        assertThat(response.getTitle()).isEqualTo(expected.getTitle());
        assertThat(response.getSummary()).isEqualTo(expected.getSummary());
        assertThat(response.getEvidenceNotes()).containsExactly("evidence");
        assertThat(response.getCreativeSuggestions()).containsExactly("creative");
        assertThat(response.isFallback()).isFalse();
    }

    @Test
    void 선택한_가이드_ref는_DB_내용으로_AI_server에_전달된다() {
        // given
        GuideTrait trait = GuideTrait.builder()
                .traitKey("TFT17_AnimalSquad")
                .name("동물특공대")
                .type("origin")
                .iconUrl("icon.png")
                .tone("tempo")
                .summary("체력 보너스와 지속 전투에 강한 시너지")
                .levelsJson("[{\"level\":3,\"effect\":\"체력 증가\"}]")
                .tierEffectsJson("[{\"tier\":\"bronze\",\"effect\":\"기본 효과\"}]")
                .championsJson("[{\"key\":\"TFT17_Sylas\",\"name\":\"사일러스\"}]")
                .specialUnitsJson("[]")
                .tipsJson("[\"초반 전열을 안정적으로 확보한다\"]")
                .patchVersion("17.3")
                .build();
        when(guideTraitRepository.findByTraitKeyAndPatchVersion("TFT17_AnimalSquad", "17.3"))
                .thenReturn(Optional.of(trait));
        GameGuideAiPathfinderRequest.GuideRefDto ref = new GameGuideAiPathfinderRequest.GuideRefDto(
                "TRAIT",
                "TFT17_AnimalSquad",
                "프론트 표시명"
        );
        GameGuideAiPathfinderRequest request = new GameGuideAiPathfinderRequest(
                "17.3",
                "traits",
                "AUTO",
                List.of(ref),
                "동물특공대 운영 알려줘"
        );
        GameGuideAiPathfinderResponse aiResponse = GameGuideAiPathfinderResponse.of(
                "AI title",
                "AI summary",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false
        );
        when(aiServerClient.pathfindGameGuide(any(), any())).thenReturn(aiResponse);

        // when
        pathfind(request);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiServerClient.GameGuideSelectedEntry>> selectedEntriesCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(aiServerClient).pathfindGameGuide(any(), selectedEntriesCaptor.capture());

        List<AiServerClient.GameGuideSelectedEntry> selectedEntries = selectedEntriesCaptor.getValue();
        assertThat(selectedEntries).hasSize(1);
        AiServerClient.GameGuideSelectedEntry selectedEntry = selectedEntries.get(0);
        assertThat(selectedEntry.guideType()).isEqualTo("TRAIT");
        assertThat(selectedEntry.targetKey()).isEqualTo("TFT17_AnimalSquad");
        assertThat(selectedEntry.name()).isEqualTo("동물특공대");
        assertThat(selectedEntry.summary()).isEqualTo("체력 보너스와 지속 전투에 강한 시너지");
        assertThat(selectedEntry.data()).containsEntry("type", "origin");
        assertThat(selectedEntry.data()).containsEntry("tone", "tempo");
        assertThat(selectedEntry.data().get("levels")).isInstanceOf(List.class);
        assertThat((List<?>) selectedEntry.data().get("levels")).hasSize(1);
    }

    @Test
    void candidateRefs_20개_초과하면_INVALID_INPUT_예외를_던진다() {
        // given
        List<GameGuideAiPathfinderRequest.GuideRefDto> candidateRefs = IntStream.rangeClosed(1, 21)
                .mapToObj(index -> new GameGuideAiPathfinderRequest.GuideRefDto(
                        "CHAMPION",
                        "CHAMPION:%d".formatted(index),
                        "champion-%d".formatted(index)
                ))
                .toList();
        GameGuideAiPathfinderRequest request = new GameGuideAiPathfinderRequest(
                "17.3",
                "traits",
                "AUTO",
                List.of(),
                candidateRefs,
                "후보가 너무 많으면 거절해야 한다"
        );

        // when & then
        assertThatThrownBy(() -> pathfind(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void AI_response_refs_not_in_candidateRefs_are_removed() {
        // given
        GameGuideAiPathfinderRequest.GuideRefDto allowedRef = new GameGuideAiPathfinderRequest.GuideRefDto(
                "CHAMPION",
                "TFT17_AllowedChampion",
                "allowed"
        );
        GameGuideAiPathfinderRequest request = new GameGuideAiPathfinderRequest(
                "17.3",
                "traits",
                "AUTO",
                List.of(),
                List.of(allowedRef),
                "allowed champion only"
        );
        GameGuideAiPathfinderResponse.GuideRefDto allowedGuideRef = GameGuideAiPathfinderResponse.GuideRefDto.of(
                "CHAMPION",
                "TFT17_AllowedChampion",
                "allowed"
        );
        GameGuideAiPathfinderResponse.GuideRefDto blockedGuideRef = GameGuideAiPathfinderResponse.GuideRefDto.of(
                "CHAMPION",
                "TFT17_BlockedChampion",
                "blocked"
        );
        GameGuideAiPathfinderResponse aiResponse = GameGuideAiPathfinderResponse.of(
                "AI title",
                "AI summary",
                List.of(),
                List.of("evidence"),
                List.of("creative"),
                List.of(GameGuideAiPathfinderResponse.PhasePlanDto.of(
                        "ANY",
                        "plan",
                        "description",
                        List.of(allowedGuideRef, blockedGuideRef)
                )),
                List.of(
                        GameGuideAiPathfinderResponse.RecommendedRefDto.of(
                                "CHAMPION",
                                "TFT17_AllowedChampion",
                                "allowed",
                                "ok"
                        ),
                        GameGuideAiPathfinderResponse.RecommendedRefDto.of(
                                "CHAMPION",
                                "TFT17_BlockedChampion",
                                "blocked",
                                "no"
                        )
                ),
                List.of(),
                List.of(allowedGuideRef, blockedGuideRef),
                List.of(),
                false
        );
        when(guideChampionRepository.findByChampionKeyAndPatchVersion("TFT17_AllowedChampion", "17.3"))
                .thenReturn(Optional.of(mock(GuideChampion.class)));
        when(aiServerClient.pathfindGameGuide(any(), any())).thenReturn(aiResponse);

        // when
        GameGuideAiPathfinderResponse result = pathfind(request);

        // then
        assertThat(result.getRecommendedRefs()).hasSize(1);
        assertThat(result.getEvidenceNotes()).containsExactly("evidence");
        assertThat(result.getCreativeSuggestions()).containsExactly("creative");
        assertThat(result.getRecommendedRefs().get(0).getTargetKey()).isEqualTo("TFT17_AllowedChampion");
        assertThat(result.getPhasePlan().get(0).getGuideRefs()).hasSize(1);
        assertThat(result.getSourceRefs()).hasSize(1);
    }

    @Test
    void candidateRef_not_found_in_guide_db_throws_INVALID_INPUT() {
        // given
        GameGuideAiPathfinderRequest.GuideRefDto missingRef = new GameGuideAiPathfinderRequest.GuideRefDto(
                "CHAMPION",
                "TFT17_MissingChampion",
                "missing"
        );
        GameGuideAiPathfinderRequest request = new GameGuideAiPathfinderRequest(
                "17.3",
                "traits",
                "AUTO",
                List.of(),
                List.of(missingRef),
                "missing champion"
        );

        // when & then
        assertThatThrownBy(() -> pathfind(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void rate_limit_초과시_AI서버를_호출하지_않고_AI_CHAT_RATE_LIMIT_예외를_던진다() {
        // given
        GameGuideAiPathfinderRequest request = new GameGuideAiPathfinderRequest(
                "17.3",
                "traits",
                "AUTO",
                List.of(),
                "운영 알려줘"
        );
        when(rateLimiter.tryAcquire(USER_ID)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> pathfind(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AI_CHAT_RATE_LIMIT));
        verifyNoInteractions(aiServerClient);
    }

    @Test
    void 인증_userId가_없으면_UNAUTHORIZED_예외를_던진다() {
        // given
        GameGuideAiPathfinderRequest request = new GameGuideAiPathfinderRequest(
                "17.3",
                "traits",
                "AUTO",
                List.of(),
                "운영 알려줘"
        );

        // when & then
        assertThatThrownBy(() -> service.pathfind(null, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED));
        verifyNoInteractions(aiServerClient);
    }
}
