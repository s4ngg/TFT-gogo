package com.tftgogo.domain.guide.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuideImportResponse {

    private String patchVersion;
    private int setNumber;
    private String mutator;
    private int createdCount;
    private int updatedCount;
    private int skippedCount;
    private int championCount;
    private int traitCount;
    private int itemCount;
    private int augmentCount;

    public int getImportedCount() {
        return createdCount + updatedCount;
    }
}
