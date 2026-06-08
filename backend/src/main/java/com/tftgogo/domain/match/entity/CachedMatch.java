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
    @Column(length = 50)
    private String matchId;

    @Column(nullable = false)
    private int queueId;

    @Column(nullable = false)
    private long gameDatetime;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String matchJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "cached_match_participant",
            joinColumns = @JoinColumn(name = "match_id")
    )
    @Column(name = "puuid", length = 100)
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
