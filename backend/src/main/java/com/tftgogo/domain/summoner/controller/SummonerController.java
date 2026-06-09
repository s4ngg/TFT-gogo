package com.tftgogo.domain.summoner.controller;

import com.tftgogo.domain.summoner.controller.docs.SummonerControllerDocs;
import com.tftgogo.domain.summoner.dto.response.SummonerDetailResponse;
import com.tftgogo.domain.summoner.service.SummonerService;
import com.tftgogo.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/summoners")
@RequiredArgsConstructor
public class SummonerController implements SummonerControllerDocs {

    private final SummonerService summonerService;

    @Override
    @GetMapping("/{gameName}/{tagLine}")
    public ResponseEntity<ApiResponse<SummonerDetailResponse>> getSummoner(
            @PathVariable("gameName") String gameName,
            @PathVariable("tagLine") String tagLine) {
        SummonerDetailResponse detail = summonerService.getDetail(gameName, tagLine);
        return ResponseEntity.ok(ApiResponse.success("소환사 조회 성공", detail));
    }

    @Override
    @PostMapping("/{gameName}/{tagLine}/refresh")
    public ResponseEntity<ApiResponse<SummonerDetailResponse>> refreshSummoner(
            @PathVariable("gameName") String gameName,
            @PathVariable("tagLine") String tagLine) {
        SummonerDetailResponse detail = summonerService.refresh(gameName, tagLine);
        return ResponseEntity.ok(ApiResponse.success("전적 갱신 성공", detail));
    }
}
