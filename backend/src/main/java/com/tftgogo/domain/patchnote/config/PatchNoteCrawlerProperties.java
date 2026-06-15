package com.tftgogo.domain.patchnote.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.patch-note.crawler")
public class PatchNoteCrawlerProperties {

    private String tagUrl = "https://www.leagueoflegends.com/ko-kr/news/tags/teamfight-tactics-patch-notes/";
    private String defaultLocale = "ko-kr";
    private String userAgent = "TFT-gogo/1.0";
    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 10000;
    private int maxDetailRows = 200;
}
