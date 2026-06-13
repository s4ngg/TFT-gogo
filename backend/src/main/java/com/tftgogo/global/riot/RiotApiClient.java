package com.tftgogo.global.riot;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.config.RiotProperties;
import com.tftgogo.global.riot.dto.AccountDto;
import com.tftgogo.global.riot.dto.LeagueEntryDto;
import com.tftgogo.global.riot.dto.LeagueListDto;
import com.tftgogo.global.riot.dto.MatchDto;
import com.tftgogo.global.riot.dto.SummonerDto;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class RiotApiClient {

    private static final Logger logger = LogManager.getLogger(RiotApiClient.class);
    private static final int DEFAULT_RETRY_AFTER_SECONDS = 120;

    private final RiotProperties riotProperties;
    private final RestTemplate restTemplate;
    private final RiotRateLimiter rateLimiter;

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

    // ── 소환사 계정 조회 (account-v1, asia) ────────────────
    public AccountDto getAccount(String gameName, String tagLine) {
        String path = "/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}";
        String url = UriComponentsBuilder
                .fromHttpUrl(riotProperties.getAsiaBaseUrl())
                .pathSegment("riot", "account", "v1", "accounts", "by-riot-id", gameName, tagLine)
                .build()
                .toUriString();
        return getByUrl(url, path, AccountDto.class, ErrorCode.ACCOUNT_NOT_FOUND);
    }

    // ── 소환사 정보 조회 (tft-summoner-v1, kr) ──────────────
    public SummonerDto getSummoner(String puuid) {
        String path = "/tft/summoner/v1/summoners/by-puuid/{puuid}";
        String url = riotProperties.getKrBaseUrl()
                + "/tft/summoner/v1/summoners/by-puuid/" + puuid;
        return getByUrl(url, path, SummonerDto.class, ErrorCode.SUMMONER_NOT_FOUND);
    }

    // ── 소환사 랭크 정보 조회 (tft-league-v1, kr) ────────────
    // RANKED_TFT 큐타입 항목만 반환. 배치 미완료·미배치(404) 시 Optional.empty()
    public Optional<LeagueEntryDto> getLeagueByPuuid(String puuid) {
        String path = "/tft/league/v1/by-puuid/{puuid}";
        String url = riotProperties.getKrBaseUrl() + "/tft/league/v1/by-puuid/" + puuid;
        try {
            LeagueEntryDto[] entries = getByUrl(url, path, LeagueEntryDto[].class, ErrorCode.LEAGUE_NOT_FOUND);
            return Stream.of(entries)
                    .filter(e -> "RANKED_TFT".equals(e.getQueueType()))
                    .findFirst();
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.LEAGUE_NOT_FOUND) {
                logger.warn("Riot API 404 — league not found (unranked): puuid={}", puuid);
                return Optional.empty();
            }
            throw e;
        }
    }

    // ── 매치 ID 목록 (start 오프셋 지원, 큐 무관 — 전적 검색용) ──
    public List<String> getMatchIds(String puuid, int count, int start) {
        String path = "/tft/match/v1/matches/by-puuid/{puuid}/ids";
        String url = riotProperties.getAsiaBaseUrl()
                + "/tft/match/v1/matches/by-puuid/" + puuid
                + "/ids?count=" + count + "&start=" + start;
        String[] ids = getByUrl(url, path, String[].class, ErrorCode.RIOT_API_ERROR);
        return ids != null ? Arrays.asList(ids) : List.of();
    }

    // ── 매치 ID 목록 (시간 범위 필터, queue=1100 — 덱 집계용) ──
    public List<String> getMatchIds(String puuid, int count, long startTime, long endTime) {
        String path = "/tft/match/v1/matches/by-puuid/{puuid}/ids";
        String url = riotProperties.getAsiaBaseUrl()
                + "/tft/match/v1/matches/by-puuid/" + puuid
                + "/ids?queue=1100&count=" + count
                + "&startTime=" + startTime
                + "&endTime=" + endTime;
        String[] ids = getByUrl(url, path, String[].class, ErrorCode.RIOT_API_ERROR);
        return ids != null ? Arrays.asList(ids) : List.of();
    }

    // ── 매치 상세 조회 ─────────────────────────────────────
    public MatchDto getMatch(String matchId) {
        String path = "/tft/match/v1/matches/{matchId}";
        String url = riotProperties.getAsiaBaseUrl() + "/tft/match/v1/matches/" + matchId;
        return getByUrl(url, path, MatchDto.class, ErrorCode.MATCH_NOT_FOUND);
    }

    // ── 매치 ID 목록 (queue 필터 + start 오프셋 지원 — 전적 수집용) ──
    public List<String> getMatchIds(String puuid, int count, int start, int queue) {
        String path = "/tft/match/v1/matches/by-puuid/{puuid}/ids";
        String url = riotProperties.getAsiaBaseUrl()
                + "/tft/match/v1/matches/by-puuid/" + puuid
                + "/ids?queue=" + queue + "&count=" + count + "&start=" + start;
        String[] ids = getByUrl(url, path, String[].class, ErrorCode.RIOT_API_ERROR);
        return ids != null ? Arrays.asList(ids) : List.of();
    }

    // ── 공통 GET (baseUrl 분기용) ───────────────────────────
    private <T> T get(String path, String baseUrl, Class<T> responseType) {
        return getByUrl(baseUrl + path, path, responseType, ErrorCode.RIOT_API_ERROR);
    }

    // ── 공통 GET — rate limiter 통과 후 실행 ──
    private <T> T getByUrl(String url, String logPath, Class<T> responseType, ErrorCode notFoundCode) {
        try {
            rateLimiter.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.RIOT_API_ERROR);
        }
        return executeRequest(url, logPath, responseType, notFoundCode);
    }

    // ── HTTP 실행 (스로틀 없음) ──
    private <T> T executeRequest(String url, String logPath, Class<T> responseType, ErrorCode notFoundCode) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Riot-Token", riotProperties.getApiKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<T> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, responseType);

            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RIOT_API_ERROR));

        } catch (BusinessException e) {
            throw e;
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Riot API 404: endpoint={}", logPath);
            throw new BusinessException(notFoundCode);
        } catch (HttpClientErrorException.TooManyRequests e) {
            int retryAfterSeconds = parseRetryAfterSeconds(e);
            logger.warn("Riot API rate limit exceeded: endpoint={}, retryAfterSeconds={}", logPath, retryAfterSeconds);
            throw new BusinessException(ErrorCode.RIOT_API_RATE_LIMIT, retryAfterSeconds);
        } catch (Exception e) {
            logger.error("Riot API 호출 실패: endpoint={}", logPath, e);
            throw new BusinessException(ErrorCode.RIOT_API_ERROR);
        }
    }

    private int parseRetryAfterSeconds(HttpClientErrorException.TooManyRequests e) {
        String retryAfter = e.getResponseHeaders() != null
                ? e.getResponseHeaders().getFirst("Retry-After")
                : null;
        if (retryAfter == null || retryAfter.isBlank()) return DEFAULT_RETRY_AFTER_SECONDS;
        try {
            return Math.max(1, Integer.parseInt(retryAfter.trim()));
        } catch (NumberFormatException ignored) {
            return DEFAULT_RETRY_AFTER_SECONDS;
        }
    }

}
