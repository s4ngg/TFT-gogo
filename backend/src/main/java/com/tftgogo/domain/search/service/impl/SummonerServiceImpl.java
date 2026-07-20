package com.tftgogo.domain.search.service.impl;

import com.tftgogo.domain.match.service.MatchCollectionService;
import com.tftgogo.domain.search.dto.response.RankInfoResponse;
import com.tftgogo.domain.search.dto.response.SummonerDetailResponse;
import com.tftgogo.domain.search.dto.response.SummonerProfileResponse;
import com.tftgogo.domain.search.entity.CachedRank;
import com.tftgogo.domain.search.entity.CachedSummoner;
import com.tftgogo.domain.search.repository.CachedRankRepository;
import com.tftgogo.domain.search.repository.CachedSummonerRepository;
import com.tftgogo.domain.search.service.SummonerService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.riot.RiotApiClient;
import com.tftgogo.global.riot.dto.AccountDto;
import com.tftgogo.global.riot.dto.SummonerDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;

@Service
public class SummonerServiceImpl implements SummonerService {

    private static final Logger logger = LogManager.getLogger(SummonerServiceImpl.class);
    private static final String PROFILE_ICON_URL_FORMAT =
            "https://ddragon.leagueoflegends.com/cdn/latest/img/profileicon/%d.png";
    private static final int SUMMONER_TTL_MINUTES = 60;
    private static final int RANK_TTL_MINUTES = 5;

    private final RiotApiClient riotApiClient;
    private final CachedSummonerRepository cachedSummonerRepository;
    private final CachedRankRepository cachedRankRepository;
    private final MatchCollectionService matchCollectionService;

    public SummonerServiceImpl(RiotApiClient riotApiClient,
                               CachedSummonerRepository cachedSummonerRepository,
                               CachedRankRepository cachedRankRepository,
                               MatchCollectionService matchCollectionService) {
        this.riotApiClient = riotApiClient;
        this.cachedSummonerRepository = cachedSummonerRepository;
        this.cachedRankRepository = cachedRankRepository;
        this.matchCollectionService = matchCollectionService;
    }

    @Override
    public SummonerProfileResponse getProfile(String gameName, String tagLine) {
        return cachedSummonerRepository
                .findByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine)
                .stream()
                .max(Comparator.comparing(CachedSummoner::getCachedAt))
                .filter(cs -> isFresh(cs.getCachedAt(), SUMMONER_TTL_MINUTES))
                .map(cs -> SummonerProfileResponse.builder()
                        .puuid(cs.getPuuid())
                        .gameName(cs.getGameName())
                        .tagLine(cs.getTagLine())
                        .profileIconId(cs.getProfileIconId())
                        .profileIconUrl(String.format(PROFILE_ICON_URL_FORMAT, cs.getProfileIconId()))
                        .summonerLevel(cs.getSummonerLevel())
                        .build())
                .orElseGet(() -> fetchAndCacheSummoner(gameName, tagLine));
    }

    private SummonerProfileResponse fetchAndCacheSummoner(String gameName, String tagLine) {
        AccountDto account = riotApiClient.getAccount(gameName, tagLine);
        SummonerDto summoner = riotApiClient.getSummoner(account.getPuuid());
        String iconUrl = String.format(PROFILE_ICON_URL_FORMAT, summoner.getProfileIconId());

        cachedSummonerRepository.save(CachedSummoner.builder()
                .puuid(account.getPuuid())
                .gameName(account.getGameName())
                .tagLine(account.getTagLine())
                .profileIconId(summoner.getProfileIconId())
                .summonerLevel(summoner.getSummonerLevel())
                .cachedAt(LocalDateTime.now())
                .build());

        return SummonerProfileResponse.of(account, summoner, iconUrl);
    }

    @Override
    public RankInfoResponse getRank(String puuid) {
        return cachedRankRepository.findById(puuid)
                .filter(cr -> isFresh(cr.getCachedAt(), RANK_TTL_MINUTES))
                .map(cr -> RankInfoResponse.builder()
                        .tier(cr.getTier())
                        .rank(cr.getRank())
                        .leaguePoints(cr.getLeaguePoints())
                        .wins(cr.getWins())
                        .losses(cr.getLosses())
                        .unranked(cr.getTier() == null)
                        .build())
                .orElseGet(() -> fetchAndCacheRank(puuid));
    }

    private RankInfoResponse fetchAndCacheRank(String puuid) {
        RankInfoResponse rank;
        try {
            rank = riotApiClient.getLeagueByPuuid(puuid)
                    .map(RankInfoResponse::of)
                    .orElseGet(RankInfoResponse::unranked);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.RIOT_API_RATE_LIMIT
                    || e.getErrorCode() == ErrorCode.RIOT_API_ERROR) {
                throw e;
            }
            logger.warn("league 조회 실패, unranked 처리: puuid={}", puuid, e);
            rank = RankInfoResponse.unranked();
        }

        cachedRankRepository.save(CachedRank.builder()
                .puuid(puuid)
                .tier(rank.getTier())
                .rank(rank.getRank())
                .leaguePoints(rank.getLeaguePoints())
                .wins(rank.getWins())
                .losses(rank.getLosses())
                .cachedAt(LocalDateTime.now())
                .build());

        return rank;
    }

    @Override
    public SummonerDetailResponse getDetail(String gameName, String tagLine) {
        SummonerProfileResponse profile = getProfile(gameName, tagLine);
        RankInfoResponse rankInfo = getRank(profile.getPuuid());
        return SummonerDetailResponse.from(profile, rankInfo);
    }

    @Override
    public SummonerDetailResponse refresh(String gameName, String tagLine) {
        cachedSummonerRepository
                .findByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine)
                .forEach(cs -> {
                    cachedRankRepository.deleteById(cs.getPuuid());
                    cachedSummonerRepository.deleteById(cs.getPuuid());
                });

        SummonerProfileResponse profile = fetchAndCacheSummoner(gameName, tagLine);
        RankInfoResponse rankInfo = fetchAndCacheRank(profile.getPuuid());
        matchCollectionService.refreshMatches(profile.getPuuid());

        return SummonerDetailResponse.from(profile, rankInfo);
    }

    private boolean isFresh(LocalDateTime cachedAt, int ttlMinutes) {
        return cachedAt != null && cachedAt.isAfter(LocalDateTime.now().minusMinutes(ttlMinutes));
    }
}
