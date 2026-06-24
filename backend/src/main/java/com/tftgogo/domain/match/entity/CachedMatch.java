package com.tftgogo.domain.match.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cached_match")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CachedMatch {

    @Id
    @Column(name = "match_id", length = 50, nullable = false)
    private String matchId;

    @Column(name = "queue_id", nullable = false)
    private int queueId;

    @Column(name = "game_datetime", nullable = false)
    private long gameDatetime;

    @Column(name = "match_json", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String matchJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "cached_match_participant",
            joinColumns = @JoinColumn(name = "match_id", nullable = false)
    )
    @Column(name = "puuid", length = 100, nullable = false)
    private Set<String> participantPuuids = new HashSet<>();

    @Builder
    public CachedMatch(String matchId, int queueId, long gameDatetime,
                       String matchJson, LocalDateTime createdAt, Set<String> participantPuuids) {
        this.matchId = matchId;
        this.queueId = queueId;
        this.gameDatetime = gameDatetime;
        this.matchJson = matchJson;
        this.createdAt = createdAt;
        this.participantPuuids = participantPuuids != null ? participantPuuids : new HashSet<>();
    }
}
 