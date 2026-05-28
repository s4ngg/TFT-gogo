package com.tftgogo.global.riot;

import com.tftgogo.global.riot.config.RiotProperties;
import com.tftgogo.global.riot.dto.LeagueListDto;
import com.tftgogo.global.riot.dto.MatchDto;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RiotApiClient {

    private static final Logger logger = LogManager.getLogger(RiotApiClient.class);

    private final RiotProperties riotProperties;
    private final RestTemplate restTemplate;

    // ── Challenger 리그 조회 ────────────────────────────────
    public LeagueListDto getChallenger() {
        String url = riotProperties.getKrBaseUrl()
                + "/tft/league/v1/challenger?queue=RANKED_TFT";
        return get(url, LeagueListDto.class);
    }

    // ── Grandmaster 리그 조회 ───────────────────────────────
    public LeagueListDto getGrandmaster() {
        String url = riotProperties.getKrBaseUrl()
                + "/tft/league/v1/grandmaster?queue=RANKED_TFT";
        return get(url, LeagueListDto.class);
    }

    // ── 소환사 PUUID로 최근 매치 ID 목록 조회 ─────────────────
    public List<String> getMatchIds(String puuid, int count) {
        String url = riotProperties.getAsiaBaseUrl()
                + "/tft/match/v1/matches/by-puuid/" + puuid
                + "/ids?queue=1100&count=" + count;
        String[] ids = get(url, String[].class);
        return ids != null ? Arrays.asList(ids) : List.of();
    }

    // ── 매치 상세 조회 ─────────────────────────────────────
    public MatchDto getMatch(String matchId) {
        String url = riotProperties.getAsiaBaseUrl()
                + "/tft/match/v1/matches/" + matchId;
        return get(url, MatchDto.class);
    }

    // ── 공통 GET ───────────────────────────────────────────
    private <T> T get(String url, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Riot-Token", riotProperties.getApiKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<T> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, responseType);
            return response.getBody();
        } catch (HttpClientErrorException.TooManyRequests e) {
            logger.warn("Riot API 레이트 리밋 초과: {}", url);
            throw e;
        } catch (Exception e) {
            logger.error("Riot API 호출 실패: {}", url, e);
            throw e;
        }
    }
}
