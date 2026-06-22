package com.tftgogo.domain.ai.service;

import com.tftgogo.domain.ai.client.AiServerClient;
import com.tftgogo.domain.ai.dto.AiChatRequest;
import com.tftgogo.domain.ai.dto.AiChatResponse;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock private AiServerClient aiServerClient;

    @InjectMocks
    private AiChatService aiChatService;

    @Test
    void AI_서버_정상_응답시_그대로_반환한다() {
        // given
        AiChatRequest request = new AiChatRequest(
                List.of(new AiChatRequest.MessageDto("user", "어떤 덱 추천해요?")),
                null
        );
        AiChatResponse expected = AiChatResponse.of("신궁 덱을 추천드립니다.");
        when(aiServerClient.chat(any())).thenReturn(expected);

        // when
        AiChatResponse result = aiChatService.chat(request);

        // then
        assertThat(result.getReply()).isEqualTo("신궁 덱을 추천드립니다.");
    }

    @Test
    void AI_서버가_응답_없으면_BusinessException을_던진다() {
        // given
        AiChatRequest request = new AiChatRequest(
                List.of(new AiChatRequest.MessageDto("user", "어떤 덱 추천해요?")),
                null
        );
        when(aiServerClient.chat(any())).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> aiChatService.chat(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AI_SERVER_ERROR));
    }

    @Test
    void AI_서버_호출_실패시_BusinessException을_던진다() {
        // given
        AiChatRequest request = new AiChatRequest(
                List.of(new AiChatRequest.MessageDto("user", "어떤 덱 추천해요?")),
                null
        );
        when(aiServerClient.chat(any())).thenThrow(new RuntimeException("connection refused"));

        // when & then
        assertThatThrownBy(() -> aiChatService.chat(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AI_SERVER_ERROR));
    }
}
