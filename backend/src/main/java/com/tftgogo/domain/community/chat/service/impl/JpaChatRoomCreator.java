package com.tftgogo.domain.community.chat.service.impl;

import com.tftgogo.domain.community.chat.entity.ChatRoomEntity;
import com.tftgogo.domain.community.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!local")
@RequiredArgsConstructor
class JpaChatRoomCreator {

    private final ChatRoomRepository chatRoomRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatRoomEntity create(String roomId, String roomName) {
        return chatRoomRepository.saveAndFlush(ChatRoomEntity.of(roomId, roomName));
    }
}
