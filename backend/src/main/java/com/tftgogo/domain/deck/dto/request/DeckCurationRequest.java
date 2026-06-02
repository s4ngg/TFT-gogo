package com.tftgogo.domain.deck.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeckCurationRequest {
    private String customName;      // null 허용 — null이면 자동 이름 사용
    private boolean hidden;
    private Integer sortPriority;   // null 허용 — null이면 기본 정렬
    private String curatorNote;
    private String boardPositions;  // JSON: {"imageUrl": {"row":N,"col":N}}
    private String playGuide;       // JSON: {"early":"...","mid":"...","late":"..."}
    private String heroAugments;    // JSON: [{"championId":"tft17_jinx","championName":"징크스","augmentName":"화약 소녀"}]
}
