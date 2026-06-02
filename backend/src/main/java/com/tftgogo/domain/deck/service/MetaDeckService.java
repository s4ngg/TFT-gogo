package com.tftgogo.domain.deck.service;

import com.tftgogo.domain.deck.dto.response.MetaDeckListResponse;
import com.tftgogo.domain.deck.entity.RankFilter;

import java.time.LocalDate;
import java.util.Optional;

public interface MetaDeckService {

    MetaDeckListResponse getMetaDecks(RankFilter rankFilter);

    Optional<String> findLatestPatchVersion(RankFilter rankFilter);

    void aggregateAndSave();

    void aggregateAndSave(LocalDate dataDate);
}
