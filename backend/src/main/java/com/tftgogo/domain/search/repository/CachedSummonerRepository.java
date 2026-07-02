package com.tftgogo.domain.search.repository;

import com.tftgogo.domain.search.entity.CachedSummoner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CachedSummonerRepository extends JpaRepository<CachedSummoner, String> {

    Optional<CachedSummoner> findByGameNameIgnoreCaseAndTagLineIgnoreCase(String gameName, String tagLine);
}
