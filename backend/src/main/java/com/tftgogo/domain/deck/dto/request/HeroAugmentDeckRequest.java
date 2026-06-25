package com.tftgogo.domain.deck.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HeroAugmentDeckRequest {

    @NotBlank
    private String name;

    private String description;
    private String champions;
    private String traits;
    private String boardPositions;
    private String heroAugments;
    private boolean recommended;
    private int sortOrder;
    private String grade;
}
