package com.tftgogo.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.guide.cdragon")
public class GuideCdragonImportProperties {

    private boolean startupImport = false;
    private String patchVersion = "latest";
    private Integer setNumber = 17;
    private String mutator = "TFTSet17";
    private boolean includeChampions = true;
    private boolean includeTraits = true;
    private boolean includeItems = true;
    private boolean includeAugments = true;
}
