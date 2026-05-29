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
    private static final int MAX_RETRY_COUNT = 3;
    private static final long MIN_REQUEST_INTERVAL_MS = 1300L;
    private static final long DEFAULT_RATE_LIMIT_WAIT_MS = 120_000L;

    private final RiotProperties riotProperties;
    private final RestTemplate restTemplate;
    private long lastRequestAt = 0L;

    public LeagueListDto getChallenger() {
        return get("/tft/league/v1/challenger?queue=RANKED_TFT",
                riotProperties.getKrBaseUrl(), LeagueListDto.class);
    }

    public LeagueListDto getGrandmaster() {
        return get("/tft/league/v1/grandmaster?queue=RANKED_TFT",
                riotProperties.getKrBaseUrl(), LeagueListDto.class);
    }

    public LeagueListDto getMaster() {
        return get("/tft/league/v1/master?queue=RANKED_TFT",
                riotProperties.getKrBaseUrl(), LeagueListDto.class);
    }

    public List<LeagueEntryDto> getLeagueEntries(String tier, String division, int page) {
        String path = "/tft/league/v1/entries/" + tier + "/" + division
                + "?queue=RANKED_TFT&page=" + page;
        LeagueEntryDto[] entries = get(path, riotProperties.getKrBaseUrl(), LeagueEntryDto[].class);
        return entries != null ? Arrays.asList(entries) : List.of();
    }

    public List<String> getMatchIds(String puuid, int count) {
        String path = "/tft/match/v1/matches/by-puuid/{puuid}/ids";
        String url = riotProperties.getAsiaBaseUrl()
                + "/tft/match/v1/matches/by-puuid/" + puuid + "/ids?queue=1100&count=" + count;
        return Arrays.asList(getByUrl(url, path, String[].class));
    }

    public List<String> getMatchIds(String puuid, int count, long startTimeSeconds) {
        String path = "/tft/match/v1/matches/by-puuid/{puuid}/ids";
        String url = riotProperties.getAsiaBaseUrl()
                + "/tft/match/v1/matches/by-puuid/" + puuid + "/ids?queue=1100&count=" + count
                + "&startTime=" + startTimeSeconds;
        return Arrays.asList(getByUrl(url, path, String[].class));
    }

    public List<String> getMatchIds(String puuid, int count, long startTimeSeconds, long endTimeSeconds) {
        String path = "/tft/match/v1/matches/by-puuid/{puuid}/ids";
        String url = riotProperties.getAsiaBaseUrl()
                + "/tft/match/v1/matches/by-puuid/" + puuid + "/ids?queue=1100&count=" + count
                + "&startTime=" + startTimeSeconds
                + "&endTime=" + endTimeSeconds;
        return Arrays.asList(getByUrl(url, path, String[].class));
    }

    public MatchDto getMatch(String matchId) {
        String path = "/tft/match/v1/matches/{matchId}";
        String url = riotProperties.getAsiaBaseUrl() + "/tft/match/v1/matches/" + matchId;
        return getByUrl(url, path, MatchDto.class);
    }

    private <T> T get(String path, String baseUrl, Class<T> responseType) {
        return getByUrl(baseUrl + path, path, responseType);
    }

    private <T> T getByUrl(String url, String logPath, Class<T> responseType) {
        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                waitForRequestWindow();

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Riot-Token", riotProperties.getApiKey());
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<T> response = restTemplate.exchange(
                        url, HttpMethod.GET, entity, responseType);

                return Optional.ofNullable(response.getBody())
                        .orElseThrow(() -> new BusinessException(ErrorCode.RIOT_API_ERROR));

            } catch (BusinessException e) {
                throw e;
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == MAX_RETRY_COUNT) {
                    logger.warn("Riot API rate limit exceeded after retries: endpoint={}", logPath);
                    throw new BusinessException(ErrorCode.RIOT_API_RATE_LIMIT);
                }

                long waitMs = getRateLimitWaitMs(e);
                logger.warn("Riot API rate limit exceeded: endpoint={}, retry={}/{}, waitMs={}",
                        logPath, attempt, MAX_RETRY_COUNT, waitMs);
                sleep(waitMs);
            } catch (Exception e) {
                logger.error("Riot API request failed: endpoint={}", logPath, e);
                throw new BusinessException(ErrorCode.RIOT_API_ERROR);
            }
        }

        throw new BusinessException(ErrorCode.RIOT_API_ERROR);
    }

    private synchronized void waitForRequestWindow() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestAt;
        if (elapsed < MIN_REQUEST_INTERVAL_MS) {
            sleep(MIN_REQUEST_INTERVAL_MS - elapsed);
        }
        lastRequestAt = System.currentTimeMillis();
    }

    private long getRateLimitWaitMs(HttpClientErrorException.TooManyRequests e) {
        String retryAfter = e.getResponseHeaders() != null
                ? e.getResponseHeaders().getFirst("Retry-After")
                : null;

        if (retryAfter == null || retryAfter.isBlank()) {
            return DEFAULT_RATE_LIMIT_WAIT_MS;
        }

        try {
            return Math.max(1, Long.parseLong(retryAfter)) * 1000L;
        } catch (NumberFormatException ignored) {
            return DEFAULT_RATE_LIMIT_WAIT_MS;
        }
    }

    private void sleep(long waitMs) {
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.RIOT_API_ERROR);
        }
    }
}
