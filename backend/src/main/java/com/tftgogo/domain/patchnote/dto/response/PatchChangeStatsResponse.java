package com.tftgogo.domain.patchnote.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class PatchChangeStatsResponse {

    private long totalChanges;
    private Map<String, Long> categoryCounts;
    private Map<String, Long> typeCounts;
    private long buffCount;
    private long nerfCount;
    private long highImpactCount;

    public static PatchChangeStatsResponse of(
            long totalChanges,
            Map<String, Long> categoryCounts,
            Map<String, Long> typeCounts,
            long buffCount,
            long nerfCount,
            long highImpactCount
    ) {
        return PatchChangeStatsResponse.builder()
                .totalChanges(totalChanges)
                .categoryCounts(categoryCounts)
                .typeCounts(typeCounts)
                .buffCount(buffCount)
                .nerfCount(nerfCount)
                .highImpactCount(highImpactCount)
                .build();
    }
}
