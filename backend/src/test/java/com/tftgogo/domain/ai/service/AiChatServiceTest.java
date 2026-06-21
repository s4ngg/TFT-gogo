package com.tftgogo.domain.ai.service;

import com.tftgogo.domain.ai.client.AiServerClient;
import com.tftgogo.domain.ai.dto.AiChatRequest;
import com.tftgogo.domain.ai.dto.AiChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void AI_서버가_응답_없으면_null을_반환한다() {
        // given
        AiChatRequest request = new AiChatRequest(
                List.of(new AiChatRequest.MessageDto("user", "어떤 덱 추천해요?")),
                null
        );
        when(aiServerClient.chat(any())).thenReturn(null);

        // when
        AiChatResponse result = aiChatService.chat(request);

        // then
        assertThat(result).isNull();
    }
}
