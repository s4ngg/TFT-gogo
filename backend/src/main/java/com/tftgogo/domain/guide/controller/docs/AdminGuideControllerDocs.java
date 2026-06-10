package com.tftgogo.domain.guide.controller.docs;

import com.tftgogo.domain.guide.dto.request.AdminGuideRequest;
import com.tftgogo.domain.guide.dto.request.GuideCdragonImportRequest;
import com.tftgogo.domain.guide.dto.response.AdminGuideResponse;
import com.tftgogo.domain.guide.dto.response.GuideImportResponse;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Admin Guide", description = "관리자 게임가이드 API")
@SecurityRequirement(name = "X-Admin-Token")
public interface AdminGuideControllerDocs {

    @Operation(summary = "관리자 게임가이드 목록 조회", description = "삭제되지 않은 게임가이드를 타입, 패치 버전, 활성 상태로 필터링해 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패")
    })
    ResponseEntity<ApiResponse<List<AdminGuideResponse>>> getAdminGuides(
            @Parameter(description = "게임가이드 타입", example = "CHAMPION")
            @RequestParam(name = "guideType", required = false) GuideType guideType,
            @Parameter(description = "패치 버전", example = "17.3")
            @RequestParam(name = "patchVersion", required = false) String patchVersion,
            @Parameter(description = "활성 상태", example = "true")
            @RequestParam(name = "active", required = false) Boolean active
    );

    @Operation(summary = "관리자 게임가이드 생성", description = "게임가이드 데이터를 생성합니다. dataJson은 JSON object여야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복 데이터")
    })
    ResponseEntity<ApiResponse<AdminGuideResponse>> createGuide(
            @RequestBody @Valid AdminGuideRequest request
    );

    @Operation(summary = "관리자 CDragon 게임가이드 import", description = "Community Dragon TFT 한국어 데이터에서 챔피언/특성/아이템 가이드를 가져와 guides 테이블에 upsert합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "import 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "외부 데이터 호출 실패")
    })
    ResponseEntity<ApiResponse<GuideImportResponse>> importCdragonGuides(
            @RequestBody @Valid GuideCdragonImportRequest request
    );

    @Operation(summary = "관리자 게임가이드 수정", description = "게임가이드 데이터를 수정합니다. dataJson은 JSON object여야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게임가이드 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복 데이터")
    })
    ResponseEntity<ApiResponse<AdminGuideResponse>> updateGuide(
            @Parameter(description = "게임가이드 ID", example = "1")
            @PathVariable("guideId") Long guideId,
            @RequestBody @Valid AdminGuideRequest request
    );

    @Operation(summary = "관리자 게임가이드 삭제", description = "게임가이드를 soft delete 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게임가이드 없음")
    })
    ResponseEntity<ApiResponse<Void>> deleteGuide(
            @Parameter(description = "게임가이드 ID", example = "1")
            @PathVariable("guideId") Long guideId
    );
}
