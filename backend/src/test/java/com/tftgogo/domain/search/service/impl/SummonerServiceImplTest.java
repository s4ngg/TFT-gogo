package com.tftgogo.domain.search.service.impl;

import com.tftgogo.domain.match.service.MatchCollectionService;
import com.tftgogo.domain.search.dto.response.RankInfoResponse;
import com.tftgogo.domain.search.dto.response.SummonerDetailResponse;
import com.tftgogo.domain.search.dto.response.SummonerProfileResponse;
import com.tftgogo.domain.search.entity.CachedRank;
import com.tftgogo.domain.search.entity.CachedSummoner;
import com.tftgogo.domain.search.repository.CachedRankRepository;
import com.tftgogo.domain.search.repository.CachedSummonerRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.RiotApiClient;
import com.tftgogo.global.riot.dto.AccountDto;
import com.tftgogo.global.riot.dto.LeagueEntryDto;
import com.tftgogo.global.riot.dto.SummonerDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummonerServiceImplTest {

    @Mock private RiotApiClient riotApiClient;
    @Mock private CachedSummonerRepository cachedSummonerRepository;
    @Mock private CachedRankRepository cachedRankRepository;
    @Mock private MatchCollectionService matchCollectionService;
    @InjectMocks private SummonerServiceImpl summonerService;

    private static final String PUUID = "test-puuid";
    private static final String GAME_NAME = "Hide";
    private static final String TAG_LINE = "KR1";

    // ── getProfile ──────────────────────────────────────────────────────────

    @Test
    void TTL_유효한_캐시가_있으면_Riot_API를_호출하지_않는다() {
        // given
        CachedSummoner cached = cachedSummoner(PUUID, GAME_NAME, TAG_LINE,
                LocalDateTime.now().minusMinutes(10));
        when(cachedSummonerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(GAME_NAME, TAG_LINE))
                .thenReturn(List.of(cached));

        // when
        SummonerProfileResponse result = summonerService.getProfile(GAME_NAME, TAG_LINE);

        // then
        assertThat(result.getPuuid()).isEqualTo(PUUID);
        verify(riotApiClient, never()).getAccount(anyString(), anyString());
    }

    @Test
    void getProfile_대소문자_무시로_캐시를_조회한다() {
        // given
        String upperGameName = "HIDE";
        String lowerTagLine = "kr1";
        CachedSummoner cached = cachedSummoner(PUUID, GAME_NAME, TAG_LINE,
                LocalDateTime.now().minusMinutes(10));
        when(cachedSummonerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(upperGameName, lowerTagLine))
                .thenReturn(List.of(cached));

        // when
        SummonerProfileResponse result = summonerService.getProfile(upperGameName, lowerTagLine);

        // then
        assertThat(result.getPuuid()).isEqualTo(PUUID);
        verify(riotApiClient, never()).getAccount(anyString(), anyString());
    }

    @Test
    void TTL_만료된_캐시는_Riot_API를_재조회하고_캐시를_갱신한다() {
        // given
        CachedSummoner stale = cachedSummoner(PUUID, GAME_NAME, TAG_LINE,
                LocalDateTime.now().minusMinutes(70));
        when(cachedSummonerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(GAME_NAME, TAG_LINE))
                .thenReturn(List.of(stale));
        when(riotApiClient.getAccount(GAME_NAME, TAG_LINE))
                .thenReturn(accountDto(PUUID, GAME_NAME, TAG_LINE));
        when(riotApiClient.getSummoner(PUUID)).thenReturn(summonerDto(PUUID, 100, 300L));

        // when
        summonerService.getProfile(GAME_NAME, TAG_LINE);

        // then
        verify(riotApiClient).getAccount(GAME_NAME, TAG_LINE);
        verify(cachedSummonerRepository).save(any(CachedSummoner.class));
    }

    @Test
    void 캐시_미스시_Riot_API를_호출하고_저장한다() {
        // given
        when(cachedSummonerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(GAME_NAME, TAG_LINE))
                .thenReturn(List.of());
        when(riotApiClient.getAccount(GAME_NAME, TAG_LINE))
                .thenReturn(accountDto(PUUID, GAME_NAME, TAG_LINE));
        when(riotApiClient.getSummoner(PUUID)).thenReturn(summonerDto(PUUID, 100, 300L));

        // when
        SummonerProfileResponse result = summonerService.getProfile(GAME_NAME, TAG_LINE);

        // then
        assertThat(result.getPuuid()).isEqualTo(PUUID);
        verify(riotApiClient).getAccount(GAME_NAME, TAG_LINE);
        verify(riotApiClient).getSummoner(PUUID);
        verify(cachedSummonerRepository).save(any(CachedSummoner.class));
    }

    @Test
    void 같은_소환사명에_puuid가_다른_캐시_행이_여러_개면_가장_최근_캐시를_사용한다() {
        // given: 라이엇 ID 소유권 이전 등으로 puuid가 다른 중복 캐시 행이 남아있는 상황
        String olderPuuid = "old-puuid";
        CachedSummoner older = cachedSummoner(olderPuuid, GAME_NAME, TAG_LINE,
                LocalDateTime.now().minusMinutes(30));
        CachedSummoner newer = cachedSummoner(PUUID, GAME_NAME, TAG_LINE,
                LocalDateTime.now().minusMinutes(5));
        when(cachedSummonerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(GAME_NAME, TAG_LINE))
                .thenReturn(List.of(older, newer));

        // when
        SummonerProfileResponse result = summonerService.getProfile(GAME_NAME, TAG_LINE);

        // then
        assertThat(result.getPuuid()).isEqualTo(PUUID);
        verify(riotApiClient, never()).getAccount(anyString(), anyString());
    }

    // ── getRank ──────────────────────────────────────────────────────────────

    @Test
    void getRank_TTL_유효한_캐시가_있으면_Riot_API를_호출하지_않는다() {
        // given
        CachedRank cached = cachedRank(PUUID, "GOLD", "II",
                LocalDateTime.now().minusMinutes(2));
        when(cachedRankRepository.findById(PUUID)).thenReturn(Optional.of(cached));

        // when
        RankInfoResponse result = summonerService.getRank(PUUID);

        // then
        assertThat(result.getTier()).isEqualTo("GOLD");
        assertThat(result.isUnranked()).isFalse();
        verify(riotApiClient, never()).getLeagueByPuuid(anyString());
    }

    @Test
    void getRank_TTL_만료되면_Riot_API를_재조회한다() {
        // given
        CachedRank stale = cachedRank(PUUID, "GOLD", "I",
                LocalDateTime.now().minusMinutes(10));
        when(cachedRankRepository.findById(PUUID)).thenReturn(Optional.of(stale));
        when(riotApiClient.getLeagueByPuuid(PUUID))
                .thenReturn(Optional.of(leagueEntryDto(PUUID, "PLATINUM", "II", 50, 100, 80)));

        // when
        RankInfoResponse result = summonerService.getRank(PUUID);

        // then
        assertThat(result.getTier()).isEqualTo("PLATINUM");
        verify(riotApiClient).getLeagueByPuuid(PUUID);
        verify(cachedRankRepository).save(any(CachedRank.class));
    }

    @Test
    void getRank_API가_empty를_반환하면_unranked로_응답한다() {
        // given
        when(cachedRankRepository.findById(PUUID)).thenReturn(Optional.empty());
        when(riotApiClient.getLeagueByPuuid(PUUID)).thenReturn(Optional.empty());

        // when
        RankInfoResponse result = summonerService.getRank(PUUID);

        // then
        assertThat(result.isUnranked()).isTrue();
    }

    @Test
    void getRank_RATE_LIMIT_에러는_예외를_전파한다() {
        // given
        when(cachedRankRepository.findById(PUUID)).thenReturn(Optional.empty());
        when(riotApiClient.getLeagueByPuuid(PUUID))
                .thenThrow(new BusinessException(ErrorCode.RIOT_API_RATE_LIMIT));

        // when, then
        assertThatThrownBy(() -> summonerService.getRank(PUUID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RIOT_API_RATE_LIMIT));
    }

    @Test
    void getRank_캐시_미스_시_일반_RuntimeException은_전파된다() {
        // given
        when(cachedRankRepository.findById(PUUID)).thenReturn(Optional.empty());
        when(riotApiClient.getLeagueByPuuid(PUUID))
                .thenThrow(new RuntimeException("network error"));

        // when, then
        assertThatThrownBy(() -> summonerService.getRank(PUUID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("network error");
    }

    @Test
    void getRank_그_외_BusinessException은_unranked_fallback으로_처리한다() {
        // given
        when(cachedRankRepository.findById(PUUID)).thenReturn(Optional.empty());
        when(riotApiClient.getLeagueByPuuid(PUUID))
                .thenThrow(new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));

        // when
        RankInfoResponse result = summonerService.getRank(PUUID);

        // then
        assertThat(result.isUnranked()).isTrue();
    }

    // ── getDetail ────────────────────────────────────────────────────────────

    @Test
    void getDetail은_getProfile과_getRank_결과를_합성한다() {
        // given
        CachedSummoner cachedSummonerData = cachedSummoner(PUUID, GAME_NAME, TAG_LINE,
                LocalDateTime.now().minusMinutes(5));
        CachedRank cachedRankData = cachedRank(PUUID, "DIAMOND", "III",
                LocalDateTime.now().minusMinutes(1));
        when(cachedSummonerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(GAME_NAME, TAG_LINE))
                .thenReturn(List.of(cachedSummonerData));
        when(cachedRankRepository.findById(PUUID)).thenReturn(Optional.of(cachedRankData));

        // when
        SummonerDetailResponse result = summonerService.getDetail(GAME_NAME, TAG_LINE);

        // then
        assertThat(result).isNotNull();
        verify(cachedSummonerRepository).findByGameNameIgnoreCaseAndTagLineIgnoreCase(GAME_NAME, TAG_LINE);
        verify(cachedRankRepository).findById(PUUID);
    }

    @Test
    void getDetail_profile과_rank_필드를_정확히_합성한다() {
        // given
        CachedSummoner cachedSummonerData = cachedSummoner(PUUID, GAME_NAME, TAG_LINE,
                LocalDateTime.now().minusMinutes(5));
        CachedRank cachedRankData = cachedRank(PUUID, "DIAMOND", "III",
                LocalDateTime.now().minusMinutes(1));
        when(cachedSummonerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(GAME_NAME, TAG_LINE))
                .thenReturn(List.of(cachedSummonerData));
        when(cachedRankRepository.findById(PUUID)).thenReturn(Optional.of(cachedRankData));

        // when
        SummonerDetailResponse result = summonerService.getDetail(GAME_NAME, TAG_LINE);

        // then
        assertThat(result.getPuuid()).isEqualTo(PUUID);
        assertThat(result.getGameName()).isEqualTo(GAME_NAME);
        assertThat(result.getTagLine()).isEqualTo(TAG_LINE);
        assertThat(result.getTier()).isEqualTo("DIAMOND");
        assertThat(result.getRank()).isEqualTo("III");
        assertThat(result.getLeaguePoints()).isEqualTo(50);
        assertThat(result.getWins()).isEqualTo(100);
        assertThat(result.getLosses()).isEqualTo(80);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CachedSummoner cachedSummoner(String puuid, String gameName, String tagLine,
                                          LocalDateTime cachedAt) {
        return CachedSummoner.builder()
                .puuid(puuid)
                .gameName(gameName)
                .tagLine(tagLine)
                .profileIconId(100)
                .summonerLevel(300L)
                .cachedAt(cachedAt)
                .build();
    }

    private CachedRank cachedRank(String puuid, String tier, String rank, LocalDateTime cachedAt) {
        return CachedRank.builder()
                .puuid(puuid)
                .tier(tier)
                .rank(rank)
                .leaguePoints(50)
                .wins(100)
                .losses(80)
                .cachedAt(cachedAt)
                .build();
    }

    private AccountDto accountDto(String puuid, String gameName, String tagLine) {
        AccountDto dto = new AccountDto();
        ReflectionTestUtils.setField(dto, "puuid", puuid);
        ReflectionTestUtils.setField(dto, "gameName", gameName);
        ReflectionTestUtils.setField(dto, "tagLine", tagLine);
        return dto;
    }

    private SummonerDto summonerDto(String puuid, int profileIconId, long summonerLevel) {
        SummonerDto dto = new SummonerDto();
        ReflectionTestUtils.setField(dto, "puuid", puuid);
        ReflectionTestUtils.setField(dto, "profileIconId", profileIconId);
        ReflectionTestUtils.setField(dto, "summonerLevel", summonerLevel);
        return dto;
    }

    private LeagueEntryDto leagueEntryDto(String puuid, String tier, String rank,
                                          int leaguePoints, int wins, int losses) {
        LeagueEntryDto dto = new LeagueEntryDto();
        ReflectionTestUtils.setField(dto, "puuid", puuid);
        ReflectionTestUtils.setField(dto, "queueType", "RANKED_TFT");
        ReflectionTestUtils.setField(dto, "tier", tier);
        ReflectionTestUtils.setField(dto, "rank", rank);
        ReflectionTestUtils.setField(dto, "leaguePoints", leaguePoints);
        ReflectionTestUtils.setField(dto, "wins", wins);
        ReflectionTestUtils.setField(dto, "losses", losses);
        return dto;
    }
}
