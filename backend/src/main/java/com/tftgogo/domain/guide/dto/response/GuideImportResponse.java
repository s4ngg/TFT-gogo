package com.tftgogo.domain.guide.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuideImportResponse {

    private int createdCount;
    private int updatedCount;
    private int skippedCount;
    private int championCount;
    private int traitCount;

    public int getImportedCount() {
        return createdCount + updatedCount;
    }
}
