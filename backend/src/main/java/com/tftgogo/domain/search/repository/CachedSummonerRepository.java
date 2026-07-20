package com.tftgogo.domain.search.repository;

import com.tftgogo.domain.search.entity.CachedSummoner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CachedSummonerRepository extends JpaRepository<CachedSummoner, String> {

    // 라이엇 ID 소유권 이전 등으로 같은 gameName/tagLine에 puuid가 다른 캐시 행이
    // 여러 개 남아있을 수 있어 단일 결과(Optional)가 아닌 List로 조회한다.
    List<CachedSummoner> findByGameNameIgnoreCaseAndTagLineIgnoreCase(String gameName, String tagLine);
}
