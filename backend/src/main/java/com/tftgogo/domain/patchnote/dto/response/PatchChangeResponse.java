package com.tftgogo.domain.patchnote.dto.response;

import com.tftgogo.domain.patchnote.entity.PatchCategory;
import com.tftgogo.domain.patchnote.entity.PatchChange;
import com.tftgogo.domain.patchnote.entity.PatchChangeType;
import com.tftgogo.domain.patchnote.entity.PatchImpact;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PatchChangeResponse {

    private Long id;
    private PatchCategory category;
    private PatchChangeType type;
    private PatchImpact impact;
    private String targetKey;
    private String targetName;
    private String summary;
    private String beforeValue;
    private String afterValue;
    private String imageUrl;
    private List<String> tags;

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
                .build();
    }
}
