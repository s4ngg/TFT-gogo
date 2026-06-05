package com.tftgogo.domain.patchnote.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AdminPatchChangeRequest {

    @NotNull(message = "패치노트 ID는 필수입니다.")
    private Long patchNoteId;

    @NotBlank(message = "변경 카테고리는 필수입니다.")
    @Size(max = 20, message = "변경 카테고리는 20자 이하로 입력해주세요.")
    private String category;

    @NotBlank(message = "변경 유형은 필수입니다.")
    @Size(max = 20, message = "변경 유형은 20자 이하로 입력해주세요.")
    private String type;

    @NotBlank(message = "영향도는 필수입니다.")
    @Size(max = 20, message = "영향도는 20자 이하로 입력해주세요.")
    private String impact;

    @NotBlank(message = "대상 key는 필수입니다.")
    @Size(max = 100, message = "대상 key는 100자 이하로 입력해주세요.")
    private String targetKey;

    @NotBlank(message = "대상 이름은 필수입니다.")
    @Size(max = 100, message = "대상 이름은 100자 이하로 입력해주세요.")
    private String targetName;

    @NotBlank(message = "변경 요약은 필수입니다.")
    @Size(max = 500, message = "변경 요약은 500자 이하로 입력해주세요.")
    private String summary;

    @Size(max = 300, message = "변경 전 값은 300자 이하로 입력해주세요.")
    private String beforeValue;

    @Size(max = 300, message = "변경 후 값은 300자 이하로 입력해주세요.")
    private String afterValue;

    @Size(max = 500, message = "이미지 URL은 500자 이하로 입력해주세요.")
    private String imageUrl;

    private List<@NotBlank(message = "태그는 빈 값으로 입력할 수 없습니다.") String> tags;

    @NotNull(message = "정렬 순서는 필수입니다.")
    @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.")
    private Integer sortOrder;
}
