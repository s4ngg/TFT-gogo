package com.tftgogo.domain.community.dto.response;

import com.tftgogo.domain.community.chat.model.CommunityChatRoomIds;
import com.tftgogo.domain.community.entity.PartyGameMode;
import com.tftgogo.domain.community.entity.PartyPost;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PartyPostResponse {

    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String gameMode;
    private String mode;
    private int currentMembers;
    private int maxMembers;
    private String capacity;
    private String chatRoomId;
    private boolean closed;
    private String status;
    private List<String> tags;
    private boolean joined;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;

    public static PartyPostResponse from(PartyPost partyPost, boolean joined) {
        PartyGameMode gameMode = partyPost.getGameMode();

        return PartyPostResponse.builder()
                .id(partyPost.getId())
                .userId(partyPost.getUserId())
                .title(partyPost.getTitle())
                .content(partyPost.getContent())
                .gameMode(gameMode == null ? null : gameMode.name())
                .mode(gameMode == null ? null : gameMode.getLabel())
                .currentMembers(partyPost.getCurrentMembers())
                .maxMembers(partyPost.getMaxMembers())
                .capacity(partyPost.getCurrentMembers() + "/" + partyPost.getMaxMembers())
                .chatRoomId(CommunityChatRoomIds.PARTY_RECRUITMENT)
                .closed(partyPost.isClosed())
                .status(partyPost.isClosed() ? "마감" : "모집중")
                .tags(List.copyOf(partyPost.getTags()))
                .joined(joined)
                .deadline(partyPost.getDeadline())
                .createdAt(partyPost.getCreatedAt())
                .build();
    }
}
