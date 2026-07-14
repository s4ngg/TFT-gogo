package com.tftgogo.domain.patchnote.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
@ConfigurationProperties(prefix = "app.patch-note.crawler")
public class PatchNoteCrawlerProperties {

    @NotBlank
    private String tagUrl = "https://teamfighttactics.leagueoflegends.com/ko-kr/news/tags/patch-notes/";

    @NotBlank
    @Pattern(regexp = "^[a-z]{2}-[a-z]{2}$")
    private String defaultLocale = "ko-kr";

    @NotBlank
    private String userAgent = "TFT-gogo/1.0";

    @Positive
    private int connectTimeoutMillis = 5000;

    @Positive
    private int readTimeoutMillis = 10000;

    @Positive
    private int maxDetailRows = 1000;

    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax("1.0")
    private double minRetainedRowRatio = 0.5;
}
