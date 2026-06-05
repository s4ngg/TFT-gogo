package com.tftgogo.domain.guide.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.tftgogo.domain.guide.entity.Guide;
import com.tftgogo.domain.guide.entity.GuideType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminGuideResponse {

    private Long id;
    private GuideType guideType;
    private String targetKey;
    private String name;
    private String summary;
    private String imageUrl;
    private JsonNode dataJson;
    private String patchVersion;
    private int sortOrder;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static AdminGuideResponse from(Guide guide, JsonNode dataJson) {
        return AdminGuideResponse.builder()
                .id(guide.getId())
                .guideType(guide.getGuideType())
                .targetKey(guide.getTargetKey())
                .name(guide.getName())
                .summary(guide.getSummary())
                .imageUrl(guide.getImageUrl())
                .dataJson(dataJson)
                .patchVersion(guide.getPatchVersion())
                .sortOrder(guide.getSortOrder())
                .active(guide.isActive())
                .createdAt(guide.getCreatedAt())
                .updatedAt(guide.getUpdatedAt())
                .deletedAt(guide.getDeletedAt())
                .build();
    }
}
