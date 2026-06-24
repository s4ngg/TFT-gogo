package com.tftgogo.domain.patchnote.service;

import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlDocument;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlFetchedPage;
import com.tftgogo.domain.patchnote.dto.crawl.PatchNoteCrawlListItem;

import java.util.List;

public interface PatchNoteCrawlerParser {

    List<PatchNoteCrawlListItem> parseListPage(PatchNoteCrawlFetchedPage fetchedPage);

    PatchNoteCrawlDocument parseDetailPage(PatchNoteCrawlFetchedPage fetchedPage, String explicitVersion, String locale);
}
