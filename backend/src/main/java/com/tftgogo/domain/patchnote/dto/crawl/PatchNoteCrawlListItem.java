package com.tftgogo.domain.patchnote.dto.crawl;

import java.time.LocalDateTime;

public record PatchNoteCrawlListItem(
        String title,
        LocalDateTime publishedAt,
        String description,
        String imageUrl,
        String contentId,
        String detailUrl
) {
}
