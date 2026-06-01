package com.tftgogo.domain.match.controller;

import com.tftgogo.domain.match.controller.docs.MatchControllerDocs;
import com.tftgogo.domain.match.dto.response.MatchSearchResponse;
import com.tftgogo.domain.match.dto.response.MatchSummaryResponse;
import com.tftgogo.domain.match.service.SummonerService;
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
        List<MatchSummaryResponse> response = summonerService.getMatches(puuid, start);
        return ResponseEntity.ok(ApiResponse.success("매치 목록 조회 성공", response));
    }
}
