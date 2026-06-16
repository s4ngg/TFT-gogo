package com.tftgogo.domain.patchnote.dto.crawl;

import java.time.LocalDateTime;

public record PatchNoteCrawlFetchedPage(
        String sourceUrl,
        String rawHtml,
        LocalDateTime fetchedAt,
        int httpStatus
) {
}
