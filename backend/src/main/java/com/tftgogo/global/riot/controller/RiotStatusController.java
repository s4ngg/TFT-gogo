package com.tftgogo.global.riot.controller;

import com.tftgogo.global.response.ApiResponse;
import com.tftgogo.global.riot.controller.docs.RiotStatusControllerDocs;
import com.tftgogo.global.riot.dto.response.RiotApiStatusResponse;
import com.tftgogo.global.riot.queue.RiotQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/riot")
@RequiredArgsConstructor
public class RiotStatusController implements RiotStatusControllerDocs {

    private final RiotQueue riotQueue;

    @Override
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<RiotApiStatusResponse>> getStatus() {
        RiotApiStatusResponse status = RiotApiStatusResponse.from(riotQueue.getPendingTaskCount());

        return ResponseEntity.ok(ApiResponse.success("Riot API 상태 조회 성공", status));
    }
}
