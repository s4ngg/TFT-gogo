package com.tftgogo.domain.patchnote.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PatchNoteCrawlRowErrorResponse {

    private String sourceKey;
    private String headingPath;
    private Integer sourceOrder;
    private String rowTextPreview;
    private String reason;
}
