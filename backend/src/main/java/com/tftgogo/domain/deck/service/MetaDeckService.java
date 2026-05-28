package com.tftgogo.domain.deck.service;

import com.tftgogo.domain.deck.dto.response.MetaDeckListResponse;
import com.tftgogo.domain.deck.entity.RankFilter;

public interface MetaDeckService {

    MetaDeckListResponse getMetaDecks(RankFilter rankFilter);

    void aggregateAndSave();
}
