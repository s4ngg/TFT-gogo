package com.tftgogo.global.riot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "riot")
public class RiotProperties {

    private String apiKey;
    private String krBaseUrl = "https://kr.api.riotgames.com";
    private String asiaBaseUrl = "https://asia.api.riotgames.com";
}
