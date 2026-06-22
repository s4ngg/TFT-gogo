package com.tftgogo.domain.guide.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GuideCatalogResponse {

    private String patchVersion;
    private List<GuideEntryResponse> entries;
    private List<AugmentGuidePlanResponse> augmentPlans;

    public static GuideCatalogResponse of(
            String patchVersion,
            List<GuideEntryResponse> entries,
            List<AugmentGuidePlanResponse> augmentPlans
    ) {
        return GuideCatalogResponse.builder()
                .patchVersion(patchVersion)
                .entries(entries)
                .augmentPlans(augmentPlans)
                .build();
    }
}
