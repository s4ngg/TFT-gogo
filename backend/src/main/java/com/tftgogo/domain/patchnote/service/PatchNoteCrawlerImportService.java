package com.tftgogo.domain.patchnote.service;

import com.tftgogo.domain.patchnote.dto.request.PatchNoteCrawlImportRequest;
import com.tftgogo.domain.patchnote.dto.response.PatchNoteCrawlImportResponse;

public interface PatchNoteCrawlerImportService {

    PatchNoteCrawlImportResponse importPatchNote(PatchNoteCrawlImportRequest request);
}
