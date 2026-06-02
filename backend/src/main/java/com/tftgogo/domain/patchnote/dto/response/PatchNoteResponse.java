package com.tftgogo.domain.patchnote.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tftgogo.domain.patchnote.entity.PatchNote;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PatchNoteResponse {

    private Long id;
    private String version;
    private String title;
    private String summary;
    private String description;
    private String focus;
    private String imageUrl;
    private LocalDateTime publishedAt;
    @Getter(AccessLevel.NONE)
    private boolean current;
    private List<String> highlights;
    private long changeCount;

    public static PatchNoteResponse from(PatchNote patchNote, List<String> highlights, long changeCount) {
        return PatchNoteResponse.builder()
                .id(patchNote.getId())
                .version(patchNote.getVersion())
                .title(patchNote.getTitle())
                .summary(patchNote.getSummary())
                .description(patchNote.getDescription())
                .focus(patchNote.getFocus())
                .imageUrl(patchNote.getImageUrl())
                .publishedAt(patchNote.getPublishedAt())
                .current(patchNote.isCurrent())
                .highlights(highlights)
                .changeCount(changeCount)
                .build();
    }

    @JsonProperty("isCurrent")
    public boolean getIsCurrent() {
        return current;
    }
}
