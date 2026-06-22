package com.tftgogo.domain.guide.dto.response;

import com.tftgogo.domain.guide.entity.AugmentGuideReward;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AugmentGuideRewardResponse {

    private String stage;
    private String condition;
    private String reward;

    public static AugmentGuideRewardResponse from(AugmentGuideReward reward) {
        return AugmentGuideRewardResponse.builder()
                .stage(reward.getStage())
                .condition(reward.getConditionText())
                .reward(reward.getRewardText())
                .build();
    }
}
