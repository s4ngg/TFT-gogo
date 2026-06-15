package com.tftgogo.domain.patchnote.dto.crawl;

import java.time.LocalDateTime;
import java.util.List;

public record PatchNoteImportCandidate(
        String version,
        String title,
        String summary,
        String description,
        String focus,
        String imageUrl,
        LocalDateTime publishedAt,
        boolean current,
        List<String> highlights,
        String sourceUrl,
        String sourceLocale,
        LocalDateTime importedAt
) {
}
