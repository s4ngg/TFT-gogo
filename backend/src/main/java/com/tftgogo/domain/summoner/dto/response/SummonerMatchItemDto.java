package com.tftgogo.domain.summoner.dto.response;

import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Builder
public class SummonerMatchItemDto {
    private String matchId;
    private int placement;
    private long gameDateTime;
    private String gameType;
    private String compositionName;
    private List<TraitDto> traits;
    private List<UnitDto> units;
    private List<ParticipantDto> participants;

    public static SummonerMatchItemDto from(MatchSummaryResponse m,
                                            Function<String, String> traitIconFn,
                                            Function<String, String> itemIconFn) {
        return SummonerMatchItemDto.builder()
                .matchId(m.getMatchId())
                .placement(m.getPlacement())
                .gameDateTime(m.getGameDatetime())
                .gameType(m.getQueueType())
                .compositionName("")
                .traits(m.getTraits().stream()
                        .filter(t -> t.getStyle() > 0)
                        .map(t -> TraitDto.from(t, traitIconFn))
                        .collect(Collectors.toList()))
                .units(m.getUnits().stream()
                        .map(u -> UnitDto.from(u, itemIconFn))
                        .collect(Collectors.toList()))
                .participants(m.getParticipants().stream()
                        .map(p -> ParticipantDto.from(p, traitIconFn, itemIconFn))
                        .collect(Collectors.toList()))
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
    public static class TraitDto {
        private String traitId;
        private String name;
        private String iconUrl;
        private int count;
        private String tone;

        public static TraitDto from(MatchSummaryResponse.TraitSummary t, Function<String, String> iconFn) {
            return TraitDto.builder()
                    .traitId(t.getName())
                    .name(t.getName())
                    .iconUrl(iconFn.apply(t.getName()))
                    .count(t.getNumUnits())
                    .tone(styleToTone(t.getStyle()))
                    .build();
        }
    }

    @Getter
    @Builder
    public static class UnitDto {
        private String characterId;
        private String imageUrl;
        private int stars;
        private List<String> itemImageUrls;

        public static UnitDto from(MatchSummaryResponse.UnitSummary u, Function<String, String> itemIconFn) {
            return UnitDto.builder()
                    .characterId(u.getCharacterId())
                    .imageUrl(com.tftgogo.global.riot.util.TftAssetUrlBuilder.buildChampionImageUrl(u.getCharacterId()))
                    .stars(u.getTier())
                    .itemImageUrls(u.getItemNames().stream()
                            .map(itemIconFn)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .build();
        }
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

        public static ParticipantDto from(MatchSummaryResponse.ParticipantSummary p,
                                          Function<String, String> traitIconFn,
                                          Function<String, String> itemIconFn) {
            return ParticipantDto.builder()
                    .puuid(p.getPuuid())
                    .riotIdGameName(p.getRiotIdGameName())
                    .riotIdTagline(p.getRiotIdTagline())
                    .placement(p.getPlacement())
                    .stage(String.valueOf(p.getLastRound()))
                    .traits(p.getTraits().stream()
                            .map(t -> TraitDto.from(t, traitIconFn))
                            .collect(Collectors.toList()))
                    .units(p.getUnits().stream()
                            .map(u -> UnitDto.from(u, itemIconFn))
                            .collect(Collectors.toList()))
                    .playersEliminated(p.getPlayersEliminated())
                    .goldLeft(p.getGoldLeft())
                    .build();
        }
    }
}
