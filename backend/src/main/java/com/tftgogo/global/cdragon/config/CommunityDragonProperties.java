package com.tftgogo.global.cdragon.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "community-dragon")
public class CommunityDragonProperties {

    private String tftKoKrUrl = "https://raw.communitydragon.org/latest/cdragon/tft/ko_kr.json";
    private String assetBaseUrl = "https://raw.communitydragon.org/latest/game";
}
