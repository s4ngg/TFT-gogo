package com.tftgogo.domain.deck.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hero_augment_decks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HeroAugmentDeck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String champions;

    @Column(columnDefinition = "TEXT")
    private String traits;

    @Column(name = "board_positions", columnDefinition = "TEXT")
    private String boardPositions;

    @Column(name = "hero_augments", columnDefinition = "TEXT")
    private String heroAugments;

    @Column(nullable = false)
    private boolean recommended;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(length = 10)
    private String grade;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public HeroAugmentDeck(String name, String description, String champions, String traits,
                           String boardPositions, String heroAugments, boolean recommended,
                           int sortOrder, String grade) {
        this.name = name;
        this.description = description;
        this.champions = champions;
        this.traits = traits;
        this.boardPositions = boardPositions;
        this.heroAugments = heroAugments;
        this.recommended = recommended;
        this.sortOrder = sortOrder;
        this.grade = grade;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String name, String description, String champions, String traits,
                       String boardPositions, String heroAugments, boolean recommended,
                       int sortOrder, String grade) {
        this.name = name;
        this.description = description;
        this.champions = champions;
        this.traits = traits;
        this.boardPositions = boardPositions;
        this.heroAugments = heroAugments;
        this.recommended = recommended;
        this.sortOrder = sortOrder;
        this.grade = grade;
        this.updatedAt = LocalDateTime.now();
    }
}
