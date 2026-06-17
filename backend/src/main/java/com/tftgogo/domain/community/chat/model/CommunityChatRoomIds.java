package com.tftgogo.domain.community.chat.model;

import java.util.Set;

public final class CommunityChatRoomIds {

    public static final String GENERAL = "general";
    public static final String DECK_GUIDE = "deck-guide";
    public static final String PARTY_RECRUITMENT = "party-recruitment";
    public static final String QUESTION_ANSWER = "question-answer";

    private static final Set<String> SUPPORTED_ROOM_IDS = Set.of(
            GENERAL,
            DECK_GUIDE,
            PARTY_RECRUITMENT,
            QUESTION_ANSWER
    );

    private CommunityChatRoomIds() {
    }

    public static boolean isSupported(String roomId) {
        return SUPPORTED_ROOM_IDS.contains(roomId);
    }
}
