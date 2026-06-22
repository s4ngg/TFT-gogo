package com.tftgogo.domain.patchnote.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminPatchNoteImportResponse {

    private final Long patchNoteId;
    private final String version;
    private final String sourceUrl;
    private final boolean patchNoteCreated;
    private final boolean patchNoteUpdated;
    private final boolean patchNoteSkipped;
    private final int createdChanges;
    private final int updatedChanges;
    private final int skippedChanges;
    private final List<String> parserWarnings;

    public static AdminPatchNoteImportResponse of(
            Long patchNoteId,
            String version,
            String sourceUrl,
            boolean patchNoteCreated,
            boolean patchNoteUpdated,
            boolean patchNoteSkipped,
            int createdChanges,
            int updatedChanges,
            int skippedChanges,
            List<String> parserWarnings
    ) {
        return AdminPatchNoteImportResponse.builder()
                .patchNoteId(patchNoteId)
                .version(version)
                .sourceUrl(sourceUrl)
                .patchNoteCreated(patchNoteCreated)
                .patchNoteUpdated(patchNoteUpdated)
                .patchNoteSkipped(patchNoteSkipped)
                .createdChanges(createdChanges)
                .updatedChanges(updatedChanges)
                .skippedChanges(skippedChanges)
                .parserWarnings(parserWarnings == null ? List.of() : List.copyOf(parserWarnings))
                .build();
    }
}
