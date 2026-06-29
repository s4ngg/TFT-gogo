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
                    + "foregroundQueueSize/backgroundQueueSize로 큐별 대기 수, "
                    + "inflightCount로 현재 실행 중인 작업 수를 확인할 수 있습니다. "
                    + "queueSize는 foreground+background 합산 값(하위호환)입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<RiotApiStatusResponse>> getStatus();
}
