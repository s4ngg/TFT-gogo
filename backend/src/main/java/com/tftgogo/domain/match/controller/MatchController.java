package com.tftgogo.domain.match.controller;

import com.tftgogo.domain.match.controller.docs.MatchControllerDocs;
import com.tftgogo.domain.match.dto.response.CollectionStatusResponse;
import com.tftgogo.domain.match.dto.response.MatchDetailResponse;
import com.tftgogo.domain.match.service.MatchService;
import com.tftgogo.domain.summoner.dto.response.SummonerMatchItemDto;
import com.tftgogo.global.cdragon.service.TftAssetCacheService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchController implements MatchControllerDocs {

    private final MatchService matchService;
    private final TftAssetCacheService tftAssetCacheService;

    @Override
    @GetMapping("/{puuid}/matches")
    public ResponseEntity<ApiResponse<List<SummonerMatchItemDto>>> getMatches(
            @PathVariable("puuid") String puuid,
            @RequestParam(name = "start", defaultValue = "0") int start,
            @RequestParam(name = "count", defaultValue = "20") int count) {
        return ResponseEntity.ok(ApiResponse.success("매치 목록 조회 성공",
                matchService.getMatches(puuid, start, count,
                        tftAssetCacheService::getTraitIconUrl,
                        tftAssetCacheService::getTraitName,
                        tftAssetCacheService::getItemIconUrl)));
    }

    @Override
    @GetMapping("/detail/{matchId}")
    public ResponseEntity<ApiResponse<MatchDetailResponse>> getMatchDetail(
            @PathVariable("matchId") String matchId) {
        return ResponseEntity.ok(ApiResponse.success("매치 상세 조회 성공",
                matchService.getMatchDetail(matchId)));
    }

    @Override
    @GetMapping("/{puuid}/collection-status")
    public ResponseEntity<ApiResponse<CollectionStatusResponse>> getCollectionStatus(
            @PathVariable("puuid") String puuid) {
        return ResponseEntity.ok(ApiResponse.success("수집 상태 조회 성공",
                matchService.getCollectionStatus(puuid)));
    }
}
