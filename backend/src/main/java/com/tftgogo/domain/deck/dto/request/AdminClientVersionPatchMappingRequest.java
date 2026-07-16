package com.tftgogo.domain.deck.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "클라이언트 버전-패치 번호 매핑 생성/수정 요청")
public class AdminClientVersionPatchMappingRequest {

    @Schema(description = "Riot 클라이언트 빌드 버전", example = "16.13", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "클라이언트 버전은 필수입니다.")
    @Size(max = 20, message = "클라이언트 버전은 20자 이하로 입력해주세요.")
    private String clientVersion;

    @Schema(description = "TFT 패치 번호", example = "17.6", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "패치 번호는 필수입니다.")
    @Size(max = 20, message = "패치 번호는 20자 이하로 입력해주세요.")
    private String patchVersion;
}
