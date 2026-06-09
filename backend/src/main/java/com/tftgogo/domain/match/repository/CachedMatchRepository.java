package com.tftgogo.domain.match.repository;

import com.tftgogo.domain.match.entity.CachedMatch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CachedMatchRepository extends JpaRepository<CachedMatch, String> {

    @Query("SELECT cm FROM CachedMatch cm JOIN cm.participantPuuids p WHERE p = :puuid ORDER BY cm.gameDatetime DESC, cm.matchId DESC")
    List<CachedMatch> findByParticipantPuuid(@Param("puuid") String puuid, Pageable pageable);

    @Query("SELECT COUNT(cm) FROM CachedMatch cm JOIN cm.participantPuuids p WHERE p = :puuid")
    long countByParticipantPuuid(@Param("puuid") String puuid);

    @Query("SELECT cm.matchId FROM CachedMatch cm WHERE cm.matchId IN :ids")
    List<String> findMatchIdsByMatchIdIn(@Param("ids") List<String> ids);
}
