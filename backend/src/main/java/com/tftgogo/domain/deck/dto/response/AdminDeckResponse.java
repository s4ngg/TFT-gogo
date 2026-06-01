package com.tftgogo.domain.deck.dto.response;

import com.tftgogo.domain.deck.entity.DeckCuration;
import com.tftgogo.domain.deck.entity.DeckUnit;
import com.tftgogo.domain.deck.entity.MetaDeck;
import com.tftgogo.global.riot.util.TftAssetUrlBuilder;
import com.tftgogo.global.riot.util.TftShopUnitFilter;
import lombok.Builder;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;

@Getter
@Builder
public class AdminDeckResponse {

    private Long id;
    private String signature;
    private String rankFilter;

    // 자동 생성 이름
    private String autoName;
    // 관리자 지정 이름 (null = 자동 이름 사용)
    private String customName;
    // 실제 표시 이름
    private String displayName;

    private boolean hidden;
    private Integer sortPriority;
    private String curatorNote;
    // 관리자 배치판 포지션 JSON (null = 자동 배치)
    private String boardPositions;

    private String grade;
    private String winRate;
    private String top4;
    private String pickRate;
    private int sampleSize;

    // 배치판 편집 UI용 챔피언 목록
    private List<UnitInfo> units;

    @Getter
    @Builder
    public static class UnitInfo {
        private String characterId;
        private String name;
        private String imageUrl;
    }

    public static AdminDeckResponse from(MetaDeck deck, DeckCuration curation) {
        String customName = curation != null ? curation.getCustomName() : null;
        boolean hidden = curation != null && curation.isHidden();
        Integer sortPriority = curation != null ? curation.getSortPriority() : null;
        String curatorNote = curation != null ? curation.getCuratorNote() : null;
        String boardPositions = curation != null ? curation.getBoardPositions() : null;

        List<UnitInfo> units = deck.getUnits().stream()
                .filter(u -> TftShopUnitFilter.isShopUnit(u.getCharacterId()))
                .sorted(Comparator.comparingInt(DeckUnit::getCost))
                .map(u -> UnitInfo.builder()
                        .characterId(u.getCharacterId())
                        .name(u.getChampionName())
                        .imageUrl(TftAssetUrlBuilder.buildChampionImageUrl(u.getCharacterId()))
                        .build())
                .toList();

        return AdminDeckResponse.builder()
                .id(deck.getId())
                .signature(deck.getSignature())
                .rankFilter(deck.getRankFilter().name())
                .autoName(deck.getName())
                .customName(customName)
                .displayName(customName != null && !customName.isBlank() ? customName : deck.getName())
                .hidden(hidden)
                .sortPriority(sortPriority)
                .curatorNote(curatorNote)
                .boardPositions(boardPositions)
                .grade(deck.getTier())
                .winRate(String.format("%.1f%%", deck.getWinRate()))
                .top4(String.format("%.1f%%", deck.getTop4Rate()))
                .pickRate(String.format("%.1f%%", deck.getPlayRate()))
                .sampleSize(deck.getSampleSize())
                .units(units)
                .build();
    }
}
