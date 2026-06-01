package com.tftgogo.domain.deck.dto.response;

import com.tftgogo.domain.deck.entity.DeckCuration;
import com.tftgogo.domain.deck.entity.MetaDeck;
import lombok.Builder;
import lombok.Getter;

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

    private String grade;
    private String winRate;
    private String top4;
    private String pickRate;
    private int sampleSize;

    public static AdminDeckResponse from(MetaDeck deck, DeckCuration curation) {
        String customName = curation != null ? curation.getCustomName() : null;
        boolean hidden = curation != null && curation.isHidden();
        Integer sortPriority = curation != null ? curation.getSortPriority() : null;
        String curatorNote = curation != null ? curation.getCuratorNote() : null;

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
                .grade(deck.getTier())
                .winRate(String.format("%.1f%%", deck.getWinRate()))
                .top4(String.format("%.1f%%", deck.getTop4Rate()))
                .pickRate(String.format("%.1f%%", deck.getPlayRate()))
                .sampleSize(deck.getSampleSize())
                .build();
    }
}
