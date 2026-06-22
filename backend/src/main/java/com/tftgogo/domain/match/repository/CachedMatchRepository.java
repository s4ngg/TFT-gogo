package com.tftgogo.domain.match.repository;

import com.tftgogo.domain.match.entity.CachedMatch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CachedMatchRepository extends JpaRepository<CachedMatch, String> {

    @Query("SELECT cm FROM CachedMatch cm JOIN cm.participantPuuids p WHERE p = :puuid ORDER BY cm.gameDatetime DESC, cm.matchId DESC")
    List<CachedMatch> findByParticipantPuuid(@Param("puuid") String puuid, Pageable pageable);

    @Query("SELECT COUNT(cm) FROM CachedMatch cm JOIN cm.participantPuuids p WHERE p = :puuid")
    long countByParticipantPuuid(@Param("puuid") String puuid);

    @Query("SELECT cm.matchId FROM CachedMatch cm WHERE cm.matchId IN :ids")
    List<String> findMatchIdsByMatchIdIn(@Param("ids") List<String> ids);

    @Query("SELECT cm FROM CachedMatch cm WHERE cm.queueId IN :queueIds ORDER BY cm.gameDatetime DESC, cm.matchId DESC")
    List<CachedMatch> findRecentByQueueIds(@Param("queueIds") Collection<Integer> queueIds, Pageable pageable);

    @Query("SELECT cm FROM CachedMatch cm JOIN cm.participantPuuids p WHERE p = :puuid AND cm.queueId = :queueId ORDER BY cm.gameDatetime DESC, cm.matchId DESC")
    List<CachedMatch> findByParticipantPuuidAndQueueId(@Param("puuid") String puuid, @Param("queueId") int queueId, Pageable pageable);

    @Query("SELECT COUNT(cm) FROM CachedMatch cm WHERE cm.queueId = :queueId")
    long countByQueueId(@Param("queueId") int queueId);

    @Query("SELECT MAX(cm.gameDatetime) FROM CachedMatch cm")
    Optional<Long> findMaxGameDatetime();

    @Query("SELECT MIN(cm.gameDatetime) FROM CachedMatch cm")
    Optional<Long> findMinGameDatetime();

    @Query("SELECT MAX(cm.createdAt) FROM CachedMatch cm")
    Optional<LocalDateTime> findLatestCachedAt();
}
