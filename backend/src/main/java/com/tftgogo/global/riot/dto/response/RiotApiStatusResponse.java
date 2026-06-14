package com.tftgogo.global.riot.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class RiotApiStatusResponse {

    private int activeConnections;
    private String checkedAt;
    private String message;
    private int queueSize;
    private String status;

    public static RiotApiStatusResponse from(int queueSize) {
        String status = queueSize > 0 ? "queue" : "available";
        String message = queueSize > 0
                ? "Riot API 요청 대기열을 처리 중입니다."
                : "Riot API 요청 대기열이 비어 있습니다.";

        return RiotApiStatusResponse.builder()
                .activeConnections(0)
                .checkedAt(OffsetDateTime.now().toString())
                .message(message)
                .queueSize(queueSize)
                .status(status)
                .build();
    }
}
