package com.tftgogo.domain.community.chat.service.impl;

import com.tftgogo.domain.community.chat.entity.ChatMessageEntity;
import com.tftgogo.domain.community.chat.entity.ChatRoomEntity;
import com.tftgogo.domain.community.chat.model.ChatMessage;
import com.tftgogo.domain.community.chat.model.CommunityChatRoomIds;
import com.tftgogo.domain.community.chat.repository.ChatMessageRepository;
import com.tftgogo.domain.community.chat.repository.ChatRoomRepository;
import com.tftgogo.domain.community.chat.service.ChatMessageStore;
import com.tftgogo.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Profile("!local & !dev")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaChatMessageStore implements ChatMessageStore {

    private static final String DEFAULT_TIER = "Unranked";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    @Transactional
    public void ensureRoom(String roomId) {
        findOrCreateRoom(roomId);
    }

    @Override
    public List<ChatMessage> getRecentMessages(String roomId) {
        List<ChatMessageEntity> recentMessages = new ArrayList<>(
                chatMessageRepository.findTop100ByRoomRoomKeyOrderByCreatedAtDescIdDesc(roomId)
        );
        Collections.reverse(recentMessages);

        return recentMessages.stream()
                .map(message -> message.toModel(DEFAULT_TIER))
                .toList();
    }

    @Override
    @Transactional
    public ChatMessage saveMessage(String roomId, Member sender, String content) {
        ChatRoomEntity room = findOrCreateRoom(roomId);
        ChatMessageEntity savedMessage = chatMessageRepository.saveAndFlush(
                ChatMessageEntity.of(room, sender, content)
        );

        return savedMessage.toModel(DEFAULT_TIER);
    }

    private ChatRoomEntity findOrCreateRoom(String roomId) {
        return chatRoomRepository.findByRoomKey(roomId)
                .orElseGet(() -> createRoom(roomId));
    }

    private ChatRoomEntity createRoom(String roomId) {
        try {
            return chatRoomRepository.saveAndFlush(ChatRoomEntity.of(roomId, resolveRoomName(roomId)));
        } catch (DataIntegrityViolationException e) {
            return chatRoomRepository.findByRoomKey(roomId)
                    .orElseThrow(() -> e);
        }
    }

    private String resolveRoomName(String roomId) {
        return switch (roomId) {
            case CommunityChatRoomIds.GENERAL -> "일반";
            case CommunityChatRoomIds.DECK_GUIDE -> "덱공략";
            case CommunityChatRoomIds.PARTY_RECRUITMENT -> "파티모집";
            case CommunityChatRoomIds.QUESTION_ANSWER -> "질문답변";
            default -> roomId;
        };
    }
}
