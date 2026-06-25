package com.tftgogo.domain.community.chat.repository;

import com.tftgogo.domain.community.chat.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findTop100ByRoomRoomKeyOrderByCreatedAtDescIdDesc(String roomKey);
}
