package com.tftgogo.domain.patchnote.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.patch-note.scheduler")
public class PatchNoteImportSchedulerProperties {

    private boolean enabled = false;
    private boolean startupImport = false;

    @NotBlank
    @Pattern(regexp = "^[a-z]{2}-[a-z]{2}$")
    private String locale = "ko-kr";

    private boolean current = true;

    @Positive
    private int listScanLimit = 5;

    @NotBlank
    private String listCron = "0 0 * * * *";

    @NotBlank
    private String refreshCron = "0 30 6 * * *";

    @NotBlank
    private String zone = "Asia/Seoul";
}
