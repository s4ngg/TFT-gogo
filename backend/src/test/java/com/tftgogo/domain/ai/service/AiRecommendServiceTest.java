package com.tftgogo.domain.ai.service;

import com.tftgogo.domain.ai.client.AiServerClient;
import com.tftgogo.domain.ai.dto.response.AiRecommendResponse;
import com.tftgogo.domain.deck.dto.response.MetaDeckListResponse;
import com.tftgogo.domain.deck.entity.RankFilter;
import com.tftgogo.domain.deck.service.MetaDeckService;
import com.tftgogo.domain.guide.entity.GuideTrait;
import com.tftgogo.domain.guide.repository.GuideTraitRepository;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.domain.match.service.MatchCollectionService;
import com.tftgogo.domain.search.dto.response.SummonerProfileResponse;
import com.tftgogo.domain.search.service.SummonerService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRecommendServiceTest {

    @Mock private AiServerClient aiServerClient;
    @Mock private SummonerService summonerService;
    @Mock private MatchCollectionService matchCollectionService;
    @Mock private MetaDeckService metaDeckService;
    @Mock private GuideTraitRepository guideTraitRepository;

    @InjectMocks
    private AiRecommendService aiRecommendService;

    private static final String GAME_NAME = "TestUser";
    private static final String TAG_LINE = "KR1";
    private static final String PUUID = "test-puuid-1234";

    @Test
    void 소환사_조회_실패시_BusinessException을_던지고_AI서버_미호출() {
        // given
        when(summonerService.getProfile(GAME_NAME, TAG_LINE))
                .thenThrow(new BusinessException(ErrorCode.SUMMONER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> aiRecommendService.recommend(GAME_NAME, TAG_LINE))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUMMONER_NOT_FOUND);
        verify(aiServerClient, never()).analyzeWithMeta(any());
    }

    @Test
    void 랭크_전적이_없으면_null을_반환하고_AI서버_미호출() {
        // given
        SummonerProfileResponse profile = SummonerProfileResponse.builder()
                .puuid(PUUID).gameName(GAME_NAME).tagLine(TAG_LINE)
                .profileIconId(1).profileIconUrl("").summonerLevel(100)
                .build();
        when(summonerService.getProfile(GAME_NAME, TAG_LINE)).thenReturn(profile);
        when(matchCollectionService.getRankedMatchSummaries(eq(PUUID), anyInt()))
                .thenReturn(List.of());

        // when
        AiRecommendResponse result = aiRecommendService.recommend(GAME_NAME, TAG_LINE);

        // then
        assertThat(result).isNull();
        verify(aiServerClient, never()).analyzeWithMeta(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void 정상_요청시_AI서버_호출_및_matches_meta_decks_바디_구성() {
        // given
        SummonerProfileResponse profile = SummonerProfileResponse.builder()
                .puuid(PUUID).gameName(GAME_NAME).tagLine(TAG_LINE)
                .profileIconId(1).profileIconUrl("").summonerLevel(100)
                .build();
        MatchSummaryResponse match = MatchSummaryResponse.builder()
                .matchId("KR_1").placement(1).level(8).lastRound(28)
                .gameLength(1800f).goldLeft(2).playersEliminated(3)
                .timeEliminated(1800f).totalDamageToPlayers(100).gameVersion("14.1")
                .queueType("RANKED_TFT").gameDatetime(0L)
                .traits(List.of()).units(List.of()).participants(List.of())
                .build();
        MetaDeckListResponse metaDeckList = MetaDeckListResponse.builder()
                .decks(List.of())
                .build();
        AiRecommendResponse mockResponse = new AiRecommendResponse();

        when(summonerService.getProfile(GAME_NAME, TAG_LINE)).thenReturn(profile);
        when(matchCollectionService.getRankedMatchSummaries(eq(PUUID), anyInt()))
                .thenReturn(List.of(match));
        when(metaDeckService.getMetaDecks(RankFilter.MASTER_PLUS)).thenReturn(metaDeckList);
        when(aiServerClient.analyzeWithMeta(any())).thenReturn(mockResponse);

        // when
        AiRecommendResponse result = aiRecommendService.recommend(GAME_NAME, TAG_LINE);

        // then
        assertThat(result).isNotNull();
        verify(aiServerClient).analyzeWithMeta(any(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void AI서버_호출_바디에_matches와_meta_decks_키_포함() {
        // given
        SummonerProfileResponse profile = SummonerProfileResponse.builder()
                .puuid(PUUID).gameName(GAME_NAME).tagLine(TAG_LINE)
                .profileIconId(1).profileIconUrl("").summonerLevel(100)
                .build();
        MatchSummaryResponse match = MatchSummaryResponse.builder()
                .matchId("KR_1").placement(2).level(7).lastRound(24)
                .gameLength(1500f).goldLeft(0).playersEliminated(1)
                .timeEliminated(1500f).totalDamageToPlayers(50).gameVersion("14.1")
                .queueType("RANKED_TFT").gameDatetime(0L)
                .traits(List.of()).units(List.of()).participants(List.of())
                .build();
        when(summonerService.getProfile(GAME_NAME, TAG_LINE)).thenReturn(profile);
        when(matchCollectionService.getRankedMatchSummaries(eq(PUUID), anyInt()))
                .thenReturn(List.of(match));
        when(metaDeckService.getMetaDecks(RankFilter.MASTER_PLUS))
                .thenReturn(MetaDeckListResponse.builder().decks(List.of()).build());
        // analyzeWithMeta는 실제로 null을 반환하지 않는다(빈 응답이면 BusinessException).
        // 여기서는 바디 구성 검증이 목적이므로 정상 응답으로 stub한다.
        when(aiServerClient.analyzeWithMeta(any())).thenReturn(new AiRecommendResponse());

        // when
        aiRecommendService.recommend(GAME_NAME, TAG_LINE);

        // then: 바디에 matches, meta_decks 키가 포함되어 전달됐는지 검증
        org.mockito.ArgumentCaptor<Map<String, Object>> captor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(aiServerClient).analyzeWithMeta(captor.capture());
        Map<String, Object> body = captor.getValue();
        assertThat(body).containsKey("matches");
        assertThat(body).containsKey("meta_decks");
        assertThat(body).containsEntry("summoner_name", GAME_NAME);
        assertThat(body).containsEntry("tag_line", TAG_LINE);
    }

    @Test
    void 시너지_이름을_GuideTrait_기준_한글로_치환한다() {
        // given
        SummonerProfileResponse profile = SummonerProfileResponse.builder()
                .puuid(PUUID).gameName(GAME_NAME).tagLine(TAG_LINE)
                .profileIconId(1).profileIconUrl("").summonerLevel(100)
                .build();
        MatchSummaryResponse match = MatchSummaryResponse.builder()
                .matchId("KR_1").placement(1).level(8).lastRound(28)
                .gameLength(1800f).goldLeft(2).playersEliminated(3)
                .timeEliminated(1800f).totalDamageToPlayers(100).gameVersion("17.6")
                .queueType("RANKED_TFT").gameDatetime(0L)
                .traits(List.of()).units(List.of()).participants(List.of())
                .build();
        when(summonerService.getProfile(GAME_NAME, TAG_LINE)).thenReturn(profile);
        when(matchCollectionService.getRankedMatchSummaries(eq(PUUID), anyInt()))
                .thenReturn(List.of(match));
        when(metaDeckService.getMetaDecks(RankFilter.MASTER_PLUS))
                .thenReturn(MetaDeckListResponse.builder().decks(List.of()).build());

        AiRecommendResponse.TraitStat psyops = new AiRecommendResponse.TraitStat();
        psyops.setName("psyops");
        AiRecommendResponse.TraitStat hiddenTag = new AiRecommendResponse.TraitStat();
        hiddenTag.setName("resisttank");
        AiRecommendResponse aiResponse = new AiRecommendResponse();
        ReflectionTestUtils.setField(aiResponse, "goodTraits", List.of(psyops));
        ReflectionTestUtils.setField(aiResponse, "badTraits", List.of(hiddenTag));
        when(aiServerClient.analyzeWithMeta(any())).thenReturn(aiResponse);

        GuideTrait psyopsTrait = GuideTrait.builder()
                .traitKey("TFT17_Psyops").name("사이옵스").type("origin").iconUrl("icon.png")
                .tone("gold").summary("summary").levelsJson("[]").tierEffectsJson("[]")
                .championsJson("[]").specialUnitsJson("[]").tipsJson("[]").patchVersion("17.6")
                .build();
        when(guideTraitRepository.findLatestPatchVersion()).thenReturn(Optional.of("17.6"));
        when(guideTraitRepository.findByPatchVersionOrderByNameAscIdAsc("17.6"))
                .thenReturn(List.of(psyopsTrait));

        // when
        AiRecommendResponse result = aiRecommendService.recommend(GAME_NAME, TAG_LINE);

        // then: GuideTrait에 매칭되는 시너지는 한글로 치환
        assertThat(result.getGoodTraits().get(0).getName()).isEqualTo("사이옵스");
        // 매칭되는 GuideTrait가 없는 시너지(숨김/내부 태그 등)는 원래 suffix 유지
        assertThat(result.getBadTraits().get(0).getName()).isEqualTo("resisttank");
    }

    @Test
    void 현재_패치의_GuideTrait가_없으면_시너지_이름을_그대로_둔다() {
        // given
        SummonerProfileResponse profile = SummonerProfileResponse.builder()
                .puuid(PUUID).gameName(GAME_NAME).tagLine(TAG_LINE)
                .profileIconId(1).profileIconUrl("").summonerLevel(100)
                .build();
        MatchSummaryResponse match = MatchSummaryResponse.builder()
                .matchId("KR_1").placement(1).level(8).lastRound(28)
                .gameLength(1800f).goldLeft(2).playersEliminated(3)
                .timeEliminated(1800f).totalDamageToPlayers(100).gameVersion("17.6")
                .queueType("RANKED_TFT").gameDatetime(0L)
                .traits(List.of()).units(List.of()).participants(List.of())
                .build();
        when(summonerService.getProfile(GAME_NAME, TAG_LINE)).thenReturn(profile);
        when(matchCollectionService.getRankedMatchSummaries(eq(PUUID), anyInt()))
                .thenReturn(List.of(match));
        when(metaDeckService.getMetaDecks(RankFilter.MASTER_PLUS))
                .thenReturn(MetaDeckListResponse.builder().decks(List.of()).build());

        AiRecommendResponse.TraitStat psyops = new AiRecommendResponse.TraitStat();
        psyops.setName("psyops");
        AiRecommendResponse aiResponse = new AiRecommendResponse();
        ReflectionTestUtils.setField(aiResponse, "goodTraits", List.of(psyops));
        when(aiServerClient.analyzeWithMeta(any())).thenReturn(aiResponse);
        when(guideTraitRepository.findLatestPatchVersion()).thenReturn(Optional.empty());

        // when
        AiRecommendResponse result = aiRecommendService.recommend(GAME_NAME, TAG_LINE);

        // then
        assertThat(result.getGoodTraits().get(0).getName()).isEqualTo("psyops");
    }
}
