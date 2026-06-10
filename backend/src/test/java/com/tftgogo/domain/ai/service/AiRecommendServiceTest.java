package com.tftgogo.domain.ai.service;

import com.tftgogo.domain.ai.client.AiServerClient;
import com.tftgogo.domain.ai.dto.AiRecommendResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * AiRecommendService 단위 테스트.
 *
 * 현재 recommend()는 전적 API 연동 전 null을 반환하는 뼈대 구현이다.
 * 팀원 전적 API 완성 후 아래 TODO 테스트를 채운다.
 */
@ExtendWith(MockitoExtension.class)
class AiRecommendServiceTest {

    @Mock
    private AiServerClient aiServerClient;

    @InjectMocks
    private AiRecommendService aiRecommendService;

    @Test
    void 전적_API_미연동_상태에서_recommend는_null을_반환한다() {
        // given
        // 전적 API 미연동 상태 — recommend()는 null을 반환하는 뼈대 구현

        // when
        AiRecommendResponse result = aiRecommendService.recommend("TestUser", "KR1");

        // then
        assertThat(result).isNull();
    }

    @Test
    void 전적_API_미연동_상태에서_AI_서버_호출은_발생하지_않는다() {
        // given
        // 전적 API 미연동 상태 — AI 서버 호출 없이 null 반환

        // when
        aiRecommendService.recommend("TestUser", "KR1");

        // then
        verify(aiServerClient, never()).analyzeWithMeta(any());
    }

    // TODO (팀원 전적 API 완성 후 구현):

    // @Test
    // void 전적이_있으면_AI_서버에_with_meta_요청을_보낸다() {
    //     // given: summonerService.getPuuid() 반환값 stub
    //     // given: matchService.getRecentRankedMatches() 반환값 stub (20게임)
    //     // given: metaDeckService.getCurrentMetaDecks() 반환값 stub
    //     // given: aiServerClient.analyzeWithMeta() mock 응답 설정
    //
    //     // when
    //     AiRecommendResponse result = aiRecommendService.recommend("TestUser", "KR1");
    //
    //     // then: result가 null이 아님
    //     // then: aiServerClient.analyzeWithMeta() 1회 호출됨
    // }

    // @Test
    // void 매치가_없으면_AI_서버_호출_없이_null을_반환한다() {
    //     // given: matchService.getRecentRankedMatches() 빈 리스트 반환
    //
    //     // when
    //     AiRecommendResponse result = aiRecommendService.recommend("TestUser", "KR1");
    //
    //     // then: null
    //     // then: aiServerClient 미호출
    // }

    // @Test
    // void AI_서버_호출_실패_시_null을_반환한다() {
    //     // given: aiServerClient.analyzeWithMeta()가 null 반환 (타임아웃 시뮬레이션)
    //
    //     // when
    //     AiRecommendResponse result = aiRecommendService.recommend("TestUser", "KR1");
    //
    //     // then: null (컨트롤러에서 fallback 처리)
    // }
}
