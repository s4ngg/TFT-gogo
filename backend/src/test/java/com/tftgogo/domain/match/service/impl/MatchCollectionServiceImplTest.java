package com.tftgogo.domain.match.service.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.match.entity.CachedMatch;
import com.tftgogo.domain.match.repository.CachedMatchRepository;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItemDto;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.RiotApiClient;
import com.tftgogo.global.riot.config.RiotProperties;
import com.tftgogo.global.riot.dto.MatchDto;
import com.tftgogo.global.riot.queue.RiotQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchCollectionServiceImplTest {

    @Mock private RiotApiClient riotApiClient;
    @Mock private RiotQueue riotQueue;
    @Mock private CachedMatchRepository cachedMatchRepository;
    @Mock private Executor matchCollectionExecutor;
    private MatchCollectionServiceImpl matchCollectionService;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);

    @BeforeEach
    void setUp() {
        RiotProperties props = new RiotProperties();
        props.setApiKey("test-key");
        matchCollectionService = new MatchCollectionServiceImpl(
                riotApiClient, riotQueue, cachedMatchRepository,
                matchCollectionExecutor, props);
    }

    @Test
    void DB에_충분한_데이터가_있으면_Riot_API를_호출하지_않는다() {
        // given
        String puuid = "test-puuid";
        when(cachedMatchRepository.findByParticipantPuuid(eq(puuid), any(Pageable.class)))
                .thenReturn(List.of(cachedMatch("m0", puuid), cachedMatch("m1", puuid)));

        // when
        matchCollectionService.fetchAndCache(puuid, 0, 2, Function.identity(), Function.identity(), s -> null);

        // then
        verify(riotQueue, never()).submit(any());
        verify(riotQueue, never()).submitForeground(any());
    }

    @Test
    void matchId_목록이_비어있으면_빈_리스트를_반환한다() {
        // given
        String puuid = "test-puuid";
        when(cachedMatchRepository.findByParticipantPuuid(eq(puuid), any(Pageable.class)))
                .thenReturn(List.of());
        // fetchMatchIds: ranked(1100) + normal(1090) 각 1회 → submitForeground 2회
        doReturn(CompletableFuture.completedFuture(List.of()))
                .doReturn(CompletableFuture.completedFuture(List.of()))
                .when(riotQueue).submitForeground(any());

        // when
        List<SummonerMatchItemDto> result = matchCollectionService.fetchAndCache(puuid, 0, 2,
                Function.identity(), Function.identity(), s -> null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void DB에_없는_matchId는_수집을_트리거하고_캐시_기반으로_결과를_빌드한다() {
        // given
        String puuid = "test-puuid";
        MatchDto fetchedDto = matchDto(1100, puuid);

        when(cachedMatchRepository.findByParticipantPuuid(eq(puuid), any(Pageable.class)))
                .thenReturn(List.of());
        // fetchMatchIds: ranked → ["m1"], normal → [] (submitForeground 2회)
        doReturn(CompletableFuture.completedFuture(List.of("m1")))
                .doReturn(CompletableFuture.completedFuture(List.of()))
                .when(riotQueue).submitForeground(any());
        // collectInBackground: getMatch("m1") (submit with dedupKey)
        doReturn(CompletableFuture.completedFuture(fetchedDto))
                .when(riotQueue).submit(anyString(), any());
        when(cachedMatchRepository.findMatchIdsByMatchIdIn(any())).thenReturn(List.of());
        doAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; })
                .when(matchCollectionExecutor).execute(any());
        when(cachedMatchRepository.existsById("m1")).thenReturn(false);
        when(cachedMatchRepository.findAllById(any())).thenReturn(List.of());

        // when
        List<SummonerMatchItemDto> result = matchCollectionService.fetchAndCache(puuid, 0, 2,
                Function.identity(), Function.identity(), s -> null);

        // then
        verify(riotQueue, times(2)).submitForeground(any()); // matchId 조회 (foreground)
        verify(riotQueue, times(1)).submit(anyString(), any()); // 매치 상세 수집 (background, dedupKey)
        verify(cachedMatchRepository).save(any(CachedMatch.class));
        assertThat(result).isEmpty(); // findAllById가 빈 목록 반환
    }

    @Test
    void Riot_API_실패시_fetchAndCache는_BusinessException을_전파한다() {
        // given
        String puuid = "test-puuid";
        when(cachedMatchRepository.findByParticipantPuuid(eq(puuid), any(Pageable.class)))
                .thenReturn(List.of());
        CompletableFuture<List<String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new BusinessException(ErrorCode.RIOT_API_ERROR));
        // fetchMatchIds는 submitForeground 사용
        doReturn(failed).when(riotQueue).submitForeground(any());

        // when / then
        assertThatThrownBy(() -> matchCollectionService.fetchAndCache(puuid, 0, 2,
                Function.identity(), Function.identity(), s -> null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RIOT_API_ERROR);
    }

    @Test
    void 매치_상세_수집_실패시_건너뛰고_나머지_결과를_빌드한다() {
        // given
        String puuid = "test-puuid";
        when(cachedMatchRepository.findByParticipantPuuid(eq(puuid), any(Pageable.class)))
                .thenReturn(List.of());
        when(cachedMatchRepository.findMatchIdsByMatchIdIn(any())).thenReturn(List.of());
        when(cachedMatchRepository.findAllById(any())).thenReturn(List.of());

        CompletableFuture<List<String>> matchIdFuture1 = CompletableFuture.completedFuture(List.of("m1"));
        CompletableFuture<List<String>> matchIdFuture2 = CompletableFuture.completedFuture(List.of());
        // fetchMatchIds: submitForeground 2회
        doReturn(matchIdFuture1).doReturn(matchIdFuture2)
                .when(riotQueue).submitForeground(any());

        // collectInBackground: 매치 상세 조회 실패 (submit with dedupKey)
        CompletableFuture<MatchDto> detailFailed = new CompletableFuture<>();
        detailFailed.completeExceptionally(new BusinessException(ErrorCode.RIOT_API_ERROR));
        doReturn(detailFailed).when(riotQueue).submit(anyString(), any());

        // when — 매치 상세 실패해도 예외 없이 반환
        List<SummonerMatchItemDto> result = matchCollectionService.fetchAndCache(puuid, 0, 2,
                Function.identity(), Function.identity(), s -> null);

        // then
        assertThat(result).isEmpty();
        verify(cachedMatchRepository, never()).save(any());
    }

    @Test
    void 타임아웃시_RIOT_API_TIMEOUT_예외가_발생한다() {
        // given — 짧은 timeout(1초)으로 서비스 재생성하여 실제 TimeoutException 경로 검증
        RiotProperties shortTimeoutProps = new RiotProperties();
        shortTimeoutProps.setApiKey("test-key");
        shortTimeoutProps.setMatchFetchTimeoutSeconds(1L);
        MatchCollectionServiceImpl shortTimeoutService = new MatchCollectionServiceImpl(
                riotApiClient, riotQueue, cachedMatchRepository,
                matchCollectionExecutor, shortTimeoutProps);

        String puuid = "test-puuid";
        when(cachedMatchRepository.findByParticipantPuuid(eq(puuid), any(Pageable.class)))
                .thenReturn(List.of());
        // 미완료 future → get(1, SECONDS)에서 실제 TimeoutException 발생
        doReturn(new CompletableFuture<>()).when(riotQueue).submitForeground(any());

        // when / then
        assertThatThrownBy(() -> shortTimeoutService.fetchAndCache(puuid, 0, 2,
                Function.identity(), Function.identity(), s -> null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RIOT_API_TIMEOUT);
    }

    @Test
    void 큐_포화시_RIOT_QUEUE_FULL_예외가_전파된다() {
        // given
        String puuid = "test-puuid";
        when(cachedMatchRepository.findByParticipantPuuid(eq(puuid), any(Pageable.class)))
                .thenReturn(List.of());
        CompletableFuture<List<String>> queueFullFuture = new CompletableFuture<>();
        queueFullFuture.completeExceptionally(new BusinessException(ErrorCode.RIOT_QUEUE_FULL));
        doReturn(queueFullFuture).when(riotQueue).submitForeground(any());

        // when / then
        assertThatThrownBy(() -> matchCollectionService.fetchAndCache(puuid, 0, 2,
                Function.identity(), Function.identity(), s -> null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RIOT_QUEUE_FULL);
    }

    private CachedMatch cachedMatch(String matchId, String puuid) {
        return CachedMatch.builder()
                .matchId(matchId)
                .queueId(1100)
                .gameDatetime(1000L)
                .matchJson(matchJsonWithPuuid(1100, puuid))
                .createdAt(LocalDateTime.now())
                .participantPuuids(Set.of(puuid))
                .build();
    }

    private String matchJsonWithPuuid(int queueId, String puuid) {
        return String.format(
                "{\"info\":{\"queue_id\":%d,\"game_datetime\":1000,\"game_length\":1800.0,"
                        + "\"game_version\":\"v14\",\"tft_set_number\":13,"
                        + "\"tft_set_core_name\":\"TFTSet13\",\"tft_game_type\":\"standard\","
                        + "\"participants\":[{\"puuid\":\"%s\",\"placement\":1}]}}",
                queueId, puuid);
    }

    private MatchDto matchDto(int queueId, String puuid) {
        try {
            return MAPPER.readValue(matchJsonWithPuuid(queueId, puuid), MatchDto.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
