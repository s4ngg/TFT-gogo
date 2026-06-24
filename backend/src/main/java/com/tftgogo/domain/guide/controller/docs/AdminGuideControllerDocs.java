package com.tftgogo.domain.guide.controller.docs;

import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Guide", description = "관리자 게임가이드 API")
@SecurityRequirement(name = "X-Admin-Token")
public interface AdminGuideControllerDocs {

    @Operation(
            summary = "관리자 CDragon 게임가이드 import",
            description = "Community Dragon TFT 한국어 데이터에서 챔피언/특성/아이템/증강체 가이드를 가져와 개별 게임가이드 테이블에 upsert합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "import 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "외부 데이터 호출 실패")
    })
    ResponseEntity<ApiResponse<GuideImportResponse>> importCdragonGuides(
            @RequestBody @Valid GuideCdragonImportRequest request
    );
}
