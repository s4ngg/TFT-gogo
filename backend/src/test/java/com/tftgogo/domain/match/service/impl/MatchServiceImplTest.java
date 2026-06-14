package com.tftgogo.domain.match.service.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.match.dto.response.CollectionStatusResponse;
import com.tftgogo.domain.match.dto.response.MatchDetailResponse;
import com.tftgogo.domain.match.entity.CachedMatch;
import com.tftgogo.domain.match.repository.CachedMatchRepository;
import com.tftgogo.domain.match.service.MatchCollectionService;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItemDto;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.RiotApiClient;
import com.tftgogo.global.riot.dto.MatchDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock private RiotApiClient riotApiClient;
    @Mock private MatchCollectionService matchCollectionService;
    @Mock private CachedMatchRepository cachedMatchRepository;
    @InjectMocks private MatchServiceImpl matchService;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);

    @Test
    void 캐시에_데이터가_있으면_Riot_API를_호출하지_않는다() {
        // given
        String matchId = "KR_12345";
        when(cachedMatchRepository.findById(matchId))
                .thenReturn(Optional.of(cachedMatch(matchId, 1100, validMatchJson(1100))));

        // when
        MatchDetailResponse result = matchService.getMatchDetail(matchId);

        // then
        assertThat(result.getMatchId()).isEqualTo(matchId);
        assertThat(result.getQueueType()).isEqualTo("RANKED");
        verify(riotApiClient, never()).getMatch(any());
    }

    @Test
    void 캐시_미스시_Riot_API를_호출하고_DB에_저장한다() {
        // given
        String matchId = "KR_12345";
        when(cachedMatchRepository.findById(matchId)).thenReturn(Optional.empty());
        when(riotApiClient.getMatch(matchId)).thenReturn(matchDto(1100));

        // when
        MatchDetailResponse result = matchService.getMatchDetail(matchId);

        // then
        assertThat(result.getMatchId()).isEqualTo(matchId);
        verify(riotApiClient).getMatch(matchId);
        verify(cachedMatchRepository).save(any(CachedMatch.class));
    }

    @Test
    void 캐시_역직렬화_실패시_Riot_API_재조회로_fallback한다() {
        // given
        String matchId = "KR_12345";
        when(cachedMatchRepository.findById(matchId))
                .thenReturn(Optional.of(cachedMatch(matchId, 1100, "invalid-json{{{")));
        when(riotApiClient.getMatch(matchId)).thenReturn(matchDto(1100));

        // when
        MatchDetailResponse result = matchService.getMatchDetail(matchId);

        // then
        assertThat(result).isNotNull();
        verify(riotApiClient).getMatch(matchId);
    }

    @Test
    void 유효하지_않은_queueId는_BusinessException_RIOT_API_ERROR를_던진다() {
        // given
        String matchId = "KR_12345";
        when(cachedMatchRepository.findById(matchId))
                .thenReturn(Optional.of(cachedMatch(matchId, 9999, validMatchJson(9999))));

        // when, then
        assertThatThrownBy(() -> matchService.getMatchDetail(matchId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RIOT_API_ERROR));
    }

    @Test
    void getMatches는_matchCollectionService_fetchAndCache에_위임한다() {
        // given
        String puuid = "test-puuid";
        List<SummonerMatchItemDto> expected = List.of();
        when(matchCollectionService.fetchAndCache(eq(puuid), eq(0), eq(10), any(), any(), any()))
                .thenReturn(expected);

        // when
        List<SummonerMatchItemDto> result = matchService.getMatches(puuid, 0, 10,
                Function.identity(), Function.identity(), s -> null);

        // then
        assertThat(result).isEqualTo(expected);
        verify(matchCollectionService).fetchAndCache(eq(puuid), eq(0), eq(10), any(), any(), any());
    }

    @Test
    void getCollectionStatus는_matchCollectionService_getStatus에_위임한다() {
        // given
        String puuid = "test-puuid";
        CollectionStatusResponse expected = CollectionStatusResponse.builder()
                .collected(5).inProgress(true).build();
        when(matchCollectionService.getStatus(puuid)).thenReturn(expected);

        // when
        CollectionStatusResponse result = matchService.getCollectionStatus(puuid);

        // then
        assertThat(result.getCollected()).isEqualTo(5);
        assertThat(result.isInProgress()).isTrue();
        verify(matchCollectionService).getStatus(puuid);
    }

    private CachedMatch cachedMatch(String matchId, int queueId, String json) {
        return CachedMatch.builder()
                .matchId(matchId)
                .queueId(queueId)
                .gameDatetime(1000L)
                .matchJson(json)
                .createdAt(LocalDateTime.now())
                .participantPuuids(Set.of())
                .build();
    }

    private String validMatchJson(int queueId) {
        return String.format(
                "{\"info\":{\"queue_id\":%d,\"game_datetime\":1000,\"game_length\":1800.0,"
                        + "\"game_version\":\"v14\",\"tft_set_number\":13,"
                        + "\"tft_set_core_name\":\"TFTSet13\",\"tft_game_type\":\"standard\","
                        + "\"participants\":[]}}",
                queueId);
    }

    private MatchDto matchDto(int queueId) {
        try {
            return MAPPER.readValue(validMatchJson(queueId), MatchDto.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
