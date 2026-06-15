package com.tftgogo.domain.patchnote.dto.response;

import com.tftgogo.domain.patchnote.entity.PatchChangeCategory;
import com.tftgogo.domain.patchnote.entity.PatchChange;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;
import com.tftgogo.domain.patchnote.entity.PatchChangeImpact;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PatchChangeResponse {

    private Long id;
    private PatchChangeCategory category;
    private PatchChangeType type;
    private PatchChangeImpact impact;
    private String targetKey;
    private String targetName;
    private String summary;
    private String beforeValue;
    private String afterValue;
    private String imageUrl;
    private List<String> tags;
    private int sortOrder;

    public static PatchChangeResponse from(PatchChange patchChange, List<String> tags) {
        return PatchChangeResponse.builder()
                .id(patchChange.getId())
                .category(patchChange.getCategory())
                .type(patchChange.getChangeType())
                .impact(patchChange.getImpact())
                .targetKey(patchChange.getTargetKey())
                .targetName(patchChange.getTargetName())
                .summary(patchChange.getSummary())
                .beforeValue(patchChange.getBeforeValue())
                .afterValue(patchChange.getAfterValue())
                .imageUrl(patchChange.getImageUrl())
                .tags(tags)
                .sortOrder(patchChange.getSortOrder())
                .build();
    }
}
