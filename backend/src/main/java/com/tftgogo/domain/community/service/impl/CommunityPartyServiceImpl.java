package com.tftgogo.domain.community.service.impl;

import com.tftgogo.domain.community.dto.request.PartyPostCreateRequest;
import com.tftgogo.domain.community.dto.response.PartyPostResponse;
import com.tftgogo.domain.community.entity.PartyApplication;
import com.tftgogo.domain.community.entity.PartyApplicationStatus;
import com.tftgogo.domain.community.entity.PartyGameMode;
import com.tftgogo.domain.community.entity.PartyPost;
import com.tftgogo.domain.community.repository.PartyApplicationRepository;
import com.tftgogo.domain.community.repository.PartyPostRepository;
import com.tftgogo.domain.community.service.CommunityPartyService;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityPartyServiceImpl implements CommunityPartyService {

    private static final int PARTY_POST_PAGE_SIZE = 50;

    private final PartyPostRepository partyPostRepository;
    private final PartyApplicationRepository partyApplicationRepository;

    @Override
    public List<PartyPostResponse> getPartyPosts(String mode, String query, Long userId) {
        PartyGameMode gameMode = PartyGameMode.fromNullable(mode);
        String normalizedQuery = normalizeQuery(query);
        PageRequest pageRequest = PageRequest.of(0, PARTY_POST_PAGE_SIZE);

        return partyPostRepository.search(gameMode, normalizedQuery, pageRequest)
                .getContent()
                .stream()
                .map(partyPost -> PartyPostResponse.from(partyPost, isJoined(partyPost, userId)))
                .toList();
    }

    @Override
    @Transactional
    public PartyPostResponse createPartyPost(Long userId, PartyPostCreateRequest request) {
        validateAuthenticated(userId);

        PartyPost partyPost = partyPostRepository.save(PartyPost.create(userId, request));

        return PartyPostResponse.from(partyPost, true);
    }

    @Override
    @Transactional
    public PartyPostResponse joinParty(Long userId, Long partyPostId) {
        validateAuthenticated(userId);

        PartyPost partyPost = getPartyPostForUpdate(partyPostId);
        if (partyPost.isOwner(userId) || hasAcceptedApplication(partyPost, userId)) {
            return PartyPostResponse.from(partyPost, true);
        }

        partyPost.join();
        partyApplicationRepository.save(PartyApplication.accepted(partyPost, userId));

        return PartyPostResponse.from(partyPost, true);
    }

    @Override
    @Transactional
    public PartyPostResponse cancelJoinParty(Long userId, Long partyPostId) {
        validateAuthenticated(userId);

        PartyPost partyPost = getPartyPostForUpdate(partyPostId);
        if (partyPost.isOwner(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Optional<PartyApplication> application = partyApplicationRepository.findByPartyPostAndUserIdAndStatus(
                partyPost,
                userId,
                PartyApplicationStatus.ACCEPTED
        );

        if (application.isEmpty()) {
            return PartyPostResponse.from(partyPost, false);
        }

        partyPost.cancelJoin(userId);
        partyApplicationRepository.delete(application.get());

        return PartyPostResponse.from(partyPost, false);
    }

    private PartyPost getPartyPostForUpdate(Long partyPostId) {
        return partyPostRepository.findActiveByIdForUpdate(partyPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_POST_NOT_FOUND));
    }

    private boolean isJoined(PartyPost partyPost, Long userId) {
        if (userId == null) {
            return false;
        }

        return partyPost.isOwner(userId) || hasAcceptedApplication(partyPost, userId);
    }

    private boolean hasAcceptedApplication(PartyPost partyPost, Long userId) {
        return partyApplicationRepository.existsByPartyPostAndUserIdAndStatus(
                partyPost,
                userId,
                PartyApplicationStatus.ACCEPTED
        );
    }

    private void validateAuthenticated(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String normalizeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        return query.trim();
    }
}
