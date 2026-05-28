package com.tftgogo.domain.deck.service;

import com.tftgogo.domain.deck.dto.response.MetaDeckResponse;

import java.util.List;

public interface MetaDeckService {

    List<MetaDeckResponse> getMetaDecks();

    void aggregateAndSave();
}
