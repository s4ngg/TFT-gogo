package com.tftgogo.domain.guide.service;

import com.tftgogo.domain.guide.dto.request.AdminGuideRequest;
import com.tftgogo.domain.guide.dto.response.AdminGuideResponse;
import com.tftgogo.domain.guide.entity.GuideType;

import java.util.List;

public interface AdminGuideService {

    List<AdminGuideResponse> getAdminGuides(GuideType guideType, String patchVersion, Boolean active);

    AdminGuideResponse createGuide(AdminGuideRequest request);

    AdminGuideResponse updateGuide(Long guideId, AdminGuideRequest request);

    void deleteGuide(Long guideId);
}
