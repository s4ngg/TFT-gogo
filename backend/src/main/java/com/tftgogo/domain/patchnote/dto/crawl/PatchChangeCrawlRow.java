package com.tftgogo.domain.patchnote.dto.crawl;

import java.util.List;

public record PatchChangeCrawlRow(
        String sourceKeyCandidate,
        String sourceKeyHash,
        String headingPath,
        int sourceOrder,
        String sectionTitle,
        String groupTitle,
        String rowText,
        String rawHtml,
        String beforeText,
        String afterText,
        List<String> parserWarnings
) {
    public PatchChangeCrawlRow {
        parserWarnings = parserWarnings == null ? List.of() : List.copyOf(parserWarnings);
    }
}
