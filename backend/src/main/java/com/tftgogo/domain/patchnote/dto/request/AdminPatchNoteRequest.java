package com.tftgogo.domain.patchnote.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "관리자 패치노트 생성/수정 요청")
public class AdminPatchNoteRequest {

    @Schema(description = "패치 버전", example = "17.3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "패치 버전은 필수입니다.")
    @Size(max = 20, message = "패치 버전은 20자 이하로 입력해주세요.")
    private String version;

    @Schema(description = "패치노트 제목", example = "17.3 패치노트", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "패치노트 제목은 필수입니다.")
    @Size(max = 200, message = "패치노트 제목은 200자 이하로 입력해주세요.")
    private String title;

    @Schema(description = "패치노트 카드/목록에 노출할 짧은 요약", example = "캐리 챔피언과 핵심 시너지의 밸런스를 조정한 패치입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "패치노트 요약은 필수입니다.")
    private String summary;

    @Schema(description = "패치노트 상세 설명", example = "이번 패치는 17.3 메타에서 과도하게 강했던 조합을 낮추고, 사용률이 낮은 챔피언의 안정성을 보강하는 방향입니다.")
    private String description;

    @Schema(description = "이번 패치의 핵심 포커스", example = "챔피언 밸런스와 조합 전환 안정성 점검")
    @Size(max = 200, message = "패치노트 포커스는 200자 이하로 입력해주세요.")
    private String focus;

    @Schema(description = "패치노트 대표 이미지 URL", example = "https://raw.communitydragon.org/latest/game/assets/characters/tft17_jinx/skins/base/images/tft17_jinx_splash_tile_38.tft_set17.png")
    @Size(max = 500, message = "이미지 URL은 500자 이하로 입력해주세요.")
    private String imageUrl;

    @Schema(description = "패치노트 공개 일시", example = "2026-06-08T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "공개 일시는 필수입니다.")
    private LocalDateTime publishedAt;

    @Schema(description = "현재 패치 여부. true로 저장하면 기존 현재 패치는 해제됩니다.", example = "true")
    private boolean current;

    @Schema(description = "패치노트 주요 하이라이트 목록", example = "[\"챔피언 12건 밸런스 조정\", \"아이템 효율 일부 조정\", \"시너지 전환 안정성 개선\"]")
    private List<@NotBlank(message = "하이라이트는 빈 값으로 입력할 수 없습니다.") String> highlights;
}
