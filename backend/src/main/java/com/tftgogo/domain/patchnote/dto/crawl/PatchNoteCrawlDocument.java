package com.tftgogo.domain.patchnote.dto.crawl;

import java.time.LocalDateTime;
import java.util.List;

public record PatchNoteCrawlDocument(
        String sourceUrl,
        String locale,
        String contentId,
        String title,
        String version,
        String summary,
        LocalDateTime publishedAt,
        String imageUrl,
        List<String> authors,
        List<String> sections,
        List<PatchChangeCrawlRow> rows,
        List<String> parserWarnings
) {
}
