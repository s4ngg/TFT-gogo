package com.tftgogo.domain.community.chat.model;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;

public final class PartyChatRoomIds {

    private static final String PARTY_ROOM_PREFIX = "party-";

    private PartyChatRoomIds() {
    }

    public static String fromPartyPostId(Long partyPostId) {
        if (partyPostId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return PARTY_ROOM_PREFIX + partyPostId;
    }
}
