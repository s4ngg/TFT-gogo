package com.tftgogo.global.riot.dto.response;

import com.fasterxml.jackson.annotation.JsonValue;
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
    private RiotApiStatusKind status;

    public static RiotApiStatusResponse from(int queueSize) {
        RiotApiStatusKind status = queueSize > 0 ? RiotApiStatusKind.QUEUE : RiotApiStatusKind.AVAILABLE;
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

    public enum RiotApiStatusKind {
        AVAILABLE("available"),
        QUEUE("queue");

        private final String value;

        RiotApiStatusKind(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }
}
