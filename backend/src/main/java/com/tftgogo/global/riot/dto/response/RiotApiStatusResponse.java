package com.tftgogo.global.riot.dto.response;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class RiotApiStatusResponse {

    private int activeConnections;
    private String checkedAt;
    private String message;
    private int queueSize;
    private int foregroundQueueSize;
    private int backgroundQueueSize;
    private int inflightCount;
    private RiotApiStatusKind status;

    public static RiotApiStatusResponse from(int foreground, int background, int inflight) {
        int totalQueue = foreground + background;
        RiotApiStatusKind status = totalQueue > 0 ? RiotApiStatusKind.QUEUE : RiotApiStatusKind.AVAILABLE;
        String message = totalQueue > 0
                ? "Riot API 요청 대기열을 처리 중입니다."
                : "Riot API 요청 대기열이 비어 있습니다.";

        return RiotApiStatusResponse.builder()
                .activeConnections(inflight)
                .checkedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .message(message)
                .queueSize(totalQueue)
                .foregroundQueueSize(foreground)
                .backgroundQueueSize(background)
                .inflightCount(inflight)
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
