package com.tftgogo.domain.guide.service;

import com.tftgogo.domain.guide.dto.response.GuideCatalogResponse;
import com.tftgogo.domain.guide.dto.response.GuideEntryResponse;
import com.tftgogo.domain.guide.dto.response.GuidePageResponse;
import com.tftgogo.domain.guide.dto.response.GuidePatchVersionResponse;

public interface GuideService {

    GuideCatalogResponse getGuideCatalog();

    GuidePatchVersionResponse getCurrentPatchVersion();

    GuidePageResponse<GuideEntryResponse> getGuideTabItems(
            String tab,
            String patchVersion,
            String query,
            Integer page,
            Integer pageSize,
            String sortKey,
            String sortDir,
            Integer cost
    );
}
