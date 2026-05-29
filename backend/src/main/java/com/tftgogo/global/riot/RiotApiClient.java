package com.tftgogo.global.riot;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.config.RiotProperties;
import com.tftgogo.global.riot.dto.LeagueEntryDto;
import com.tftgogo.global.riot.dto.LeagueListDto;
import com.tftgogo.global.riot.dto.MatchDto;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RiotApiClient {

    private static final Logger logger = LogManager.getLogger(RiotApiClient.class);

    private final RiotProperties riotProperties;
    private final RestTemplate restTemplate;

    // ── Challenger 리그 조회 ────────────────────────────────
    public LeagueListDto getChallenger() {
        return get("/tft/league/v1/challenger?queue=RANKED_TFT",
                riotProperties.getKrBaseUrl(), LeagueListDto.class);
    }

    // ── Grandmaster 리그 조회 ───────────────────────────────
    public LeagueListDto getGrandmaster() {
        return get("/tft/league/v1/grandmaster?queue=RANKED_TFT",
                riotProperties.getKrBaseUrl(), LeagueListDto.class);
    }

    // ── Master 리그 조회 ────────────────────────────────────
    public LeagueListDto getMaster() {
        return get("/tft/league/v1/master?queue=RANKED_TFT",
                riotProperties.getKrBaseUrl(), LeagueListDto.class);
    }

    // ── 특정 티어/디비전 소환사 목록 조회 ──────────────────────
    // tier: DIAMOND/EMERALD, division: I/II/III/IV
    public List<LeagueEntryDto> getLeagueEntries(String tier, String division, int page) {
        String path = "/tft/league/v1/entries/" + tier + "/" + division
                + "?queue=RANKED_TFT&page=" + page;
        LeagueEntryDto[] entries = get(path, riotProperties.getKrBaseUrl(), LeagueEntryDto[].class);
        return entries != null ? Arrays.asList(entries) : List.of();
    }

    // ── 소환사 PUUID로 최근 매치 ID 목록 조회 ─────────────────
    public List<String> getMatchIds(String puuid, int count) {
        String path = "/tft/match/v1/matches/by-puuid/{puuid}/ids";
        String url = riotProperties.getAsiaBaseUrl()
                + "/tft/match/v1/matches/by-puuid/" + puuid + "/ids?queue=1100&count=" + count;
        // getByUrl은 body가 null이면 예외를 던지므로 null 체크 불필요
        return Arrays.asList(getByUrl(url, path, String[].class));
    }

    // ── 특정 날짜 시작 시각 이후 매치 ID 목록 조회 ────────────
    public List<String> getMatchIds(String puuid, int count, long startTimeSeconds) {
        String path = "/tft/match/v1/matches/by-puuid/{puuid}/ids";
        String url = riotProperties.getAsiaBaseUrl()
                + "/tft/match/v1/matches/by-puuid/" + puuid + "/ids?queue=1100&count=" + count
                + "&startTime=" + startTimeSeconds;
        return Arrays.asList(getByUrl(url, path, String[].class));
    }

    // ── 특정 날짜 구간 매치 ID 목록 조회 ──────────────────────
    public List<String> getMatchIds(String puuid, int count, long startTimeSeconds, long endTimeSeconds) {
        String path = "/tft/match/v1/matches/by-puuid/{puuid}/ids";
        String url = riotProperties.getAsiaBaseUrl()
                + "/tft/match/v1/matches/by-puuid/" + puuid + "/ids?queue=1100&count=" + count
                + "&startTime=" + startTimeSeconds
                + "&endTime=" + endTimeSeconds;
        return Arrays.asList(getByUrl(url, path, String[].class));
    }

    // ── 매치 상세 조회 ─────────────────────────────────────
    public MatchDto getMatch(String matchId) {
        String path = "/tft/match/v1/matches/{matchId}";
        String url = riotProperties.getAsiaBaseUrl() + "/tft/match/v1/matches/" + matchId;
        return getByUrl(url, path, MatchDto.class);
    }

    // ── 공통 GET (baseUrl 분기용) ───────────────────────────
    private <T> T get(String path, String baseUrl, Class<T> responseType) {
        return getByUrl(baseUrl + path, path, responseType);
    }

    // ── 공통 GET (로그에는 path만, URL에 민감정보 포함 가능) ──
    private <T> T getByUrl(String url, String logPath, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Riot-Token", riotProperties.getApiKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<T> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, responseType);

            // null body 방어 (컨벤션: null 반환 금지 → orElseThrow)
            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RIOT_API_ERROR));

        } catch (BusinessException e) {
            throw e;
        } catch (HttpClientErrorException.TooManyRequests e) {
            logger.warn("Riot API 레이트 리밋 초과: endpoint={}", logPath);
            throw new BusinessException(ErrorCode.RIOT_API_RATE_LIMIT);
        } catch (Exception e) {
            logger.error("Riot API 호출 실패: endpoint={}", logPath, e);
            throw new BusinessException(ErrorCode.RIOT_API_ERROR);
        }
    }
}
