package com.tftgogo.domain.community.chat.repository;

import com.tftgogo.domain.community.chat.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {

    Optional<ChatRoomEntity> findByRoomKey(String roomKey);
}
