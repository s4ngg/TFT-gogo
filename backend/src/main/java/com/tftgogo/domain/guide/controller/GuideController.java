package com.tftgogo.domain.guide.controller;

import com.tftgogo.domain.guide.controller.docs.GuideControllerDocs;
import com.tftgogo.domain.guide.dto.response.GuideCatalogResponse;
import com.tftgogo.domain.guide.dto.response.GuideEntryResponse;
import com.tftgogo.domain.guide.dto.response.GuidePageResponse;
import com.tftgogo.domain.guide.dto.response.GuidePatchVersionResponse;
import com.tftgogo.domain.guide.service.GuideService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guide")
@RequiredArgsConstructor
public class GuideController implements GuideControllerDocs {

    private final GuideService guideService;

    @GetMapping
    public ResponseEntity<ApiResponse<GuideCatalogResponse>> getGuideCatalog() {
        GuideCatalogResponse response = guideService.getGuideCatalog();
        return ResponseEntity.ok(ApiResponse.success("게임가이드 조회 성공", response));
    }

    @GetMapping("/patch-version")
    public ResponseEntity<ApiResponse<GuidePatchVersionResponse>> getCurrentPatchVersion() {
        GuidePatchVersionResponse response = guideService.getCurrentPatchVersion();
        return ResponseEntity.ok(ApiResponse.success("게임가이드 현재 패치 조회 성공", response));
    }

    @GetMapping("/{tab}")
    public ResponseEntity<ApiResponse<GuidePageResponse<GuideEntryResponse>>> getGuideTabItems(
            @PathVariable("tab") String tab,
            @RequestParam(name = "patchVersion", required = false) String patchVersion,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(name = "sortKey", required = false) String sortKey,
            @RequestParam(name = "sortDir", required = false) String sortDir,
            @RequestParam(name = "cost", required = false) Integer cost) {
        GuidePageResponse<GuideEntryResponse> response = guideService.getGuideTabItems(
                tab,
                patchVersion,
                query,
                page,
                pageSize,
                sortKey,
                sortDir,
                cost
        );
        return ResponseEntity.ok(ApiResponse.success("게임가이드 탭 조회 성공", response));
    }
}
