package com.tftgogo.domain.deck.service;

import com.tftgogo.domain.deck.dto.response.MetaDeckListResponse;
import com.tftgogo.domain.deck.entity.RankFilter;

import java.time.LocalDate;

public interface MetaDeckService {

    MetaDeckListResponse getMetaDecks(RankFilter rankFilter);

    String findLatestPatchVersion(RankFilter rankFilter);

    void aggregateAndSave();

    void aggregateAndSave(LocalDate dataDate);
}
