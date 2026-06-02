package com.tftgogo.domain.patchnote.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PatchChangePageResponse {

    private List<PatchChangeResponse> items;
    private int page;
    private int pageSize;
    private long totalItems;
    private int totalPages;
    private PatchChangeStatsResponse stats;

    public static PatchChangePageResponse of(
            List<PatchChangeResponse> items,
            int page,
            int pageSize,
            long totalItems,
            int totalPages,
            PatchChangeStatsResponse stats
    ) {
        return PatchChangePageResponse.builder()
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .stats(stats)
                .build();
    }
}
