package com.tftgogo.domain.guide.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuidePatchVersionResponse {

    private String patchVersion;

    public static GuidePatchVersionResponse of(String patchVersion) {
        return GuidePatchVersionResponse.builder()
                .patchVersion(patchVersion)
                .build();
    }
}
