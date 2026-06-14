package com.tftgogo.domain.community.service;

import com.tftgogo.domain.community.dto.request.PartyPostCreateRequest;
import com.tftgogo.domain.community.dto.response.PartyPostResponse;

import java.util.List;

public interface CommunityPartyService {

    List<PartyPostResponse> getPartyPosts(String mode, String query, Long userId);

    PartyPostResponse createPartyPost(Long userId, PartyPostCreateRequest request);

    PartyPostResponse joinParty(Long userId, Long partyPostId);

    PartyPostResponse cancelJoinParty(Long userId, Long partyPostId);
}
