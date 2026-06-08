package com.tftgogo.domain.guide.controller;

import com.tftgogo.domain.guide.controller.docs.AdminGuideControllerDocs;
import com.tftgogo.domain.guide.dto.request.AdminGuideRequest;
import com.tftgogo.domain.guide.dto.response.AdminGuideResponse;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.service.AdminGuideService;
import com.tftgogo.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/guides")
@RequiredArgsConstructor
public class AdminGuideController implements AdminGuideControllerDocs {

    private final AdminGuideService adminGuideService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminGuideResponse>>> getAdminGuides(
            @RequestParam(name = "guideType", required = false) GuideType guideType,
            @RequestParam(name = "patchVersion", required = false) String patchVersion,
            @RequestParam(name = "active", required = false) Boolean active) {
        List<AdminGuideResponse> response = adminGuideService.getAdminGuides(guideType, patchVersion, active);
        return ResponseEntity.ok(ApiResponse.success("관리자 게임가이드 목록 조회 성공", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminGuideResponse>> createGuide(
            @RequestBody @Valid AdminGuideRequest request) {
        AdminGuideResponse response = adminGuideService.createGuide(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("관리자 게임가이드 생성 성공", response));
    }

    @PatchMapping("/{guideId}")
    public ResponseEntity<ApiResponse<AdminGuideResponse>> updateGuide(
            @PathVariable Long guideId,
            @RequestBody @Valid AdminGuideRequest request) {
        AdminGuideResponse response = adminGuideService.updateGuide(guideId, request);
        return ResponseEntity.ok(ApiResponse.success("관리자 게임가이드 수정 성공", response));
    }

    @DeleteMapping("/{guideId}")
    public ResponseEntity<ApiResponse<Void>> deleteGuide(@PathVariable Long guideId) {
        adminGuideService.deleteGuide(guideId);
        return ResponseEntity.ok(ApiResponse.success("관리자 게임가이드 삭제 성공", null));
    }
}
