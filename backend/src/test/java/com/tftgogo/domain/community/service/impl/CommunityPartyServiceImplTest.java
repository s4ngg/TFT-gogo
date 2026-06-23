package com.tftgogo.domain.community.service.impl;

import com.tftgogo.domain.community.chat.service.ChatService;
import com.tftgogo.domain.community.dto.request.PartyPostCreateRequest;
import com.tftgogo.domain.community.dto.response.PartyPostResponse;
import com.tftgogo.domain.community.entity.PartyApplication;
import com.tftgogo.domain.community.entity.PartyApplicationStatus;
import com.tftgogo.domain.community.entity.PartyGameMode;
import com.tftgogo.domain.community.entity.PartyPost;
import com.tftgogo.domain.community.repository.PartyApplicationRepository;
import com.tftgogo.domain.community.repository.PartyPostRepository;
import com.tftgogo.domain.member.entity.Member;
import com.tftgogo.domain.member.repository.MemberRepository;
import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityPartyServiceImplTest {

    @Mock
    private PartyPostRepository partyPostRepository;

    @Mock
    private PartyApplicationRepository partyApplicationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private CommunityPartyServiceImpl communityPartyService;

    @Test
    void 파티_모집글_목록은_게임모드와_검색어로_조회한다() {
        // given
        PartyPost partyPost = partyPost(1L, 1L, "마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 1, 2);
        PageRequest pageRequest = PageRequest.of(0, 50);
        when(partyPostRepository.search(PartyGameMode.RANKED_TFT, "마스터", pageRequest))
                .thenReturn(new PageImpl<>(List.of(partyPost)));
        when(partyApplicationRepository.findPartyPostIdsByUserIdAndStatus(
                2L,
                PartyApplicationStatus.ACCEPTED,
                List.of(1L)
        )).thenReturn(List.of(1L));

        // when
        List<PartyPostResponse> response = communityPartyService.getPartyPosts("랭크", " 마스터 ", 2L);

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0))
                .extracting(
                        PartyPostResponse::getTitle,
                        PartyPostResponse::getGameMode,
                        PartyPostResponse::getMode,
                        PartyPostResponse::getCapacity,
                        PartyPostResponse::getChatRoomId,
                        PartyPostResponse::isJoined
                )
                .containsExactly("마스터 듀오 구합니다", "RANKED_TFT", "랭크", "1/2", "party-recruitment", true);
        verify(partyPostRepository).search(PartyGameMode.RANKED_TFT, "마스터", pageRequest);
        verify(partyApplicationRepository).findPartyPostIdsByUserIdAndStatus(
                2L,
                PartyApplicationStatus.ACCEPTED,
                List.of(1L)
        );
    }

    @Test
    void 마감시간이_지난_모집글은_목록에서_마감으로_응답한다() {
        // given
        PartyPost partyPost = partyPost(1L, 1L, "마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 1, 3);
        ReflectionTestUtils.setField(partyPost, "deadline", LocalDateTime.now().minusMinutes(1));
        when(partyPostRepository.search(null, null, PageRequest.of(0, 50)))
                .thenReturn(new PageImpl<>(List.of(partyPost)));

        // when
        List<PartyPostResponse> response = communityPartyService.getPartyPosts(null, null, null);

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).isClosed()).isTrue();
        assertThat(response.get(0).getStatus()).isEqualTo("마감");
    }

    @Test
    void 파티_모집글_작성은_작성자를_현재인원으로_계산한다() {
        // given
        PartyPostCreateRequest request = partyPostCreateRequest("마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 2);
        givenMemberLocked(1L);
        givenNoActivePartyParticipation(1L);
        when(partyPostRepository.save(any(PartyPost.class))).thenAnswer(invocation -> {
            PartyPost partyPost = invocation.getArgument(0);
            ReflectionTestUtils.setField(partyPost, "id", 10L);
            return partyPost;
        });

        // when
        PartyPostResponse response = communityPartyService.createPartyPost(1L, request);

        // then
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCapacity()).isEqualTo("1/2");
        assertThat(response.getChatRoomId()).isEqualTo("party-recruitment");
        assertThat(response.getTags()).contains("마스터+");
        assertThat(response.isJoined()).isTrue();
        InOrder inOrder = inOrder(memberRepository, partyPostRepository, partyApplicationRepository);
        inOrder.verify(memberRepository).findByIdForUpdate(1L);
        inOrder.verify(partyPostRepository).existsActiveOwnedPartyPost(eq(1L), any(LocalDateTime.class));
        inOrder.verify(partyApplicationRepository).existsActiveAcceptedApplication(
                eq(1L),
                eq(PartyApplicationStatus.ACCEPTED),
                any(LocalDateTime.class)
        );
        inOrder.verify(partyPostRepository).save(any(PartyPost.class));
        verify(chatService).ensureRoom("party-recruitment");
        verify(partyApplicationRepository, never()).save(any(PartyApplication.class));
    }

    @Test
    void 파티_모집글_작성_사용자를_잠글_수_없으면_작성할_수_없다() {
        // given
        PartyPostCreateRequest request = partyPostCreateRequest("마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 2);
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> communityPartyService.createPartyPost(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));

        verify(partyPostRepository, never()).existsActiveOwnedPartyPost(any(Long.class), any(LocalDateTime.class));
        verify(partyApplicationRepository, never()).existsActiveAcceptedApplication(
                any(Long.class),
                any(PartyApplicationStatus.class),
                any(LocalDateTime.class)
        );
        verify(partyPostRepository, never()).save(any(PartyPost.class));
        verify(chatService, never()).ensureRoom(any());
        verify(partyApplicationRepository, never()).save(any(PartyApplication.class));
    }

    @Test
    void 이미_활성_모집글을_작성한_사용자는_모집글을_작성할_수_없다() {
        // given
        PartyPostCreateRequest request = partyPostCreateRequest("마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 2);
        givenMemberLocked(1L);
        when(partyPostRepository.existsActiveOwnedPartyPost(eq(1L), any(LocalDateTime.class))).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> communityPartyService.createPartyPost(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PARTY_ALREADY_JOINED));

        verify(partyApplicationRepository, never()).existsActiveAcceptedApplication(
                any(Long.class),
                any(PartyApplicationStatus.class),
                any(LocalDateTime.class)
        );
        verify(partyPostRepository, never()).save(any(PartyPost.class));
        verify(chatService, never()).ensureRoom(any());
        verify(partyApplicationRepository, never()).save(any(PartyApplication.class));
    }

    @Test
    void 이미_다른_활성_파티에_참여한_사용자는_모집글을_작성할_수_없다() {
        // given
        PartyPostCreateRequest request = partyPostCreateRequest("마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 2);
        givenMemberLocked(1L);
        when(partyPostRepository.existsActiveOwnedPartyPost(eq(1L), any(LocalDateTime.class))).thenReturn(false);
        when(partyApplicationRepository.existsActiveAcceptedApplication(
                eq(1L),
                eq(PartyApplicationStatus.ACCEPTED),
                any(LocalDateTime.class)
        )).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> communityPartyService.createPartyPost(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PARTY_ALREADY_JOINED));

        verify(partyPostRepository, never()).save(any(PartyPost.class));
        verify(chatService, never()).ensureRoom(any());
        verify(partyApplicationRepository, never()).save(any(PartyApplication.class));
    }

    @Test
    void 과거_마감시간으로_모집글을_작성할_수_없다() {
        // given
        PartyPostCreateRequest request = partyPostCreateRequest("마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 2);
        ReflectionTestUtils.setField(request, "deadline", LocalDateTime.now().minusMinutes(1));

        // when, then
        assertThatThrownBy(() -> communityPartyService.createPartyPost(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        verify(partyPostRepository, never()).save(any(PartyPost.class));
        verify(chatService, never()).ensureRoom(any());
        verify(partyApplicationRepository, never()).save(any(PartyApplication.class));
    }

    @Test
    void 인증되지_않은_사용자는_모집글을_작성할_수_없다() {
        // given
        PartyPostCreateRequest request = partyPostCreateRequest("마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 2);

        // when, then
        assertThatThrownBy(() -> communityPartyService.createPartyPost(null, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));

        verify(partyPostRepository, never()).save(any(PartyPost.class));
        verify(chatService, never()).ensureRoom(any());
        verify(partyApplicationRepository, never()).save(any(PartyApplication.class));
    }

    @Test
    void 파티_참여는_ACCEPTED_신청을_저장하고_인원을_늘린다() {
        // given
        PartyPost partyPost = partyPost(1L, 1L, "마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 1, 3);
        givenMemberLocked(2L);
        when(partyPostRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(partyPost));
        when(partyApplicationRepository.existsByPartyPostAndUserIdAndStatus(
                partyPost,
                2L,
                PartyApplicationStatus.ACCEPTED
        )).thenReturn(false);
        givenNoOtherActivePartyParticipation(2L, 1L);

        // when
        PartyPostResponse response = communityPartyService.joinParty(2L, 1L);

        // then
        assertThat(response.getCapacity()).isEqualTo("2/3");
        assertThat(response.isJoined()).isTrue();
        InOrder inOrder = inOrder(memberRepository, partyPostRepository);
        inOrder.verify(memberRepository).findByIdForUpdate(2L);
        inOrder.verify(partyPostRepository).findActiveByIdForUpdate(1L);
        verify(partyApplicationRepository).save(any(PartyApplication.class));
    }

    @Test
    void 파티_참여_사용자를_잠글_수_없으면_참여할_수_없다() {
        // given
        when(memberRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> communityPartyService.joinParty(2L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));

        verify(partyPostRepository, never()).findActiveByIdForUpdate(any(Long.class));
        verify(partyPostRepository, never()).existsActiveOwnedPartyPostForOtherParty(
                any(Long.class),
                any(Long.class),
                any(LocalDateTime.class)
        );
        verify(partyApplicationRepository, never()).existsActiveAcceptedApplicationForOtherParty(
                any(Long.class),
                any(PartyApplicationStatus.class),
                any(Long.class),
                any(LocalDateTime.class)
        );
        verify(partyApplicationRepository, never()).save(any(PartyApplication.class));
    }

    @Test
    void 정원이_가득_찬_파티에는_참여할_수_없다() {
        // given
        PartyPost partyPost = partyPost(1L, 1L, "마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 2, 2);
        givenMemberLocked(2L);
        when(partyPostRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(partyPost));
        when(partyApplicationRepository.existsByPartyPostAndUserIdAndStatus(
                partyPost,
                2L,
                PartyApplicationStatus.ACCEPTED
        )).thenReturn(false);
        givenNoOtherActivePartyParticipation(2L, 1L);

        // when, then
        assertThatThrownBy(() -> communityPartyService.joinParty(2L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PARTY_POST_FULL));

        verify(partyApplicationRepository, never()).save(any(PartyApplication.class));
    }

    @Test
    void 마감시간이_지난_파티에는_참여할_수_없다() {
        // given
        PartyPost partyPost = partyPost(1L, 1L, "마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 1, 3);
        ReflectionTestUtils.setField(partyPost, "deadline", LocalDateTime.now().minusMinutes(1));
        givenMemberLocked(2L);
        when(partyPostRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(partyPost));
        when(partyApplicationRepository.existsByPartyPostAndUserIdAndStatus(
                partyPost,
                2L,
                PartyApplicationStatus.ACCEPTED
        )).thenReturn(false);
        givenNoOtherActivePartyParticipation(2L, 1L);

        // when, then
        assertThatThrownBy(() -> communityPartyService.joinParty(2L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PARTY_POST_CLOSED));

        verify(partyApplicationRepository, never()).save(any(PartyApplication.class));
    }

    @Test
    void 이미_참여한_사용자가_다시_참여하면_인원을_늘리지_않는다() {
        // given
        PartyPost partyPost = partyPost(1L, 1L, "마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 1, 3);
        givenMemberLocked(2L);
        when(partyPostRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(partyPost));
        when(partyApplicationRepository.existsByPartyPostAndUserIdAndStatus(
                partyPost,
                2L,
                PartyApplicationStatus.ACCEPTED
        )).thenReturn(true);

        // when
        PartyPostResponse response = communityPartyService.joinParty(2L, 1L);

        // then
        assertThat(response.getCapacity()).isEqualTo("1/3");
        assertThat(response.isJoined()).isTrue();
        verify(partyApplicationRepository, never()).save(any(PartyApplication.class));
        verify(partyPostRepository, never()).existsActiveOwnedPartyPostForOtherParty(
                any(Long.class),
                any(Long.class),
                any(LocalDateTime.class)
        );
        verify(partyApplicationRepository, never()).existsActiveAcceptedApplicationForOtherParty(
                any(Long.class),
                any(PartyApplicationStatus.class),
                any(Long.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    void 이미_다른_활성_파티에_참여한_사용자는_새_파티에_참여할_수_없다() {
        // given
        PartyPost partyPost = partyPost(1L, 1L, "마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 1, 3);
        givenMemberLocked(2L);
        when(partyPostRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(partyPost));
        when(partyApplicationRepository.existsByPartyPostAndUserIdAndStatus(
                partyPost,
                2L,
                PartyApplicationStatus.ACCEPTED
        )).thenReturn(false);
        when(partyPostRepository.existsActiveOwnedPartyPostForOtherParty(
                eq(2L),
                eq(1L),
                any(LocalDateTime.class)
        )).thenReturn(false);
        when(partyApplicationRepository.existsActiveAcceptedApplicationForOtherParty(
                eq(2L),
                eq(PartyApplicationStatus.ACCEPTED),
                eq(1L),
                any(LocalDateTime.class)
        )).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> communityPartyService.joinParty(2L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PARTY_ALREADY_JOINED));

        verify(partyApplicationRepository, never()).save(any(PartyApplication.class));
    }

    @Test
    void 다른_활성_모집글을_작성한_사용자는_새_파티에_참여할_수_없다() {
        // given
        PartyPost partyPost = partyPost(1L, 1L, "마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 1, 3);
        givenMemberLocked(2L);
        when(partyPostRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(partyPost));
        when(partyApplicationRepository.existsByPartyPostAndUserIdAndStatus(
                partyPost,
                2L,
                PartyApplicationStatus.ACCEPTED
        )).thenReturn(false);
        when(partyPostRepository.existsActiveOwnedPartyPostForOtherParty(
                eq(2L),
                eq(1L),
                any(LocalDateTime.class)
        )).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> communityPartyService.joinParty(2L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PARTY_ALREADY_JOINED));

        verify(partyApplicationRepository, never()).existsActiveAcceptedApplicationForOtherParty(
                any(Long.class),
                any(PartyApplicationStatus.class),
                any(Long.class),
                any(LocalDateTime.class)
        );
        verify(partyApplicationRepository, never()).save(any(PartyApplication.class));
    }

    @Test
    void 참여_취소는_ACCEPTED_신청을_삭제하고_인원을_줄인다() {
        // given
        PartyPost partyPost = partyPost(1L, 1L, "마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 2, 2);
        PartyApplication application = PartyApplication.accepted(partyPost, 2L);
        when(partyPostRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(partyPost));
        when(partyApplicationRepository.findByPartyPostAndUserIdAndStatus(
                partyPost,
                2L,
                PartyApplicationStatus.ACCEPTED
        )).thenReturn(Optional.of(application));

        // when
        PartyPostResponse response = communityPartyService.cancelJoinParty(2L, 1L);

        // then
        assertThat(response.getCapacity()).isEqualTo("1/2");
        assertThat(response.isJoined()).isFalse();
        verify(partyApplicationRepository).delete(application);
    }

    @Test
    void 작성자는_참여_취소를_할_수_없다() {
        // given
        PartyPost partyPost = partyPost(1L, 1L, "마스터 듀오 구합니다", PartyGameMode.RANKED_TFT, 1, 2);
        when(partyPostRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(partyPost));

        // when, then
        assertThatThrownBy(() -> communityPartyService.cancelJoinParty(1L, 1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    private PartyPost partyPost(
            Long id,
            Long userId,
            String title,
            PartyGameMode gameMode,
            int currentMembers,
            int maxMembers
    ) {
        PartyPost partyPost = PartyPost.create(userId, partyPostCreateRequest(title, gameMode, maxMembers));
        ReflectionTestUtils.setField(partyPost, "id", id);
        ReflectionTestUtils.setField(partyPost, "currentMembers", currentMembers);
        ReflectionTestUtils.setField(partyPost, "closed", currentMembers >= maxMembers);
        return partyPost;
    }

    private PartyPostCreateRequest partyPostCreateRequest(String title, PartyGameMode gameMode, int maxMembers) {
        PartyPostCreateRequest request = new PartyPostCreateRequest();
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "content", "저녁 랭크 같이 하실 분 구합니다.");
        ReflectionTestUtils.setField(request, "gameMode", gameMode);
        ReflectionTestUtils.setField(request, "maxMembers", maxMembers);
        ReflectionTestUtils.setField(request, "tags", List.of("마스터+", "음성 가능", "순방 목표"));
        return request;
    }

    private void givenMemberLocked(Long userId) {
        when(memberRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(Member.builder()
                .email("member" + userId + "@example.com")
                .passwordHash("encoded-password")
                .nickname("회원" + userId)
                .build()));
    }

    private void givenNoActivePartyParticipation(Long userId) {
        when(partyPostRepository.existsActiveOwnedPartyPost(
                eq(userId),
                any(LocalDateTime.class)
        )).thenReturn(false);
        when(partyApplicationRepository.existsActiveAcceptedApplication(
                eq(userId),
                eq(PartyApplicationStatus.ACCEPTED),
                any(LocalDateTime.class)
        )).thenReturn(false);
    }

    private void givenNoOtherActivePartyParticipation(Long userId, Long partyPostId) {
        when(partyPostRepository.existsActiveOwnedPartyPostForOtherParty(
                eq(userId),
                eq(partyPostId),
                any(LocalDateTime.class)
        )).thenReturn(false);
        when(partyApplicationRepository.existsActiveAcceptedApplicationForOtherParty(
                eq(userId),
                eq(PartyApplicationStatus.ACCEPTED),
                eq(partyPostId),
                any(LocalDateTime.class)
        )).thenReturn(false);
    }
}
