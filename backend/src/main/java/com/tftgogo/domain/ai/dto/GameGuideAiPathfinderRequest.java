package com.tftgogo.domain.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GameGuideAiPathfinderRequest {

    @NotBlank
    @Size(max = 20)
    private String patchVersion;

    @NotBlank
    @Pattern(regexp = "traits|items|augments|champions")
    private String activeTab;

    @NotBlank
    @Pattern(regexp = "AUTO")
    private String mode;

    @Valid
    @Size(max = 5)
    private List<GuideRefDto> selectedRefs;

    @Valid
    @Size(max = 20)
    private List<GuideRefDto> candidateRefs;

    @NotBlank
    @Size(max = 500)
    private String question;

    public GameGuideAiPathfinderRequest(
            String patchVersion,
            String activeTab,
            String mode,
            List<GuideRefDto> selectedRefs,
            String question
    ) {
        this(patchVersion, activeTab, mode, selectedRefs, List.of(), question);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuideRefDto {
        @NotBlank
        @Pattern(regexp = "TRAIT|ITEM|AUGMENT|CHAMPION")
        private String guideType;

        @NotBlank
        @Size(max = 120)
        private String targetKey;

        @Size(max = 100)
        private String name;
    }
}
