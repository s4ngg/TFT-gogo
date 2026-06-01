package com.tftgogo.domain.match.controller;

import com.tftgogo.domain.match.dto.response.MatchSearchResponse;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.domain.match.service.SummonerService;
import com.tftgogo.global.response.ApiResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/summoners")
@RequiredArgsConstructor
public class SummonerController {

    private final SummonerService summonerService;

    @GetMapping("/{gameName}/{tagLine}")
    public ResponseEntity<ApiResponse<SummonerDetailDto>> getSummoner(
            @PathVariable("gameName") String gameName,
            @PathVariable("tagLine") String tagLine) {
        MatchSearchResponse result = summonerService.search(gameName, tagLine);
        return ResponseEntity.ok(ApiResponse.success("소환사 조회 성공", buildDetail(result)));
    }

    @GetMapping("/{gameName}/{tagLine}/matches")
    public ResponseEntity<ApiResponse<List<FrontendMatchDto>>> getMatches(
            @PathVariable("gameName") String gameName,
            @PathVariable("tagLine") String tagLine) {
        MatchSearchResponse result = summonerService.search(gameName, tagLine);
        List<FrontendMatchDto> matches = result.getMatchList().stream()
                .map(this::toFrontendMatch)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("매치 조회 성공", matches));
    }

    private SummonerDetailDto buildDetail(MatchSearchResponse result) {
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

        return SummonerDetailDto.builder()
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
                .topTraits(List.of())
                .topChampions(List.of())
                .build();
    }

    private FrontendMatchDto toFrontendMatch(MatchSummaryResponse m) {
        List<FrontendMatchDto.TraitDto> traits = m.getTraits().stream()
                .filter(t -> t.getStyle() > 0)
                .map(t -> FrontendMatchDto.TraitDto.builder()
                        .traitId(t.getName())
                        .name(t.getName())
                        .iconUrl("")
                        .count(t.getNumUnits())
                        .tone(styleToTone(t.getStyle()))
                        .build())
                .collect(Collectors.toList());

        List<FrontendMatchDto.UnitDto> units = m.getUnits().stream()
                .map(u -> FrontendMatchDto.UnitDto.builder()
                        .characterId(u.getCharacterId())
                        .imageUrl("")
                        .stars(u.getTier())
                        .itemImageUrls(List.of())
                        .build())
                .collect(Collectors.toList());

        List<FrontendMatchDto.ParticipantDto> participants = m.getParticipants().stream()
                .map(p -> FrontendMatchDto.ParticipantDto.builder()
                        .puuid(p.getPuuid())
                        .riotIdGameName(p.getRiotIdGameName())
                        .riotIdTagline(p.getRiotIdTagline())
                        .placement(p.getPlacement())
                        .stage(String.valueOf(p.getLevel()))
                        .traits(p.getTraits().stream()
                                .map(t -> FrontendMatchDto.TraitDto.builder()
                                        .traitId(t.getName())
                                        .name(t.getName())
                                        .iconUrl("")
                                        .count(t.getNumUnits())
                                        .tone(styleToTone(t.getStyle()))
                                        .build())
                                .collect(Collectors.toList()))
                        .units(p.getUnits().stream()
                                .map(u -> FrontendMatchDto.UnitDto.builder()
                                        .characterId(u.getCharacterId())
                                        .imageUrl("")
                                        .stars(u.getTier())
                                        .itemImageUrls(List.of())
                                        .build())
                                .collect(Collectors.toList()))
                        .playersEliminated(p.getPlayersEliminated())
                        .goldLeft(p.getGoldLeft())
                        .build())
                .collect(Collectors.toList());

        return FrontendMatchDto.builder()
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

    @Getter
    @Builder
    public static class SummonerDetailDto {
        private String puuid;
        private String gameName;
        private String tagLine;
        private int profileIconId;
        private long summonerLevel;
        private String tier;
        private String rank;
        private int leaguePoints;
        private int wins;
        private int losses;
        private double avgPlace;
        private double top4Rate;
        private int[] rankDistribution;
        private List<Object> topTraits;
        private List<Object> topChampions;
    }

    @Getter
    @Builder
    public static class FrontendMatchDto {
        private String matchId;
        private int placement;
        private long gameDateTime;
        private String gameType;
        private String compositionName;
        private List<TraitDto> traits;
        private List<UnitDto> units;
        private List<ParticipantDto> participants;

        @Getter
        @Builder
        public static class TraitDto {
            private String traitId;
            private String name;
            private String iconUrl;
            private int count;
            private String tone;
        }

        @Getter
        @Builder
        public static class UnitDto {
            private String characterId;
            private String imageUrl;
            private int stars;
            private List<String> itemImageUrls;
        }

        @Getter
        @Builder
        public static class ParticipantDto {
            private String puuid;
            private String riotIdGameName;
            private String riotIdTagline;
            private int placement;
            private String stage;
            private List<TraitDto> traits;
            private List<UnitDto> units;
            private int playersEliminated;
            private int goldLeft;
        }
    }
}
