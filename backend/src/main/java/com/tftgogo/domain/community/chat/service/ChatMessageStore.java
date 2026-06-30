package com.tftgogo.domain.community.chat.service;

import com.tftgogo.domain.community.chat.model.ChatMessage;
import com.tftgogo.domain.member.entity.Member;

import java.util.List;

public interface ChatMessageStore {

    void ensureRoom(String roomId);

    List<ChatMessage> getRecentMessages(String roomId);

    ChatMessage saveMessage(String roomId, Member sender, String content);
}
