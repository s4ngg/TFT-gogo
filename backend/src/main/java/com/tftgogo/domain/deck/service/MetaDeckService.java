package com.tftgogo.domain.deck.service;

import com.tftgogo.domain.deck.dto.response.MetaDeckListResponse;
import com.tftgogo.domain.deck.entity.RankFilter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface MetaDeckService {

    MetaDeckListResponse getMetaDecks(RankFilter rankFilter);

    Optional<String> findLatestPatchVersion(RankFilter rankFilter);

    // displayPatchVersion(예: "17.6")에 매핑되는 원본 client version 목록을 조회 시점에 계산해 반환한다.
    List<String> resolveRawVersionsForPatch(RankFilter rankFilter, String displayPatchVersion);

    void aggregateAndSave();

    void aggregateAndSave(LocalDate dataDate);

    CompletableFuture<Void> aggregateAndSaveAsync(LocalDate dataDate);
}
