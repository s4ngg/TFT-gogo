package com.tftgogo.domain.community.service.impl;

import com.tftgogo.domain.community.chat.model.CommunityChatRoomIds;
import com.tftgogo.domain.community.chat.service.ChatService;
import com.tftgogo.domain.community.dto.request.PartyPostCreateRequest;
import com.tftgogo.domain.community.dto.response.PartyPostResponse;
import com.tftgogo.domain.community.entity.PartyApplication;
import com.tftgogo.domain.community.entity.PartyApplicationStatus;
import com.tftgogo.domain.community.entity.PartyGameMode;
import com.tftgogo.domain.community.entity.PartyPost;
import com.tftgogo.domain.community.repository.PartyApplicationRepository;
import com.tftgogo.domain.community.repository.PartyPostRepository;
import com.tftgogo.domain.community.service.CommunityPartyService;
import com.tftgogo.domain.member.repository.MemberRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityPartyServiceImpl implements CommunityPartyService {

    private static final int PARTY_POST_PAGE_SIZE = 50;

    private final PartyPostRepository partyPostRepository;
    private final PartyApplicationRepository partyApplicationRepository;
    private final MemberRepository memberRepository;
    private final ChatService chatService;

    @Override
    public List<PartyPostResponse> getPartyPosts(String mode, String query, Long userId) {
        PartyGameMode gameMode = PartyGameMode.fromNullable(mode);
        String normalizedQuery = normalizeQuery(query);
        PageRequest pageRequest = PageRequest.of(0, PARTY_POST_PAGE_SIZE);

        List<PartyPost> partyPosts = partyPostRepository.search(gameMode, normalizedQuery, pageRequest)
                .getContent();
        Set<Long> joinedPartyPostIds = getJoinedPartyPostIds(partyPosts, userId);

        return partyPosts
                .stream()
                .map(partyPost -> PartyPostResponse.from(partyPost, isJoined(partyPost, userId, joinedPartyPostIds)))
                .toList();
    }

    @Override
    @Transactional
    public PartyPostResponse createPartyPost(Long userId, PartyPostCreateRequest request) {
        validateAuthenticated(userId);
        PartyPost partyPost = PartyPost.create(userId, request);

        lockMemberForPartyMutation(userId);
        if (hasActivePartyParticipation(userId)) {
            throw new BusinessException(ErrorCode.PARTY_ALREADY_JOINED);
        }

        PartyPost savedPartyPost = partyPostRepository.save(partyPost);
        chatService.ensureRoom(CommunityChatRoomIds.PARTY_RECRUITMENT);

        return PartyPostResponse.from(savedPartyPost, true);
    }

    @Override
    @Transactional
    public PartyPostResponse joinParty(Long userId, Long partyPostId) {
        validateAuthenticated(userId);
        lockMemberForPartyMutation(userId);

        PartyPost partyPost = getPartyPostForUpdate(partyPostId);
        if (partyPost.isOwner(userId) || hasAcceptedApplication(partyPost, userId)) {
            return PartyPostResponse.from(partyPost, true);
        }

        if (hasOtherActivePartyParticipation(userId, partyPost.getId())) {
            throw new BusinessException(ErrorCode.PARTY_ALREADY_JOINED);
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

    private void lockMemberForPartyMutation(Long userId) {
        memberRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private boolean isJoined(PartyPost partyPost, Long userId, Set<Long> joinedPartyPostIds) {
        if (userId == null) {
            return false;
        }

        return partyPost.isOwner(userId)
                || (partyPost.getId() != null && joinedPartyPostIds.contains(partyPost.getId()));
    }

    private Set<Long> getJoinedPartyPostIds(List<PartyPost> partyPosts, Long userId) {
        if (userId == null || partyPosts.isEmpty()) {
            return Set.of();
        }

        List<Long> partyPostIds = partyPosts.stream()
                .map(PartyPost::getId)
                .filter(id -> id != null)
                .toList();
        if (partyPostIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(partyApplicationRepository.findPartyPostIdsByUserIdAndStatus(
                userId,
                PartyApplicationStatus.ACCEPTED,
                partyPostIds
        ));
    }

    private boolean hasAcceptedApplication(PartyPost partyPost, Long userId) {
        return partyApplicationRepository.existsByPartyPostAndUserIdAndStatus(
                partyPost,
                userId,
                PartyApplicationStatus.ACCEPTED
        );
    }

    private boolean hasActivePartyParticipation(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        return partyPostRepository.existsActiveOwnedPartyPost(userId, now)
                || partyApplicationRepository.existsActiveAcceptedApplication(
                        userId,
                        PartyApplicationStatus.ACCEPTED,
                        now
                );
    }

    private boolean hasOtherActivePartyParticipation(Long userId, Long partyPostId) {
        LocalDateTime now = LocalDateTime.now();

        return partyPostRepository.existsActiveOwnedPartyPostForOtherParty(userId, partyPostId, now)
                || partyApplicationRepository.existsActiveAcceptedApplicationForOtherParty(
                        userId,
                        PartyApplicationStatus.ACCEPTED,
                        partyPostId,
                        now
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
