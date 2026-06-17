package com.tftgogo.domain.patchnote.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminPatchNoteImportRequest {

    @Schema(description = "Official Riot patch note detail URL. If omitted, latest tag page item is imported.")
    @Size(max = 500)
    private String sourceUrl;

    @Schema(description = "Riot site locale. Defaults to crawler config default locale.")
    @Pattern(regexp = "^[a-z]{2}-[a-z]{2}$")
    private String locale;

    @Schema(description = "Version override used when the crawler cannot resolve version from title or URL.")
    @Size(max = 20)
    private String version;

    @Schema(description = "Whether the imported patch note should become the current patch. Defaults to true.")
    private Boolean current;

    public boolean shouldMarkCurrent() {
        return current == null || current;
    }
}
