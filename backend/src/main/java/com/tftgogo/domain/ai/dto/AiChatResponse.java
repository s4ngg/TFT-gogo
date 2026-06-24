package com.tftgogo.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatResponse {

    private String reply;

    public static AiChatResponse of(String reply) {
        AiChatResponse res = new AiChatResponse();
        res.reply = reply;
        return res;
    }

    public static AiChatResponse serviceUnavailable() {
        return of("죄송합니다. 현재 AI 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    }
}
