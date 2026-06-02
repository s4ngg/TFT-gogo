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

import java.util.List;
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
        List<SummonerMatchItem> matches = summonerService.getMatchesByRiotId(gameName, tagLine).stream()
                .map(this::toMatchItem)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("매치 조회 성공", matches));
    }

    private SummonerDetailResponse buildDetail(MatchSearchResponse result) {
        var profile = result.getProfile();
        var rankInfo = result.getRankInfo();
        return SummonerDetailResponse.builder()
                .puuid(profile.getPuuid())
                .gameName(profile.getGameName())
                .tagLine(profile.getTagLine())
                .profileIconId(profile.getProfileIconId())
                .summonerLevel(profile.getSummonerLevel())
                .tier(rankInfo.isUnranked() ? null : rankInfo.getTier())
                .rank(rankInfo.isUnranked() ? null : rankInfo.getRank())
                .leaguePoints(rankInfo.isUnranked() ? 0 : rankInfo.getLeaguePoints())
                .wins(rankInfo.isUnranked() ? 0 : rankInfo.getWins())
                .losses(rankInfo.isUnranked() ? 0 : rankInfo.getLosses())
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

    private static String styleToTone(int style) {
        return switch (style) {
            case 2 -> "silver";
            case 3 -> "gold";
            case 4 -> "prismatic";
            default -> "bronze";
        };
    }
}
