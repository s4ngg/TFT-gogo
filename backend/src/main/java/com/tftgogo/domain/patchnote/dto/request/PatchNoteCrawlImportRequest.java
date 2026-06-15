package com.tftgogo.domain.patchnote.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "패치노트 공식 크롤링 import 요청")
public class PatchNoteCrawlImportRequest {

    @Schema(description = "공식 Riot/TFT 패치노트 상세 URL. 비우면 태그 페이지에서 최신 상세 URL을 찾습니다.")
    @Size(max = 500, message = "sourceUrl은 500자 이하여야 합니다.")
    private String sourceUrl;

    @Schema(description = "명시 패치 버전. 비우면 제목 또는 URL slug에서 추출합니다.", example = "17.2")
    @Size(max = 20, message = "version은 20자 이하여야 합니다.")
    private String version;

    @Schema(description = "크롤링 locale. 비우면 ko-kr을 사용합니다.", example = "ko-kr")
    @Size(max = 20, message = "locale은 20자 이하여야 합니다.")
    private String locale;

    @Schema(description = "저장 없이 fetch/parse/중복 매칭만 확인합니다. 기본값은 true입니다.", example = "true")
    private Boolean dryRun;

    @Schema(description = "수동 수정된 import 데이터를 덮어쓸지 여부입니다. 기본값은 false입니다.", example = "false")
    private Boolean forceOverwrite;

    public boolean isDryRun() {
        return dryRun == null || dryRun;
    }

    public boolean isForceOverwrite() {
        return Boolean.TRUE.equals(forceOverwrite);
    }

    public String resolveLocale(String defaultLocale) {
        if (hasText(locale)) {
            return locale.trim();
        }
        return defaultLocale;
    }

    public String resolveVersion() {
        return hasText(version) ? version.trim() : null;
    }

    public String resolveSourceUrl() {
        return hasText(sourceUrl) ? sourceUrl.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
