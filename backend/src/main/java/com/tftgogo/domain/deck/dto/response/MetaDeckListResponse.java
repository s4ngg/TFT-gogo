package com.tftgogo.domain.deck.dto.response;

import com.tftgogo.domain.deck.entity.RankFilter;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class MetaDeckListResponse {

    private String patchVersion;
    private RankFilter rankFilter;
    private LocalDate dataStartDate;
    private List<MetaDeckResponse> decks;
}
