package com.tftgogo.domain.patchnote.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "관리자 패치 변경사항 생성/수정 요청")
public class AdminPatchChangeRequest {

    @Schema(description = "변경사항을 연결할 패치노트 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "패치노트 ID는 필수입니다.")
    private Long patchNoteId;

    @Schema(description = "변경 카테고리", example = "CHAMPION", allowableValues = {"CHAMPION", "TRAIT", "ITEM", "AUGMENT", "SYSTEM"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "변경 카테고리는 필수입니다.")
    @Size(max = 20, message = "변경 카테고리는 20자 이하로 입력해주세요.")
    private String category;

    @Schema(description = "변경 유형", example = "BUFF", allowableValues = {"BUFF", "NERF", "ADJUST", "NEW"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "변경 유형은 필수입니다.")
    @Size(max = 20, message = "변경 유형은 20자 이하로 입력해주세요.")
    private String type;

    @Schema(description = "게임에 미치는 영향도", example = "HIGH", allowableValues = {"HIGH", "MEDIUM", "LOW"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "영향도는 필수입니다.")
    @Size(max = 20, message = "영향도는 20자 이하로 입력해주세요.")
    private String impact;

    @Schema(description = "변경 대상의 내부 key. 챔피언/특성/아이템 식별에 사용합니다.", example = "TFT17_Jinx")
    @Size(max = 100, message = "대상 key는 100자 이하로 입력해주세요.")
    private String targetKey;

    @Schema(description = "사용자에게 보여줄 변경 대상 이름", example = "징크스", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "대상 이름은 필수입니다.")
    @Size(max = 100, message = "대상 이름은 100자 이하로 입력해주세요.")
    private String targetName;

    @Schema(description = "변경사항 요약", example = "스킬 피해량이 증가해 후반 캐리 안정성이 개선되었습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "변경 요약은 필수입니다.")
    @Size(max = 5000)
    private String summary;

    @Schema(description = "변경 전 수치 또는 설명", example = "스킬 피해량 220/330/500")
    @Size(max = 2000)
    private String beforeValue;

    @Schema(description = "변경 후 수치 또는 설명", example = "스킬 피해량 240/360/550")
    @Size(max = 2000)
    private String afterValue;

    @Schema(description = "변경 대상 이미지 URL", example = "https://raw.communitydragon.org/latest/game/assets/characters/tft17_jinx/skins/base/images/tft17_jinx_splash_tile_38.tft_set17.png")
    @Size(max = 500, message = "이미지 URL은 500자 이하로 입력해주세요.")
    private String imageUrl;

    @Schema(description = "변경사항 태그 목록", example = "[\"챔피언\", \"버프\", \"후반 캐리\"]")
    private List<@NotBlank(message = "태그는 빈 값으로 입력할 수 없습니다.") String> tags;

    @Schema(description = "패치 상세 화면에서 노출할 정렬 순서", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "정렬 순서는 필수입니다.")
    @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.")
    private Integer sortOrder;
}
