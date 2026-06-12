package com.tftgogo.domain.community.entity;

import com.tftgogo.domain.community.dto.request.PartyPostCreateRequest;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "party_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", length = 30)
    private PartyGameMode gameMode;

    @Column(name = "max_members", nullable = false)
    private int maxMembers;

    @Column(name = "current_members", nullable = false)
    private int currentMembers;

    @Column
    private LocalDateTime deadline;

    @Column(name = "is_closed", nullable = false)
    private boolean closed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ElementCollection
    @BatchSize(size = 50)
    @CollectionTable(name = "party_post_tags", joinColumns = @JoinColumn(name = "party_post_id"))
    @OrderColumn(name = "tag_order")
    @Column(name = "tag", length = 30)
    private List<String> tags = new ArrayList<>();

    public static PartyPost create(Long userId, PartyPostCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = request.getDeadline();
        if (deadline != null && !deadline.isAfter(now)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        PartyPost partyPost = new PartyPost();
        partyPost.userId = userId;
        partyPost.title = request.getTitle().trim();
        partyPost.content = request.getContent().trim();
        partyPost.gameMode = request.getGameMode();
        partyPost.maxMembers = request.getMaxMembers();
        partyPost.currentMembers = 1;
        partyPost.deadline = deadline;
        partyPost.closed = false;
        partyPost.createdAt = now;
        partyPost.updatedAt = now;
        partyPost.tags = normalizeTags(request.getTags());
        partyPost.refreshClosed();
        return partyPost;
    }

    public void join() {
        if (isFull()) {
            throw new BusinessException(ErrorCode.PARTY_POST_FULL);
        }
        if (isClosed()) {
            throw new BusinessException(ErrorCode.PARTY_POST_CLOSED);
        }

        currentMembers++;
        updatedAt = LocalDateTime.now();
        refreshClosed();
    }

    public void cancelJoin(Long userId) {
        if (isOwner(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 작성자 1명은 동시성/데이터 불일치 상황에서도 보존한다.
        currentMembers = Math.max(1, currentMembers - 1);
        updatedAt = LocalDateTime.now();
        refreshClosed();
    }

    public boolean isOwner(Long userId) {
        return userId != null && userId.equals(this.userId);
    }

    public boolean isFull() {
        return currentMembers >= maxMembers;
    }

    public boolean isClosed() {
        return closed || isDeadlineExpired() || isFull();
    }

    private void refreshClosed() {
        closed = isFull() || isDeadlineExpired();
    }

    private boolean isDeadlineExpired() {
        return deadline != null && !deadline.isAfter(LocalDateTime.now());
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .limit(4)
                .toList());
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
