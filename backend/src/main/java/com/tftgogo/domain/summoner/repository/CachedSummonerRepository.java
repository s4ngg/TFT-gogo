package com.tftgogo.domain.summoner.repository;

import com.tftgogo.domain.summoner.entity.CachedSummoner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CachedSummonerRepository extends JpaRepository<CachedSummoner, String> {

    Optional<CachedSummoner> findByGameNameIgnoreCaseAndTagLineIgnoreCase(String gameName, String tagLine);
}
