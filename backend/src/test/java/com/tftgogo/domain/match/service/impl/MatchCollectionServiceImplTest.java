package com.tftgogo.domain.match.service.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.match.dto.response.CollectionStatusResponse;
import com.tftgogo.domain.match.entity.CachedMatch;
import com.tftgogo.domain.match.repository.CachedMatchRepository;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItemDto;
import com.tftgogo.global.riot.RiotApiClient;
import com.tftgogo.global.riot.dto.MatchDto;
import com.tftgogo.global.riot.queue.RiotQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchCollectionServiceImplTest {

    @Mock private RiotApiClient riotApiClient;
    @Mock private RiotQueue riotQueue;
    @Mock private CachedMatchRepository cachedMatchRepository;
    @Mock private Executor matchCollectionExecutor;
    @InjectMocks private MatchCollectionServiceImpl matchCollectionService;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);

    @Test
    void DB에_충분한_데이터가_있으면_Riot_API를_호출하지_않는다() {
        // given
        String puuid = "test-puuid";
        when(cachedMatchRepository.findByParticipantPuuid(eq(puuid), any(Pageable.class)))
                .thenReturn(List.of(cachedMatch("m0", puuid), cachedMatch("m1", puuid)));

        // when
        matchCollectionService.fetchAndCache(puuid, 0, 2);

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
        List<SummonerMatchItemDto> result = matchCollectionService.fetchAndCache(puuid, 0, 2);

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
        // collectInBackground: getMatch("m1") (submit 1회)
        doReturn(CompletableFuture.completedFuture(fetchedDto))
                .when(riotQueue).submit(any());
        when(cachedMatchRepository.findMatchIdsByMatchIdIn(any())).thenReturn(List.of());
        doAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; })
                .when(matchCollectionExecutor).execute(any());
        when(cachedMatchRepository.existsById("m1")).thenReturn(false);
        when(cachedMatchRepository.findAllById(any())).thenReturn(List.of());

        // when
        List<SummonerMatchItemDto> result = matchCollectionService.fetchAndCache(puuid, 0, 2);

        // then
        verify(riotQueue, times(2)).submitForeground(any()); // matchId 조회 (foreground)
        verify(riotQueue, times(1)).submit(any());           // 매치 상세 수집 (background)
        verify(cachedMatchRepository).save(any(CachedMatch.class));
        assertThat(result).isEmpty(); // findAllById가 빈 목록 반환
    }

    @Test
    void getStatus는_collected_카운트와_inProgress_false를_반환한다() {
        // given
        String puuid = "test-puuid";
        when(cachedMatchRepository.countByParticipantPuuid(puuid)).thenReturn(7L);

        // when
        CollectionStatusResponse result = matchCollectionService.getStatus(puuid);

        // then
        assertThat(result.getCollected()).isEqualTo(7);
        assertThat(result.isInProgress()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getStatus는_수집_진행_중이면_inProgress_true를_반환한다() {
        // given
        String puuid = "test-puuid";
        ConcurrentHashMap<String, Boolean> inProgressMap =
                (ConcurrentHashMap<String, Boolean>) ReflectionTestUtils.getField(
                        matchCollectionService, "inProgressMap");
        inProgressMap.put(puuid, Boolean.TRUE);
        when(cachedMatchRepository.countByParticipantPuuid(puuid)).thenReturn(3L);

        // when
        CollectionStatusResponse result = matchCollectionService.getStatus(puuid);

        // then
        assertThat(result.isInProgress()).isTrue();
        assertThat(result.getCollected()).isEqualTo(3);
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
