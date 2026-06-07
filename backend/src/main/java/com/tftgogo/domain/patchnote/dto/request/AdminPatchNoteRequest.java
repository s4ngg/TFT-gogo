package com.tftgogo.domain.patchnote.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class AdminPatchNoteRequest {

    @NotBlank(message = "패치 버전은 필수입니다.")
    @Size(max = 20, message = "패치 버전은 20자 이하로 입력해주세요.")
    private String version;

    @NotBlank(message = "패치노트 제목은 필수입니다.")
    @Size(max = 150, message = "패치노트 제목은 150자 이하로 입력해주세요.")
    private String title;

    @NotBlank(message = "패치노트 요약은 필수입니다.")
    @Size(max = 500, message = "패치노트 요약은 500자 이하로 입력해주세요.")
    private String summary;

    private String description;

    @Size(max = 200, message = "패치노트 포커스는 200자 이하로 입력해주세요.")
    private String focus;

    @Size(max = 500, message = "이미지 URL은 500자 이하로 입력해주세요.")
    private String imageUrl;

    @NotNull(message = "공개 일시는 필수입니다.")
    private LocalDateTime publishedAt;

    private boolean current;

    private List<@NotBlank(message = "하이라이트는 빈 값으로 입력할 수 없습니다.") String> highlights;
}
