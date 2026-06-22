package com.tftgogo.domain.guide.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.tftgogo.domain.guide.entity.AugmentGuidePlan;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AugmentGuidePlanResponse {

    private String key;
    private String label;
    private JsonNode stages;

    public static AugmentGuidePlanResponse from(AugmentGuidePlan plan, JsonNode stages) {
        return AugmentGuidePlanResponse.builder()
                .key(plan.getPlanKey())
                .label(plan.getLabel())
                .stages(stages)
                .build();
    }
}
