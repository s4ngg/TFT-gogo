package com.tftgogo.domain.guide.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "Community Dragon 게임가이드 import 요청")
public class GuideCdragonImportRequest {

    @Schema(description = "저장할 패치 버전", example = "17.3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "패치 버전은 필수입니다.")
    @Size(max = 20, message = "패치 버전은 20자 이하여야 합니다.")
    private String patchVersion;

    @Schema(description = "TFT 세트 번호", example = "17")
    @Min(value = 1, message = "세트 번호는 1 이상이어야 합니다.")
    private Integer setNumber;

    @Schema(description = "CDragon setData mutator. 비우면 TFTSet{setNumber}를 사용합니다.", example = "TFTSet17")
    private String mutator;

    @Schema(description = "챔피언 가이드 import 여부. 비우면 true입니다.", example = "true")
    private Boolean includeChampions;

    @Schema(description = "특성/시너지 가이드 import 여부. 비우면 true입니다.", example = "true")
    private Boolean includeTraits;

    @Schema(description = "아이템 가이드 import 여부. 기본값은 false이며 명시적으로 true를 보내야 import합니다.", example = "true")
    private Boolean includeItems;

    @Schema(description = "증강체 가이드 import 여부. 기본값은 false이며 명시적으로 true를 보내야 import합니다.", example = "true")
    private Boolean includeAugments;

    public int resolveSetNumber() {
        return setNumber != null ? setNumber : 17;
    }

    public String resolveMutator() {
        if (mutator != null && !mutator.trim().isEmpty()) {
            return mutator.trim();
        }
        return "TFTSet" + resolveSetNumber();
    }

    public boolean shouldIncludeChampions() {
        return includeChampions == null || includeChampions;
    }

    public boolean shouldIncludeTraits() {
        return includeTraits == null || includeTraits;
    }

    public boolean shouldIncludeItems() {
        return Boolean.TRUE.equals(includeItems);
    }

    public boolean shouldIncludeAugments() {
        return Boolean.TRUE.equals(includeAugments);
    }
}
