package com.tftgogo.domain.deck.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "deck_curations",
    uniqueConstraints = { @UniqueConstraint(columnNames = {"signature", "rank_filter"}) }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckCuration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MetaDeck.signature 기반 — 재수집 후에도 큐레이션 유지
    @Column(nullable = false, length = 255)
    private String signature;

    @Enumerated(EnumType.STRING)
    @Column(name = "rank_filter", nullable = false, length = 20)
    private RankFilter rankFilter;

    // null = 자동 생성 이름 사용
    @Column(name = "custom_name", length = 200)
    private String customName;

    // true = 덱 목록에서 숨김
    @Column(name = "is_hidden", nullable = false)
    private boolean hidden = false;

    // null = pickRate 기본 정렬, 숫자 낮을수록 상단
    @Column(name = "sort_priority")
    private Integer sortPriority;

    // 관리자 메모 (사용자에게 노출 안 됨)
    @Column(name = "curator_note", columnDefinition = "TEXT")
    private String curatorNote;

    // 관리자가 직접 배치한 포지션 JSON
    // {"imageUrl": {"row":0,"col":3}, ...}
    @Column(name = "board_positions", columnDefinition = "TEXT")
    private String boardPositions;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public DeckCuration(String signature, RankFilter rankFilter, String customName,
                        boolean hidden, Integer sortPriority, String curatorNote,
                        String boardPositions) {
        this.signature = signature;
        this.rankFilter = rankFilter;
        this.customName = customName;
        this.hidden = hidden;
        this.sortPriority = sortPriority;
        this.curatorNote = curatorNote;
        this.boardPositions = boardPositions;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String customName, boolean hidden, Integer sortPriority, String curatorNote,
                       String boardPositions) {
        this.customName = customName;
        this.hidden = hidden;
        this.sortPriority = sortPriority;
        this.curatorNote = curatorNote;
        this.boardPositions = boardPositions;
        this.updatedAt = LocalDateTime.now();
    }
}
