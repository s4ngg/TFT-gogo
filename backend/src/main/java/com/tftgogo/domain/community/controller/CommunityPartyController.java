package com.tftgogo.domain.community.controller;

import com.tftgogo.domain.community.controller.docs.CommunityPartyControllerDocs;
import com.tftgogo.domain.community.dto.request.PartyPostCreateRequest;
import com.tftgogo.domain.community.dto.response.PartyPostResponse;
import com.tftgogo.domain.community.service.CommunityPartyService;
import com.tftgogo.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/community/parties")
@RequiredArgsConstructor
public class CommunityPartyController implements CommunityPartyControllerDocs {

    private final CommunityPartyService communityPartyService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<PartyPostResponse>>> getPartyPosts(
            @RequestParam(name = "mode", required = false) String mode,
            @RequestParam(name = "query", required = false) String query,
            @AuthenticationPrincipal Long userId
    ) {
        List<PartyPostResponse> response = communityPartyService.getPartyPosts(mode, query, userId);
        return ResponseEntity.ok(ApiResponse.success("파티 모집글 조회 성공", response));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<PartyPostResponse>> createPartyPost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PartyPostCreateRequest request
    ) {
        PartyPostResponse response = communityPartyService.createPartyPost(userId, request);
        return ResponseEntity.ok(ApiResponse.success("파티 모집글 등록 성공", response));
    }

    @Override
    @PostMapping("/{partyPostId}/join")
    public ResponseEntity<ApiResponse<PartyPostResponse>> joinParty(
            @AuthenticationPrincipal Long userId,
            @PathVariable("partyPostId") Long partyPostId
    ) {
        PartyPostResponse response = communityPartyService.joinParty(userId, partyPostId);
        return ResponseEntity.ok(ApiResponse.success("파티 참여 성공", response));
    }

    @Override
    @DeleteMapping("/{partyPostId}/join")
    public ResponseEntity<ApiResponse<PartyPostResponse>> cancelJoinParty(
            @AuthenticationPrincipal Long userId,
            @PathVariable("partyPostId") Long partyPostId
    ) {
        PartyPostResponse response = communityPartyService.cancelJoinParty(userId, partyPostId);
        return ResponseEntity.ok(ApiResponse.success("파티 참여 취소 성공", response));
    }
}
