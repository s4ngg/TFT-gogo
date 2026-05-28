package com.tftgogo.domain.deck.service;

import com.tftgogo.domain.deck.dto.response.MetaDeckResponse;
import com.tftgogo.domain.deck.entity.RankFilter;

import java.util.List;

public interface MetaDeckService {

    List<MetaDeckResponse> getMetaDecks(RankFilter rankFilter);

    void aggregateAndSave();
}
