package com.tftgogo.domain.guide.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.tftgogo.domain.guide.entity.GuideType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminGuideRequest {

    @NotNull(message = "게임가이드 타입은 필수입니다.")
    private GuideType guideType;

    @NotBlank(message = "대상 키는 필수입니다.")
    @Size(max = 100, message = "대상 키는 100자 이하여야 합니다.")
    private String targetKey;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
    private String name;

    @Size(max = 5000, message = "요약은 5000자 이하여야 합니다.")
    private String summary;

    @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
    private String imageUrl;

    @NotNull(message = "dataJson은 필수입니다.")
    private JsonNode dataJson;

    @NotBlank(message = "패치 버전은 필수입니다.")
    @Size(max = 20, message = "패치 버전은 20자 이하여야 합니다.")
    private String patchVersion;

    @NotNull(message = "정렬 순서는 필수입니다.")
    @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.")
    private Integer sortOrder;

    private Boolean active;

    public boolean resolveActive(boolean defaultValue) {
        return active != null ? active : defaultValue;
    }
}
