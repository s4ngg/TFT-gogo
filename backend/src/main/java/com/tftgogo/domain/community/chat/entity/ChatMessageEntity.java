package com.tftgogo.domain.community.chat.entity;

import com.tftgogo.domain.community.chat.model.ChatMessage;
import com.tftgogo.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Member sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoomEntity room;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_filtered", nullable = false)
    private boolean filtered;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ChatMessageEntity of(ChatRoomEntity room, Member sender, String content) {
        ChatMessageEntity message = new ChatMessageEntity();
        message.room = room;
        message.sender = sender;
        message.content = content;
        message.filtered = false;
        message.createdAt = LocalDateTime.now();
        return message;
    }

    public ChatMessage toModel(String tier) {
        return new ChatMessage(
                String.valueOf(id),
                room.getRoomKey(),
                sender.getNickname(),
                tier,
                content,
                createdAt.atZone(ZoneId.systemDefault()).toInstant()
        );
    }
}
