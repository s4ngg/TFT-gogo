package com.tftgogo.domain.deck.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeckCurationRequest {

    @Size(max = 200, message = "덱 이름은 200자 이하여야 합니다.")
    private String customName;      // null 허용 — null이면 자동 이름 사용

    private boolean hidden;

    @Min(value = 1, message = "정렬 우선순위는 1 이상이어야 합니다.")
    private Integer sortPriority;   // null 허용 — null이면 기본 정렬

    @Size(max = 1000, message = "관리자 메모는 1000자 이하여야 합니다.")
    private String curatorNote;

    @Size(max = 50000, message = "배치판 데이터가 너무 큽니다.")
    private String boardPositions;  // JSON: {"imageUrl": {"row":N,"col":N}}

    @Size(max = 10000, message = "운영 가이드 데이터가 너무 큽니다.")
    private String playGuide;       // JSON: {"early":"...","mid":"...","late":"..."}

    @Size(max = 10000, message = "영웅 증강 데이터가 너무 큽니다.")
    private String heroAugments;    // JSON: [{"championId":"tft17_jinx","championName":"징크스","augmentName":"화약 소녀"}]
}
