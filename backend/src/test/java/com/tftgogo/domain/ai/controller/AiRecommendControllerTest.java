package com.tftgogo.domain.ai.controller;

import com.tftgogo.domain.ai.dto.AiRecommendResponse;
import com.tftgogo.domain.ai.service.AiRecommendService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRecommendControllerTest {

    @Mock
    private AiRecommendService aiRecommendService;

    @InjectMocks
    private AiRecommendController aiRecommendController;

    @Test
    void AI_서버_응답이_null이면_200에_data_null_반환() {
        // given
        when(aiRecommendService.recommend("TestUser", "KR1")).thenReturn(null);

        // when
        ResponseEntity<?> response = aiRecommendController.recommend("TestUser", "KR1");

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isInstanceOf(com.tftgogo.global.response.ApiResponse.class);
        var body = (com.tftgogo.global.response.ApiResponse<?>) response.getBody();
        assertThat(body.getData()).isNull();
    }

    @Test
    void AI_서버_응답이_있으면_200에_data_포함_반환() {
        // given
        AiRecommendResponse mockResponse = new AiRecommendResponse();
        when(aiRecommendService.recommend("TestUser", "KR1")).thenReturn(mockResponse);

        // when
        ResponseEntity<?> response = aiRecommendController.recommend("TestUser", "KR1");

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isInstanceOf(com.tftgogo.global.response.ApiResponse.class);
        var body = (com.tftgogo.global.response.ApiResponse<?>) response.getBody();
        assertThat(body.getData()).isNotNull();
    }
}
