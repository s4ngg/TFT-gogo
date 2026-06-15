package com.tftgogo.domain.patchnote.dto.crawl;

import com.tftgogo.domain.patchnote.entity.PatchChangeCategory;
import com.tftgogo.domain.patchnote.entity.PatchChangeImpact;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;

import java.time.LocalDateTime;
import java.util.List;

public record PatchChangeImportCandidate(
        String sourceKey,
        String sourceUrl,
        String sourceHeadingPath,
        Integer sourceOrder,
        String sourceLocale,
        LocalDateTime importedAt,
        PatchChangeCategory category,
        PatchChangeType changeType,
        PatchChangeImpact impact,
        String targetKey,
        String targetName,
        String summary,
        String beforeValue,
        String afterValue,
        String imageUrl,
        List<String> tags,
        int sortOrder,
        boolean active,
        boolean reviewRequired
) {
}
