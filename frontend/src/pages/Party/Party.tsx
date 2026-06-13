import { AppLayout } from '../../components/layout'
import PartyChatPanel from './components/PartyChatPanel'
import PartyCreateForm from './components/PartyCreateForm'
import PartyFilterBar from './components/PartyFilterBar'
import PartyPostList from './components/PartyPostList'
import { usePartyChat } from './hooks/usePartyChat'
import { usePartyPosts } from './hooks/usePartyPosts'
import styles from './Party.module.css'

function Party() {
  const chat = usePartyChat()
  const party = usePartyPosts({ onPartyMessage: chat.appendPartyMessage })

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
            descriptionDraft={party.descriptionDraft}
            modeDraft={party.modeDraft}
            onCapacityChange={party.setCapacityDraft}
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

          <PartyPostList
            currentPage={party.currentPage}
            joinedPostId={party.joinedPostId}
            onJoinToggle={party.toggleJoin}
            onPageChange={party.setCurrentPage}
            posts={party.pageItems}
            totalPages={party.totalPages}
          />
        </section>

        <PartyChatPanel
          activeMessages={chat.activeMessages}
          activeRoomName={chat.activeRoomName}
          chatInput={chat.chatInput}
          onActiveRoomChange={chat.setActiveRoomName}
          onChatInputChange={chat.setChatInput}
          onMessageSubmit={chat.sendMessage}
          rooms={chat.rooms}
        />
      </div>
    </AppLayout>
  )
}

export default Party
