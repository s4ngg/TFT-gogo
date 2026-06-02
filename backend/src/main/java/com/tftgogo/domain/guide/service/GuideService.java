package com.tftgogo.domain.guide.service;

import com.tftgogo.domain.guide.dto.response.GuideEntryResponse;
import com.tftgogo.domain.guide.dto.response.GuidePageResponse;

import java.util.List;

public interface GuideService {

    List<GuideEntryResponse> getGuideCatalog();

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
