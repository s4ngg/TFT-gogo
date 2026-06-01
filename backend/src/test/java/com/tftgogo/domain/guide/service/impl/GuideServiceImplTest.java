package com.tftgogo.domain.guide.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tftgogo.domain.guide.dto.response.GuidePageResponse;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideType;
import com.tftgogo.domain.guide.repository.GuideRepository;
import com.tftgogo.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuideServiceImplTest {

    @Mock
    private GuideRepository guideRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GuideServiceImpl guideService;

    @Test
    void 챔피언_탭은_cost_필터를_적용한다() {
        Guide fourCostChampion = championGuide("kaisa", "카이사", 4, 1);
        Guide twoCostChampion = championGuide("jinx", "징크스", 2, 2);
        when(guideRepository.findByGuideTypeAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc(GuideType.CHAMPION))
                .thenReturn(List.of(fourCostChampion, twoCostChampion));

        GuidePageResponse<?> response = guideService.getGuideTabItems(
                "champions",
                null,
                null,
                1,
                10,
                null,
                null,
                4
        );

        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void dataJson은_JSON_object로_응답한다() {
        Guide champion = championGuide("kaisa", "카이사", 4, 1);
        when(guideRepository.findByGuideTypeAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc(GuideType.CHAMPION))
                .thenReturn(List.of(champion));

        GuidePageResponse<?> response = guideService.getGuideTabItems(
                "champions",
                null,
                null,
                1,
                10,
                null,
                null,
                null
        );

        Object firstItem = response.getItems().get(0);
        assertThat(firstItem)
                .hasFieldOrPropertyWithValue("name", "카이사")
                .extracting("dataJson")
                .satisfies(dataJson -> assertThat(dataJson.toString()).contains("\"cost\":4"));
    }

    @Test
    void 지원하지_않는_탭은_예외를_던진다() {
        assertThatThrownBy(() -> guideService.getGuideTabItems(
                "unknown",
                null,
                null,
                1,
                10,
                null,
                null,
                null
        )).isInstanceOf(BusinessException.class);
    }

    private Guide championGuide(String targetKey, String name, int cost, int sortOrder) {
        return Guide.builder()
                .guideType(GuideType.CHAMPION)
                .targetKey(targetKey)
                .name(name)
                .summary(name + " 요약")
                .imageUrl("https://example.com/" + targetKey + ".png")
                .dataJson("{\"cost\":" + cost + ",\"role\":\"캐리\",\"traits\":[\"도전자\"],\"bestItems\":[],\"stats\":{}}")
                .patchVersion("17.0")
                .sortOrder(sortOrder)
                .active(true)
                .build();
    }
}
