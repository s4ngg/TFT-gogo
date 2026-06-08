package com.tftgogo.domain.guide.service;

import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;

public interface GuideCdragonImportService {

    GuideImportResponse importGuides(GuideCdragonImportRequest request);
}
