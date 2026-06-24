package com.tftgogo.domain.guide.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
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
}
