package com.tftgogo.domain.match.dto.response;

import com.tftgogo.global.riot.dto.MatchDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@Schema(description = "매치 상세 응답 — 8인 전체 참가자 완전 데이터")
public class MatchDetailResponse {

    @Schema(description = "Riot 매치 ID", example = "KR_7654321098", requiredMode = Schema.RequiredMode.REQUIRED)
    private String matchId;

    @Schema(description = "게임 시작 시각 (epoch ms)", example = "1717200000000", requiredMode = Schema.RequiredMode.REQUIRED)
    private long gameDatetime;

    @Schema(description = "게임 총 시간 (초)", example = "1834.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private float gameLength;

    @Schema(description = "원본 패치 버전 문자열", example = "Version 14.12.639.9834 (Sep 10 2024/11:11:11) [PUBLIC] <Releases/14.12>", requiredMode = Schema.RequiredMode.REQUIRED)
    private String gameVersion;

    @Schema(description = "큐 타입 레이블 (queue_id 기반 변환: 1090=NORMAL, 1100=RANKED)", example = "RANKED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String queueType;

    @Schema(description = "TFT 세트 번호", example = "13", requiredMode = Schema.RequiredMode.REQUIRED)
    private int tftSetNumber;

    @Schema(description = "세트 내부 코어명", example = "TFTSet13", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tftSetCoreName;

    @Schema(description = "게임 타입", example = "standard", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tftGameType;

    @Schema(description = "8인 전체 참가자 목록 (순위순 정렬)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ParticipantDetail> participants;

    public static MatchDetailResponse of(String matchId, MatchDto matchDto) {
        MatchDto.MatchInfoDto info = matchDto.getInfo();
        return MatchDetailResponse.builder()
                .matchId(matchId)
                .gameDatetime(info.getGame_datetime())
                .gameLength(info.getGame_length())
                .gameVersion(info.getGame_version())
                .queueType(info.getQueue_id() == 1100 ? "RANKED" : "NORMAL")
                .tftSetNumber(info.getTft_set_number())
                .tftSetCoreName(info.getTft_set_core_name())
                .tftGameType(info.getTft_game_type())
                .participants(info.getParticipants() == null ? List.of() :
                        info.getParticipants().stream()
                                .map(ParticipantDetail::of)
                                .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    @Schema(description = "참가자 전체 데이터")
    public static class ParticipantDetail {

        @Schema(description = "참가자 PUUID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String puuid;

        @Schema(description = "참가자 게임 이름", example = "Faker", requiredMode = Schema.RequiredMode.REQUIRED)
        private String riotIdGameName;

        @Schema(description = "참가자 태그라인", example = "KR1", requiredMode = Schema.RequiredMode.REQUIRED)
        private String riotIdTagline;

        @Schema(description = "최종 순위 (1~8). 4 이하=승, 5 이상=패", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        private int placement;

        @Schema(description = "소환사 레벨", example = "9", requiredMode = Schema.RequiredMode.REQUIRED)
        private int level;

        @Schema(description = "마지막 라운드", example = "27", requiredMode = Schema.RequiredMode.REQUIRED)
        private int lastRound;

        @Schema(description = "남은 골드", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private int goldLeft;

        @Schema(description = "처치한 플레이어 수", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private int playersEliminated;

        @Schema(description = "탈락 시각 (초)", example = "1834.5", requiredMode = Schema.RequiredMode.REQUIRED)
        private float timeEliminated;

        @Schema(description = "플레이어에게 준 총 피해량", example = "48", requiredMode = Schema.RequiredMode.REQUIRED)
        private int totalDamageToPlayers;

        @Schema(description = "활성화된 특성 목록 (style > 0)", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<TraitDetail> traits;

        @Schema(description = "배치된 유닛 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<UnitDetail> units;

        public static ParticipantDetail of(MatchDto.ParticipantDto p) {
            return ParticipantDetail.builder()
                    .puuid(p.getPuuid())
                    .riotIdGameName(p.getRiotIdGameName())
                    .riotIdTagline(p.getRiotIdTagline())
                    .placement(p.getPlacement())
                    .level(p.getLevel())
                    .lastRound(p.getLast_round())
                    .goldLeft(p.getGold_left())
                    .playersEliminated(p.getPlayers_eliminated())
                    .timeEliminated(p.getTime_eliminated())
                    .totalDamageToPlayers(p.getTotal_damage_to_players())
                    .traits(p.getTraits() == null ? List.of() :
                            p.getTraits().stream()
                                    .filter(t -> t.getStyle() > 0)
                                    .map(t -> TraitDetail.builder()
                                            .name(t.getName())
                                            .numUnits(t.getNum_units())
                                            .style(t.getStyle())
                                            .tierCurrent(t.getTier_current())
                                            .tierTotal(t.getTier_total())
                                            .build())
                                    .collect(Collectors.toList()))
                    .units(p.getUnits() == null ? List.of() :
                            p.getUnits().stream()
                                    .map(u -> UnitDetail.builder()
                                            .characterId(u.getCharacter_id())
                                            .name(u.getName())
                                            .tier(u.getTier())
                                            .rarity(u.getRarity())
                                            .itemNames(u.getItemNames() == null ? List.of() : u.getItemNames())
                                            .build())
                                    .collect(Collectors.toList()))
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "특성 상세")
    public static class TraitDetail {

        @Schema(description = "특성 내부 키", example = "Set13_Challenger", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @Schema(description = "해당 특성 유닛 배치 수", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        private int numUnits;

        @Schema(description = "활성화 단계 스타일 (1=브론즈, 2=실버, 3=골드, 4=프리즈매틱)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        private int style;

        @Schema(description = "현재 활성화 단계", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private int tierCurrent;

        @Schema(description = "최대 활성화 단계", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        private int tierTotal;
    }

    @Getter
    @Builder
    @Schema(description = "유닛 상세")
    public static class UnitDetail {

        @Schema(description = "유닛 내부 캐릭터 ID", example = "TFT13_Jinx", requiredMode = Schema.RequiredMode.REQUIRED)
        private String characterId;

        @Schema(description = "유닛 표시 이름", example = "Jinx", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @Schema(description = "유닛 성급 (1~3)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private int tier;

        @Schema(description = "유닛 코스트 등급 (0=1코, 1=2코, 2=3코, 4=4코, 6=5코)", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        private int rarity;

        @Schema(description = "장착 아이템 이름 배열", example = "[\"Infinity Edge\",\"Bloodthirster\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<String> itemNames;
    }
}
