package com.tftgogo.domain.guide.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GuideCatalogResponse {

    private String patchVersion;
    private List<GuideEntryResponse> entries;

    public static GuideCatalogResponse of(
            String patchVersion,
            List<GuideEntryResponse> entries
    ) {
        return GuideCatalogResponse.builder()
                .patchVersion(patchVersion)
                .entries(entries)
                .build();
    }
}
