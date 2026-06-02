package com.tftgogo.domain.guide.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuideEntryResponse {

    private Long id;
    private GuideType guideType;
    private String targetKey;
    private String name;
    private String summary;
    private String imageUrl;
    private String patchVersion;
    private int sortOrder;
    private JsonNode dataJson;

    public static GuideEntryResponse from(Guide guide, JsonNode dataJson) {
        return GuideEntryResponse.builder()
                .id(guide.getId())
                .guideType(guide.getGuideType())
                .targetKey(guide.getTargetKey())
                .name(guide.getName())
                .summary(guide.getSummary())
                .imageUrl(guide.getImageUrl())
                .patchVersion(guide.getPatchVersion())
                .sortOrder(guide.getSortOrder())
                .dataJson(dataJson)
                .build();
    }
}
