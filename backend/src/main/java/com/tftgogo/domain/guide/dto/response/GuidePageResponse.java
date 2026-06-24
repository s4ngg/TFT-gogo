package com.tftgogo.domain.guide.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GuidePageResponse<T> {

    private List<T> items;
    private int page;
    private int pageSize;
    private long totalItems;
    private int totalPages;

    public static <T> GuidePageResponse<T> of(
            List<T> items,
            int page,
            int pageSize,
            long totalItems,
            int totalPages
    ) {
        return GuidePageResponse.<T>builder()
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .build();
    }
}
