package com.tftgogo.domain.match.dto.response;

import com.tftgogo.global.riot.dto.MatchDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@Schema(description = "매치 목록 단건 응답 (5.3 매치 목록 영역 — 검색된 소환사 기준)")
public class MatchSummaryResponse {

    @Schema(description = "Riot 매치 ID", example = "KR_7654321098", requiredMode = Schema.RequiredMode.REQUIRED)
    private String matchId;

    @Schema(description = "게임 시작 시각 (epoch milliseconds, InfoDto.game_datetime)", example = "1717200000000", requiredMode = Schema.RequiredMode.REQUIRED)
    private long gameDatetime;

    @Schema(description = "게임 총 시간 (초, InfoDto.game_length)", example = "1834.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private float gameLength;

    @Schema(description = "원본 패치 버전 문자열 (InfoDto.game_version)", example = "Version 14.12.639.9834 (Sep 10 2024/11:11:11) [PUBLIC] <Releases/14.12>", requiredMode = Schema.RequiredMode.REQUIRED)
    private String gameVersion;

    @Schema(description = "큐 타입 레이블 (queue_id 기반 변환: 1090=NORMAL, 1100=RANKED)", example = "RANKED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String queueType;

    @Schema(description = "검색된 소환사의 최종 순위 1~8 (ParticipantDto.placement). 4 이하=승, 5 이상=패", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private int placement;

    @Schema(description = "소환사 레벨 (ParticipantDto.level)", example = "9", requiredMode = Schema.RequiredMode.REQUIRED)
    private int level;

    @Schema(description = "마지막 라운드 (ParticipantDto.last_round)", example = "27", requiredMode = Schema.RequiredMode.REQUIRED)
    private int lastRound;

    @Schema(description = "남은 골드 (ParticipantDto.gold_left)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private int goldLeft;

    @Schema(description = "처치한 플레이어 수 (ParticipantDto.players_eliminated)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private int playersEliminated;

    @Schema(description = "탈락 시각 (초, ParticipantDto.time_eliminated)", example = "1834.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private float timeEliminated;

    @Schema(description = "플레이어에게 준 총 피해량 (ParticipantDto.total_damage_to_players)", example = "48", requiredMode = Schema.RequiredMode.REQUIRED)
    private int totalDamageToPlayers;

    @Schema(description = "활성화된 특성 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<TraitSummary> traits;

    @Schema(description = "배치된 유닛 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<UnitSummary> units;

    @Schema(description = "같은 게임의 모든 참가자 목록 (8명)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ParticipantSummary> participants;

    public static MatchSummaryResponse of(String matchId, MatchDto.MatchInfoDto info, MatchDto.ParticipantDto participant) {
        return MatchSummaryResponse.builder()
                .matchId(matchId)
                .gameDatetime(info.getGame_datetime())
                .gameLength(info.getGame_length())
                .gameVersion(info.getGame_version())
                .queueType(info.getQueue_id() == 1100 ? "RANKED" : "NORMAL")
                .placement(participant.getPlacement())
                .level(participant.getLevel())
                .lastRound(participant.getLast_round())
                .goldLeft(participant.getGold_left())
                .playersEliminated(participant.getPlayers_eliminated())
                .timeEliminated(participant.getTime_eliminated())
                .totalDamageToPlayers(participant.getTotal_damage_to_players())
                .traits(participant.getTraits().stream()
                        .map(t -> TraitSummary.builder()
                                .name(t.getName())
                                .numUnits(t.getNum_units())
                                .style(t.getStyle())
                                .tierCurrent(t.getTier_current())
                                .tierTotal(t.getTier_total())
                                .build())
                        .collect(Collectors.toList()))
                .units(participant.getUnits().stream()
                        .map(u -> UnitSummary.builder()
                                .characterId(u.getCharacter_id())
                                .name(u.getName())
                                .tier(u.getTier())
                                .rarity(u.getRarity())
                                .itemNames(u.getItemNames() == null ? List.of() : u.getItemNames())
                                .build())
                        .collect(Collectors.toList()))
                .participants(info.getParticipants() == null ? List.of() :
                        info.getParticipants().stream()
                                .map(p -> ParticipantSummary.builder()
                                        .puuid(p.getPuuid())
                                        .riotIdGameName(p.getRiotIdGameName())
                                        .riotIdTagline(p.getRiotIdTagline())
                                        .placement(p.getPlacement())
                                        .level(p.getLevel())
                                        .lastRound(p.getLast_round())
                                        .goldLeft(p.getGold_left())
                                        .playersEliminated(p.getPlayers_eliminated())
                                        .traits(p.getTraits() == null ? List.of() :
                                                p.getTraits().stream()
                                                        .filter(t -> t.getStyle() > 0)
                                                        .map(t -> TraitSummary.builder()
                                                                .name(t.getName())
                                                                .numUnits(t.getNum_units())
                                                                .style(t.getStyle())
                                                                .tierCurrent(t.getTier_current())
                                                                .tierTotal(t.getTier_total())
                                                                .build())
                                                        .collect(Collectors.toList()))
                                        .units(p.getUnits() == null ? List.of() :
                                                p.getUnits().stream()
                                                        .map(u -> UnitSummary.builder()
                                                                .characterId(u.getCharacter_id())
                                                                .name(u.getName())
                                                                .tier(u.getTier())
                                                                .rarity(u.getRarity())
                                                                .itemNames(u.getItemNames() == null ? List.of() : u.getItemNames())
                                                                .build())
                                                        .collect(Collectors.toList()))
                                        .build())
                                .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    @Schema(description = "특성 요약")
    public static class TraitSummary {

        @Schema(description = "특성 내부 키 (TraitDto.name)", example = "Set13_Challenger", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @Schema(description = "해당 특성 유닛 배치 수 (TraitDto.num_units)", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        private int numUnits;

        @Schema(description = "활성화 단계 스타일 (0=비활성, 1=브론즈, 2=실버, 3=골드, 4=프리즈매틱, TraitDto.style)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        private int style;

        @Schema(description = "현재 활성화 단계 (TraitDto.tier_current)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private int tierCurrent;

        @Schema(description = "최대 활성화 단계 (TraitDto.tier_total)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        private int tierTotal;
    }

    @Getter
    @Builder
    @Schema(description = "유닛 요약")
    public static class UnitSummary {

        @Schema(description = "유닛 내부 캐릭터 ID (UnitDto.character_id)", example = "TFT13_Jinx", requiredMode = Schema.RequiredMode.REQUIRED)
        private String characterId;

        @Schema(description = "유닛 표시 이름 (UnitDto.name)", example = "Jinx", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @Schema(description = "유닛 성급 1~3 (UnitDto.tier)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private int tier;

        @Schema(description = "유닛 등급(코스트) — 0=1코, 1=2코, 2=3코, 4=4코, 6=5코 (UnitDto.rarity)", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        private int rarity;

        @Schema(description = "장착 아이템 이름 배열 (UnitDto.itemNames)", example = "[\"Infinity Edge\",\"Bloodthirster\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<String> itemNames;
    }

    @Getter
    @Builder
    @Schema(description = "같은 게임의 참가자 요약")
    public static class ParticipantSummary {

        @Schema(description = "참가자 PUUID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String puuid;

        @Schema(description = "참가자 게임 이름", example = "Faker", requiredMode = Schema.RequiredMode.REQUIRED)
        private String riotIdGameName;

        @Schema(description = "참가자 태그라인", example = "KR1", requiredMode = Schema.RequiredMode.REQUIRED)
        private String riotIdTagline;

        @Schema(description = "최종 순위 (1~8)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        private int placement;

        @Schema(description = "소환사 레벨", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
        private int level;

        @Schema(description = "마지막 라운드", example = "27", requiredMode = Schema.RequiredMode.REQUIRED)
        private int lastRound;

        @Schema(description = "남은 골드", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private int goldLeft;

        @Schema(description = "처치한 플레이어 수", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private int playersEliminated;

        @Schema(description = "활성화된 특성 목록 (style > 0)", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<TraitSummary> traits;

        @Schema(description = "배치된 유닛 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<UnitSummary> units;
    }
}
