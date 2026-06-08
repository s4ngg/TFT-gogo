package com.tftgogo.domain.guide.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.tftgogo.domain.guide.entity.GuideType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "관리자 게임가이드 생성/수정 요청")
public class AdminGuideRequest {

    @Schema(description = "게임가이드 타입", example = "CHAMPION", allowableValues = {"TRAIT", "ITEM", "AUGMENT", "CHAMPION"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "게임가이드 타입은 필수입니다.")
    private GuideType guideType;

    @Schema(description = "가이드 대상 내부 key. 챔피언/특성/아이템/증강체 식별에 사용합니다.", example = "TFT17_Jinx", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "대상 키는 필수입니다.")
    @Size(max = 100, message = "대상 키는 100자 이하여야 합니다.")
    private String targetKey;

    @Schema(description = "사용자에게 보여줄 가이드 대상 이름", example = "징크스", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
    private String name;

    @Schema(description = "가이드 요약 설명", example = "후반 화력이 강한 4코스트 캐리 챔피언입니다. 공속과 치명타 아이템 효율이 좋습니다.")
    @Size(max = 5000, message = "요약은 5000자 이하여야 합니다.")
    private String summary;

    @Schema(description = "가이드 대표 이미지 URL", example = "https://raw.communitydragon.org/latest/game/assets/characters/tft17_jinx/skins/base/images/tft17_jinx_splash_tile_38.tft_set17.png")
    @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
    private String imageUrl;

    @Schema(description = "탭별 상세 표시와 정렬에 사용할 JSON object. 문자열이 아니라 객체 형태로 입력해야 합니다.", example = "{\"cost\":4,\"traits\":[\"사수\",\"전략가\"],\"avgPlace\":\"4.12\",\"pickRate\":\"12.4%\",\"top4\":\"54.1%\",\"winRate\":\"13.2%\",\"tips\":[\"공속 아이템을 우선합니다\",\"후반 캐리 포지션을 보호합니다\"]}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "dataJson은 필수입니다.")
    private JsonNode dataJson;

    @Schema(description = "패치 버전", example = "17.3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "패치 버전은 필수입니다.")
    @Size(max = 20, message = "패치 버전은 20자 이하여야 합니다.")
    private String patchVersion;

    @Schema(description = "가이드 목록에서 노출할 정렬 순서", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "정렬 순서는 필수입니다.")
    @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.")
    private Integer sortOrder;

    @Schema(description = "공개 조회 노출 여부. 생성 시 생략하면 true로 저장됩니다.", example = "true")
    private Boolean active;

    public boolean resolveActive(boolean defaultValue) {
        return active != null ? active : defaultValue;
    }
}
