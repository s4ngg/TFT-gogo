package com.tftgogo.domain.summoner.controller;

import com.tftgogo.domain.match.dto.response.MatchSearchResponse;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.global.riot.util.TftAssetUrlBuilder;
import com.tftgogo.domain.summoner.controller.docs.SummonerControllerDocs;
import com.tftgogo.domain.summoner.dto.response.SummonerDetailResponse;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItem;
import com.tftgogo.domain.summoner.service.SummonerService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/summoners")
@RequiredArgsConstructor
public class SummonerController implements SummonerControllerDocs {

    private final SummonerService summonerService;

    @Override
    @GetMapping("/{gameName}/{tagLine}")
    public ResponseEntity<ApiResponse<SummonerDetailResponse>> getSummoner(
            @PathVariable("gameName") String gameName,
            @PathVariable("tagLine") String tagLine) {
        MatchSearchResponse result = summonerService.search(gameName, tagLine);
        return ResponseEntity.ok(ApiResponse.success("소환사 조회 성공", buildDetail(result)));
    }

    @Override
    @GetMapping("/{gameName}/{tagLine}/matches")
    public ResponseEntity<ApiResponse<List<SummonerMatchItem>>> getMatches(
            @PathVariable("gameName") String gameName,
            @PathVariable("tagLine") String tagLine) {
        MatchSearchResponse result = summonerService.search(gameName, tagLine);
        List<SummonerMatchItem> matches = result.getMatchList().stream()
                .map(this::toMatchItem)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("매치 조회 성공", matches));
    }

    private SummonerDetailResponse buildDetail(MatchSearchResponse result) {
        var profile = result.getProfile();
        var rankInfo = result.getRankInfo();
        var matchList = result.getMatchList();

        int wins = (int) matchList.stream().filter(m -> m.getPlacement() <= 4).count();
        int losses = matchList.size() - wins;
        double avgPlace = matchList.isEmpty() ? 0.0
                : matchList.stream().mapToInt(MatchSummaryResponse::getPlacement).average().orElse(0.0);
        double top4Rate = matchList.isEmpty() ? 0.0
                : (double) wins / matchList.size() * 100.0;

        int[] rankDist = new int[8];
        matchList.forEach(m -> {
            int idx = Math.min(m.getPlacement() - 1, 7);
            rankDist[idx]++;
        });

        return SummonerDetailResponse.builder()
                .puuid(profile.getPuuid())
                .gameName(profile.getGameName())
                .tagLine(profile.getTagLine())
                .profileIconId(profile.getProfileIconId())
                .summonerLevel(profile.getSummonerLevel())
                .tier(rankInfo.isUnranked() ? null : rankInfo.getTier())
                .rank(rankInfo.isUnranked() ? null : rankInfo.getRank())
                .leaguePoints(rankInfo.isUnranked() ? 0 : rankInfo.getLeaguePoints())
                .wins(wins)
                .losses(losses)
                .avgPlace(Math.round(avgPlace * 10.0) / 10.0)
                .top4Rate(Math.round(top4Rate * 10.0) / 10.0)
                .rankDistribution(rankDist)
                .topTraits(buildTopTraits(matchList))
                .topChampions(buildTopChampions(matchList))
                .build();
    }

    private SummonerMatchItem toMatchItem(MatchSummaryResponse m) {
        List<SummonerMatchItem.TraitDto> traits = m.getTraits().stream()
                .filter(t -> t.getStyle() > 0)
                .map(t -> SummonerMatchItem.TraitDto.builder()
                        .traitId(t.getName())
                        .name(t.getName())
                        .iconUrl(TftAssetUrlBuilder.buildTraitIconUrl(t.getName()))
                        .count(t.getNumUnits())
                        .tone(styleToTone(t.getStyle()))
                        .build())
                .collect(Collectors.toList());

        List<SummonerMatchItem.UnitDto> units = m.getUnits().stream()
                .map(u -> SummonerMatchItem.UnitDto.builder()
                        .characterId(u.getCharacterId())
                        .imageUrl(TftAssetUrlBuilder.buildChampionImageUrl(u.getCharacterId()))
                        .stars(u.getTier())
                        .itemImageUrls(List.of())
                        .build())
                .collect(Collectors.toList());

        List<SummonerMatchItem.ParticipantDto> participants = m.getParticipants().stream()
                .map(p -> SummonerMatchItem.ParticipantDto.builder()
                        .puuid(p.getPuuid())
                        .riotIdGameName(p.getRiotIdGameName())
                        .riotIdTagline(p.getRiotIdTagline())
                        .placement(p.getPlacement())
                        .stage(String.valueOf(p.getLevel()))
                        .traits(p.getTraits().stream()
                                .map(t -> SummonerMatchItem.TraitDto.builder()
                                        .traitId(t.getName())
                                        .name(t.getName())
                                        .iconUrl(TftAssetUrlBuilder.buildTraitIconUrl(t.getName()))
                                        .count(t.getNumUnits())
                                        .tone(styleToTone(t.getStyle()))
                                        .build())
                                .collect(Collectors.toList()))
                        .units(p.getUnits().stream()
                                .map(u -> SummonerMatchItem.UnitDto.builder()
                                        .characterId(u.getCharacterId())
                                        .imageUrl(TftAssetUrlBuilder.buildChampionImageUrl(u.getCharacterId()))
                                        .stars(u.getTier())
                                        .itemImageUrls(List.of())
                                        .build())
                                .collect(Collectors.toList()))
                        .playersEliminated(p.getPlayersEliminated())
                        .goldLeft(p.getGoldLeft())
                        .build())
                .collect(Collectors.toList());

        return SummonerMatchItem.builder()
                .matchId(m.getMatchId())
                .placement(m.getPlacement())
                .gameDateTime(m.getGameDatetime())
                .gameType(m.getQueueType())
                .compositionName("")
                .traits(traits)
                .units(units)
                .participants(participants)
                .build();
    }

    private List<SummonerDetailResponse.TopTraitDto> buildTopTraits(List<MatchSummaryResponse> matchList) {
        Map<String, Integer> gameCount = new LinkedHashMap<>();
        Map<String, Integer> totalPlace = new HashMap<>();
        Map<String, Integer> totalUnits = new HashMap<>();
        Map<String, Integer> totalStyle = new HashMap<>();

        for (MatchSummaryResponse match : matchList) {
            Set<String> seen = new HashSet<>();
            for (MatchSummaryResponse.TraitSummary trait : match.getTraits()) {
                if (trait.getStyle() > 0 && seen.add(trait.getName())) {
                    String key = trait.getName();
                    gameCount.merge(key, 1, Integer::sum);
                    totalPlace.merge(key, match.getPlacement(), Integer::sum);
                    totalUnits.merge(key, trait.getNumUnits(), Integer::sum);
                    totalStyle.merge(key, trait.getStyle(), Integer::sum);
                }
            }
        }

        return gameCount.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(5)
                .map(e -> {
                    String id = e.getKey();
                    int games = e.getValue();
                    double avg = (double) totalPlace.get(id) / games;
                    int count = Math.round((float) totalUnits.get(id) / games);
                    int style = Math.round((float) totalStyle.get(id) / games);
                    return SummonerDetailResponse.TopTraitDto.builder()
                            .traitId(id)
                            .name(id.replaceAll("(?i)^(TFT|Set)\\d+_", ""))
                            .count(count)
                            .iconUrl(TftAssetUrlBuilder.buildTraitIconUrl(id))
                            .tone(styleToTone(style))
                            .games(games)
                            .avgPlace(Math.round(avg * 10.0) / 10.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<SummonerDetailResponse.TopChampionDto> buildTopChampions(List<MatchSummaryResponse> matchList) {
        Map<String, Integer> gameCount = new LinkedHashMap<>();
        Map<String, Integer> totalPlace = new HashMap<>();
        Map<String, String> displayName = new HashMap<>();
        Map<String, Integer> rarityMap = new HashMap<>();

        for (MatchSummaryResponse match : matchList) {
            Set<String> seen = new HashSet<>();
            for (MatchSummaryResponse.UnitSummary unit : match.getUnits()) {
                if (seen.add(unit.getCharacterId())) {
                    String key = unit.getCharacterId();
                    gameCount.merge(key, 1, Integer::sum);
                    totalPlace.merge(key, match.getPlacement(), Integer::sum);
                    displayName.putIfAbsent(key, unit.getName().isBlank() ? key : unit.getName());
                    rarityMap.putIfAbsent(key, unit.getRarity());
                }
            }
        }

        return gameCount.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(5)
                .map(e -> {
                    String id = e.getKey();
                    int games = e.getValue();
                    double avg = (double) totalPlace.get(id) / games;
                    int cost = rarityCost(rarityMap.getOrDefault(id, 0));
                    return SummonerDetailResponse.TopChampionDto.builder()
                            .characterId(id)
                            .name(displayName.getOrDefault(id, id))
                            .imageUrl(TftAssetUrlBuilder.buildChampionImageUrl(id))
                            .cost(cost)
                            .games(games)
                            .avgPlace(Math.round(avg * 10.0) / 10.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static int rarityCost(int rarity) {
        return switch (rarity) {
            case 1 -> 2;
            case 2 -> 3;
            case 4 -> 4;
            case 6 -> 5;
            default -> 1;
        };
    }

    private static String styleToTone(int style) {
        return switch (style) {
            case 2 -> "silver";
            case 3 -> "gold";
            case 4 -> "prismatic";
            default -> "bronze";
        };
    }
}
