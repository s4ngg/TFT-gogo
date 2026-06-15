package com.tftgogo.domain.patchnote.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PatchNoteCrawlImportResponse {

    private String sourceUrl;
    private String version;
    private String locale;
    private boolean dryRun;
    private Long patchNoteId;
    private int createdCount;
    private int updatedCount;
    private int skippedCount;
    private int reviewRequiredCount;
    private int failedCount;
    private List<String> parserWarnings;
    private List<PatchNoteCrawlRowErrorResponse> rowErrors;
}
