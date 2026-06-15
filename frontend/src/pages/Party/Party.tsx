import { useEffect, useMemo, useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import {
  COMMUNITY_CHAT_ROOM_QUERY_PARAM,
  DEFAULT_COMMUNITY_CHAT_ROOM_ID,
  normalizeCommunityChatRoomId,
  type CommunityChatRoomId,
} from '../../constants/communityChatRooms'
import PartyChatPanel from './components/PartyChatPanel'
import PartyCreateForm from './components/PartyCreateForm'
import PartyFilterBar from './components/PartyFilterBar'
import PartyPostList from './components/PartyPostList'
import { usePartyChat } from './hooks/usePartyChat'
import { usePartyPosts } from './hooks/usePartyPosts'
import styles from './Party.module.css'

function Party() {
  const [searchParams, setSearchParams] = useSearchParams()
  const activeRoomId = useMemo(
    () =>
      normalizeCommunityChatRoomId(searchParams.get(COMMUNITY_CHAT_ROOM_QUERY_PARAM))
        ?? DEFAULT_COMMUNITY_CHAT_ROOM_ID,
    [searchParams],
  )
  const setActiveRoomId = useCallback(
    (roomId: CommunityChatRoomId) => {
      if (searchParams.get(COMMUNITY_CHAT_ROOM_QUERY_PARAM) === roomId) {
        return
      }

      const nextParams = new URLSearchParams(searchParams)
      nextParams.set(COMMUNITY_CHAT_ROOM_QUERY_PARAM, roomId)
      setSearchParams(nextParams, { replace: true })
    },
    [searchParams, setSearchParams],
  )
  const chat = usePartyChat({
    activeRoomId,
    onActiveRoomChange: setActiveRoomId,
  })
  const party = usePartyPosts({
    onPartyMessage: chat.appendPartyMessage,
    onPartyPostCreated: chat.preparePartyRoom,
  })

  useEffect(() => {
    setActiveRoomId(activeRoomId)
  }, [activeRoomId, setActiveRoomId])

  return (
    <AppLayout>
      <div className={styles.communityPage}>
        <header className={styles.pageHeader}>
          <div>
            <p>Community</p>
            <h1>커뮤니티</h1>
          </div>
          <span>파티 모집과 실시간 채팅을 한 화면에서 확인하세요.</span>
        </header>

        <section className={`${styles.panel} ${styles.partyPanel}`}>
          <div className={styles.panelHeader}>
            <div>
              <h2>파티원 찾기</h2>
              <p>티어와 플레이 스타일에 맞는 TFT 듀오를 찾아보세요.</p>
            </div>
            <button type="button" className={styles.primaryButton} onClick={party.focusCompose}>
              모집글 작성
            </button>
          </div>

          <PartyFilterBar
            onFilterChange={party.changeFilter}
            onSearchChange={party.updateSearchDraft}
            onSearchSubmit={party.submitSearch}
            searchDraft={party.searchDraft}
            selectedFilter={party.selectedFilter}
          />

          <PartyCreateForm
            capacityDraft={party.capacityDraft}
            composeError={party.composeError}
            deadlineDraft={party.deadlineDraft}
            descriptionDraft={party.descriptionDraft}
            isAuthenticated={party.isAuthenticated}
            isSubmitting={party.isCreating}
            minDeadline={party.minDeadline}
            modeDraft={party.modeDraft}
            onCapacityChange={party.setCapacityDraft}
            onDeadlineChange={party.setDeadlineDraft}
            onDescriptionChange={party.setDescriptionDraft}
            onModeChange={party.setModeDraft}
            onSubmit={party.submitPartyPost}
            onTagsChange={party.setTagsDraft}
            onTierChange={party.setTierDraft}
            onTitleChange={party.setTitleDraft}
            tagsDraft={party.tagsDraft}
            tierDraft={party.tierDraft}
            titleDraft={party.titleDraft}
            titleInputRef={party.titleInputRef}
          />

          {party.statusMessage && (
            <p className={styles.statusMessage} role="status">
              {party.statusMessage}
            </p>
          )}

          <PartyPostList
            currentPage={party.currentPage}
            isAuthenticated={party.isAuthenticated}
            joinedPostId={party.joinedPostId}
            joiningPostId={party.joiningPostId}
            onJoinToggle={party.toggleJoin}
            onPageChange={party.setCurrentPage}
            posts={party.pageItems}
            totalPages={party.totalPages}
          />
        </section>

        <PartyChatPanel
          activeMessages={chat.activeMessages}
          activeRoomId={chat.activeRoomId}
          activeRoomName={chat.activeRoomName}
          chatInput={chat.chatInput}
          chatNotice={chat.chatNotice}
          connectionLabel={chat.connectionLabel}
          currentUserName={chat.currentUserName}
          isLoading={chat.isLoading}
          isMessageDisabled={chat.isMessageDisabled}
          onActiveRoomChange={setActiveRoomId}
          onChatInputChange={chat.setChatInput}
          onMessageSubmit={chat.sendMessage}
          rooms={chat.rooms}
        />
      </div>
    </AppLayout>
  )
}

export default Party
