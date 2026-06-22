package com.tftgogo.domain.community.controller.docs;

import com.tftgogo.domain.community.dto.request.PartyPostCreateRequest;
import com.tftgogo.domain.community.dto.response.PartyPostResponse;
import com.tftgogo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Community Party", description = "커뮤니티 파티 모집 API")
public interface CommunityPartyControllerDocs {

    @Operation(summary = "파티 모집글 목록 조회", description = "게임 모드와 검색어로 파티 모집글을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<PartyPostResponse>>> getPartyPosts(
            @Parameter(description = "게임 모드: RANKED_TFT, NORMAL_TFT, CUSTOM 또는 한글 라벨", example = "RANKED_TFT")
            @RequestParam(name = "mode", required = false) String mode,
            @Parameter(description = "제목/본문 검색어", example = "마스터")
            @RequestParam(name = "query", required = false) String query,
            @AuthenticationPrincipal Long userId
    );

    @Operation(summary = "파티 모집글 등록", description = "인증된 사용자가 파티 모집글을 등록합니다. 티어 조건은 ERD에 없는 별도 컬럼을 만들지 않고 custom tags에 포함해 저장합니다. MVP에서는 파티별 채팅방을 만들지 않고 응답의 chatRoomId는 고정 파티 모집 채널(party-recruitment)을 반환합니다. 파티별 전용 채팅방과 멤버십 검증은 후속 과제로 둡니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 활성 파티 작성 또는 참여 중")
    })
    ResponseEntity<ApiResponse<PartyPostResponse>> createPartyPost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PartyPostCreateRequest request
    );

    @Operation(summary = "파티 참여", description = "인증된 사용자가 파티 모집글에 즉시 참여합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참여 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음 또는 모집글 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "정원 초과, 모집 마감 또는 이미 다른 활성 파티 참여/작성 중")
    })
    ResponseEntity<ApiResponse<PartyPostResponse>> joinParty(
            @AuthenticationPrincipal Long userId,
            @PathVariable("partyPostId") Long partyPostId
    );

    @Operation(summary = "파티 참여 취소", description = "인증된 사용자가 파티 참여를 취소합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 취소 불가"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "모집글 없음")
    })
    ResponseEntity<ApiResponse<PartyPostResponse>> cancelJoinParty(
            @AuthenticationPrincipal Long userId,
            @PathVariable("partyPostId") Long partyPostId
    );
}
