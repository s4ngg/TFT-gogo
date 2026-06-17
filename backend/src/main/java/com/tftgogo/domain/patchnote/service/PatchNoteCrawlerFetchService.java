package com.tftgogo.domain.patchnote.service;

import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlFetchedPage;

public interface PatchNoteCrawlerFetchService {

    PatchNoteCrawlFetchedPage fetch(String sourceUrl);

    PatchNoteCrawlFetchedPage fetchTagPage(String locale);
}
