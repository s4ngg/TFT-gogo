package com.tftgogo.global.riot.controller.docs;

import com.tftgogo.global.response.ApiResponse;
import com.tftgogo.global.riot.dto.response.RiotApiStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Riot Status", description = "Riot API 상태 API")
public interface RiotStatusControllerDocs {

    @Operation(
            summary = "Riot API 상태 조회",
            description = "현재 서버의 Riot API 요청 대기열 상태를 반환합니다. "
                    + "status는 available 또는 queue 소문자 값으로 내려가며, "
                    + "activeConnections는 아직 실제 연결 수집원이 없어 0으로 반환하는 예약 필드입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<RiotApiStatusResponse>> getStatus();
}
