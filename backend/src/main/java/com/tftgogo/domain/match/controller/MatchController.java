package com.tftgogo.domain.match.controller;

import com.tftgogo.domain.match.controller.docs.MatchControllerDocs;
import com.tftgogo.domain.match.dto.response.MatchDetailResponse;
import com.tftgogo.domain.match.dto.response.MatchSearchResponse;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.domain.summoner.service.SummonerService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchController implements MatchControllerDocs {

    private final SummonerService summonerService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<MatchSearchResponse>> search(
            @RequestParam("gameName") String gameName,
            @RequestParam("tagLine") String tagLine) {
        MatchSearchResponse response = summonerService.search(gameName, tagLine);
        return ResponseEntity.ok(ApiResponse.success("전적 조회 성공", response));
    }

    @GetMapping("/{puuid}/matches")
    public ResponseEntity<ApiResponse<List<MatchSummaryResponse>>> getMatches(
            @PathVariable("puuid") String puuid,
            @RequestParam(name = "start", defaultValue = "0") int start) {
        if (start < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        List<MatchSummaryResponse> response = summonerService.getMatches(puuid, start);
        return ResponseEntity.ok(ApiResponse.success("매치 목록 조회 성공", response));
    }

    @GetMapping("/detail/{matchId}")
    public ResponseEntity<ApiResponse<MatchDetailResponse>> getMatchDetail(
            @PathVariable("matchId") String matchId) {
        MatchDetailResponse response = summonerService.getMatchDetail(matchId);
        return ResponseEntity.ok(ApiResponse.success("매치 상세 조회 성공", response));
    }
}
