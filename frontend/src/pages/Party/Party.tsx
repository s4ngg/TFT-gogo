import { type FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import {
  Clock3,
  Crown,
  Leaf,
  MessageCircle,
  Search,
  Send,
  Sparkles,
  Swords,
  Users,
} from 'lucide-react'
import { AppLayout } from '../../components/layout'
import useAuthStore from '../../store/useAuthStore'
import { partyFilters, type PartyFilter } from './partyFilters'
import { useRealtimeChat } from './hooks/useRealtimeChat'
import styles from './Party.module.css'

interface PartyPost {
  capacity: string
  close: string
  description: string
  icon: 'crown' | 'leaf' | 'spark' | 'swords'
  id: string
  mode: Exclude<PartyFilter, '전체'>
  status: '모집중' | '대기중'
  tags: string[]
  tier: string
  title: string
  tone: 'purple' | 'green' | 'cyan' | 'gold'
}

interface ChatRoom {
  id: string
  lastMessage: string
  name: string
  users: string
}

const initialPartyPosts: PartyPost[] = [
  {
    id: 'party-master-duo',
    title: '마스터 이상 듀오 구합니다',
    mode: '랭크',
    tier: '마스터+',
    capacity: '2/2',
    close: '마감 15분 전',
    status: '모집중',
    description: '17.3 추천 메타 기준으로 빠르게 점수 올리실 분 찾아요.',
    tags: ['음성 가능', '연승 목표', '빠른 매칭'],
    icon: 'crown',
    tone: 'purple',
  },
  {
    id: 'party-diamond-practice',
    title: '다이아 구간 야부/연습 같이해요',
    mode: '랭크',
    tier: '다이아+',
    capacity: '1/2',
    close: '마감 42분 전',
    status: '모집중',
    description: '운영 피드백 주고받으면서 편하게 연습하실 분이면 좋아요.',
    tags: ['피드백 환영', '저녁 접속', '마이크 선택'],
    icon: 'leaf',
    tone: 'green',
  },
  {
    id: 'party-casual-evening',
    title: '저녁 근접, 편하게 즐기실 분!',
    mode: '일반',
    tier: '제한 없음',
    capacity: '3/4',
    close: '마감 1시간 전',
    status: '대기중',
    description: '랭크 부담 없이 조합 테스트하면서 같이 하실 분 구해요.',
    tags: ['초보 환영', '일반전', '덱 실험'],
    icon: 'spark',
    tone: 'cyan',
  },
  {
    id: 'party-weekend-master',
    title: '주말 마스터 달성 목표!',
    mode: '랭크',
    tier: '플래티넘+',
    capacity: '2/3',
    close: '마감 2시간 전',
    status: '모집중',
    description: '순방 위주로 안정적인 운영 맞춰가실 분이면 좋습니다.',
    tags: ['주말 집중', '순방 운영', '멘탈 좋음'],
    icon: 'swords',
    tone: 'gold',
  },
]

const initialChatRooms: ChatRoom[] = [
  { id: 'general', name: '일반', users: '1,234', lastMessage: '새로운 패치 적응 중입니다!' },
  { id: 'deck-guide', name: '덱 공략', users: '856', lastMessage: '증강 추천 부탁드려요' },
  { id: 'party-recruitment', name: '파티 모집', users: '622', lastMessage: '마스터 듀오 구해요~' },
  { id: 'question-answer', name: '질문 & 답변', users: '741', lastMessage: '초보 운영 질문 있습니다' },
]

const partyIconMap = {
  crown: Crown,
  leaf: Leaf,
  spark: Sparkles,
  swords: Swords,
}

function formatMessageTime(createdAt: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(createdAt))
}

function normalizeCapacity(value: string) {
  const trimmedValue = value.trim()
  const slashMatch = trimmedValue.match(/^(\d+)\/(\d+)$/)

  if (slashMatch) {
    const current = Number(slashMatch[1])
    const total = Number(slashMatch[2])

    if (Number.isFinite(current) && Number.isFinite(total) && total > 0) {
      return `${Math.min(current, total)}/${total}`
    }
  }

  const total = Number(trimmedValue)

  if (Number.isFinite(total) && total > 0) {
    return `1/${total}`
  }

  return '1/2'
}

function parseCapacity(capacity: string) {
  const [currentRaw, totalRaw] = capacity.split('/').map(Number)
  const total = Number.isFinite(totalRaw) && totalRaw > 0 ? totalRaw : 2
  const current = Number.isFinite(currentRaw) ? Math.min(Math.max(currentRaw, 0), total) : 0

  return { current, total }
}

function getPostStyle(mode: Exclude<PartyFilter, '전체'>, tier: string) {
  if (mode === '일반') {
    return { icon: 'spark' as const, tone: 'cyan' as const }
  }

  if (mode === '커스텀') {
    return { icon: 'swords' as const, tone: 'gold' as const }
  }

  if (tier.includes('다이아')) {
    return { icon: 'leaf' as const, tone: 'green' as const }
  }

  return { icon: 'crown' as const, tone: 'purple' as const }
}

function Party() {
  const authUser = useAuthStore((state) => state.user)
  const authToken = useAuthStore((state) => state.token)
  const [posts, setPosts] = useState<PartyPost[]>(initialPartyPosts)
  const [rooms, setRooms] = useState<ChatRoom[]>(initialChatRooms)
  const [selectedFilter, setSelectedFilter] = useState<PartyFilter>('전체')
  const [searchDraft, setSearchDraft] = useState('')
  const [query, setQuery] = useState('')
  const [activeRoomId, setActiveRoomId] = useState(initialChatRooms[0]?.id ?? 'general')
  const [chatInput, setChatInput] = useState('')
  const [chatStatusMessage, setChatStatusMessage] = useState('')
  const [joinedPostId, setJoinedPostId] = useState<string | null>(null)
  const [titleDraft, setTitleDraft] = useState('')
  const [modeDraft, setModeDraft] = useState<Exclude<PartyFilter, '전체'>>('랭크')
  const [tierDraft, setTierDraft] = useState('마스터+')
  const [capacityDraft, setCapacityDraft] = useState('2')
  const [tagsDraft, setTagsDraft] = useState('')
  const [descriptionDraft, setDescriptionDraft] = useState('')
  const [composeError, setComposeError] = useState('')
  const titleInputRef = useRef<HTMLInputElement>(null)
  const {
    connectionStatus,
    errorMessage: chatErrorMessage,
    isLoading: isChatLoading,
    isSending: isChatSending,
    messages: activeMessages,
    queryError: chatQueryError,
    sendMessage,
  } = useRealtimeChat(activeRoomId, Boolean(authToken))
  const currentUserName = authUser?.nickname ?? authUser?.summonerName ?? '나'
  const currentUserTier = authUser?.tier ?? 'Unranked'
  const isChatAvailable = Boolean(authToken)
  const activeRoom = rooms.find((room) => room.id === activeRoomId) ?? rooms[0]
  const filteredPartyPosts = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()

    return posts.filter((post) => {
      const matchesFilter = selectedFilter === '전체' || post.mode === selectedFilter
      const searchableText = [
        post.title,
        post.mode,
        post.tier,
        post.description,
        ...post.tags,
      ].join(' ').toLowerCase()
      const matchesQuery = normalizedQuery.length === 0 || searchableText.includes(normalizedQuery)

      return matchesFilter && matchesQuery
    })
  }, [posts, query, selectedFilter])
  useEffect(() => {
    const lastMessage = activeMessages[activeMessages.length - 1]

    if (!lastMessage) {
      return
    }

    setRooms((currentRooms) =>
      currentRooms.map((room) =>
        room.id === lastMessage.roomId ? { ...room, lastMessage: lastMessage.content } : room,
      ),
    )
  }, [activeMessages])

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setQuery(searchDraft)
  }

  const handleComposeFocus = () => {
    titleInputRef.current?.focus()
    setComposeError('')
  }

  const handlePartySubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const title = titleDraft.trim()
    const description = descriptionDraft.trim()

    if (!title || !description) {
      setComposeError('모집글 제목과 플레이 스타일을 입력해주세요.')
      return
    }

    const style = getPostStyle(modeDraft, tierDraft)
    const parsedTags = tagsDraft
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean)
      .slice(0, 4)
    const nextPost: PartyPost = {
      id: `party-${crypto.randomUUID()}`,
      title,
      mode: modeDraft,
      tier: tierDraft,
      capacity: normalizeCapacity(capacityDraft),
      close: '방금 등록',
      status: '모집중',
      description,
      tags: parsedTags.length > 0 ? parsedTags : [modeDraft, tierDraft, '방금 등록'],
      ...style,
    }

    setPosts((currentPosts) => [nextPost, ...currentPosts])
    setSelectedFilter('전체')
    setSearchDraft('')
    setQuery('')
    setTitleDraft('')
    setModeDraft('랭크')
    setTierDraft('마스터+')
    setCapacityDraft('2')
    setTagsDraft('')
    setDescriptionDraft('')
    setComposeError('')
  }

  const handleJoinToggle = async (postId: string) => {
    const targetPost = posts.find((post) => post.id === postId)

    if (!targetPost) {
      return
    }

    const alreadyJoined = joinedPostId === postId
    const { current, total } = parseCapacity(targetPost.capacity)

    if (!alreadyJoined && (joinedPostId !== null || current >= total)) {
      return
    }

    const nextCurrent = alreadyJoined ? Math.max(0, current - 1) : Math.min(total, current + 1)
    const nextMessage = alreadyJoined
      ? `${targetPost.title} 참여 신청을 취소했어요.`
      : `${targetPost.title} 참여 신청했습니다. (${nextCurrent}/${total})`
    const partyRoomId = `party-${postId}`

    setPosts((currentPosts) =>
      currentPosts.map((post) => {
        if (post.id !== postId) {
          return post
        }

        return {
          ...post,
          capacity: `${nextCurrent}/${total}`,
          status: nextCurrent >= total ? '대기중' : '모집중',
        }
      }),
    )

    setJoinedPostId(alreadyJoined ? null : postId)
    setRooms((currentRooms) =>
      currentRooms.some((room) => room.id === partyRoomId)
        ? currentRooms.map((room) =>
            room.id === partyRoomId ? { ...room, users: `${nextCurrent}/${total}` } : room,
          )
        : [
            ...currentRooms,
            {
              id: partyRoomId,
              lastMessage: '파티 채팅방이 열렸습니다.',
              name: targetPost.title,
              users: `${nextCurrent}/${total}`,
            },
          ],
    )
    setActiveRoomId(partyRoomId)
    setChatStatusMessage('')

    if (!isChatAvailable) {
      setChatStatusMessage('로그인 후 파티 채팅 알림을 보낼 수 있습니다.')
      return
    }

    try {
      const sentMessage = await sendMessage({
        content: nextMessage,
        roomId: partyRoomId,
        senderName: currentUserName,
        tier: currentUserTier,
      })

      setRooms((currentRooms) =>
        currentRooms.map((room) =>
          room.id === sentMessage.roomId ? { ...room, lastMessage: sentMessage.content } : room,
        ),
      )
    } catch {
      setChatStatusMessage('참여 상태는 반영됐지만 채팅 알림 전송에 실패했습니다.')
    }
  }

  const handleMessageSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const trimmedMessage = chatInput.trim()

    if (trimmedMessage.length === 0 || !activeRoom || !isChatAvailable) {
      return
    }

    try {
      const sentMessage = await sendMessage({
        content: trimmedMessage,
        senderName: currentUserName,
        tier: currentUserTier,
      })

      setRooms((currentRooms) =>
        currentRooms.map((room) =>
          room.id === sentMessage.roomId ? { ...room, lastMessage: sentMessage.content } : room,
        ),
      )
      setChatInput('')
      setChatStatusMessage('')
    } catch {
      setChatStatusMessage('메시지 전송에 실패했습니다.')
    }
  }

  const connectionLabel = connectionStatus === 'connected'
    ? '실시간 연결됨'
    : connectionStatus === 'connecting'
      ? '연결 중'
      : '실시간 연결 대기'
  const chatNotice = !isChatAvailable
    ? '로그인 후 채팅을 조회하고 메시지를 보낼 수 있습니다.'
    : chatStatusMessage || chatErrorMessage || (chatQueryError ? '채팅 메시지를 불러오지 못했습니다.' : '')
  const isMessageDisabled = !isChatAvailable || isChatSending || !activeRoom

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
            <button type="button" className={styles.primaryButton} onClick={handleComposeFocus}>
              모집글 작성
            </button>
          </div>

          <div className={styles.toolbar}>
            <form className={styles.searchBox} onSubmit={handleSearchSubmit}>
              <Search size={18} />
              <input
                aria-label="파티 모집 검색"
                onChange={(event) => {
                  const nextQuery = event.target.value

                  setSearchDraft(nextQuery)
                  setQuery(nextQuery)
                }}
                placeholder="티어, 모드, 키워드 검색"
                value={searchDraft}
              />
              <button type="submit">검색</button>
            </form>
            <div className={styles.filterTabs} aria-label="파티원 찾기 필터">
              {partyFilters.map((filter) => (
                <button
                  aria-pressed={selectedFilter === filter}
                  className={selectedFilter === filter ? styles.selectedTab : undefined}
                  key={filter}
                  onClick={() => setSelectedFilter(filter)}
                  type="button"
                >
                  {filter}
                </button>
              ))}
            </div>
          </div>

          <form
            className={styles.composeBox}
            onSubmit={handlePartySubmit}
          >
            <input
              aria-label="모집글 제목"
              onChange={(event) => setTitleDraft(event.target.value)}
              placeholder="모집글 제목"
              ref={titleInputRef}
              value={titleDraft}
            />
            <select
              aria-label="모집 모드"
              onChange={(event) => setModeDraft(event.target.value as Exclude<PartyFilter, '전체'>)}
              value={modeDraft}
            >
              <option value="랭크">랭크</option>
              <option value="일반">일반</option>
              <option value="커스텀">커스텀</option>
            </select>
            <select
              aria-label="티어 조건"
              onChange={(event) => setTierDraft(event.target.value)}
              value={tierDraft}
            >
              <option value="마스터+">마스터+</option>
              <option value="다이아+">다이아+</option>
              <option value="플래티넘+">플래티넘+</option>
              <option value="제한 없음">제한 없음</option>
            </select>
            <input
              aria-label="모집 인원"
              onChange={(event) => setCapacityDraft(event.target.value)}
              placeholder="모집 인원"
              value={capacityDraft}
            />
            <input
              aria-label="모집 태그"
              className={styles.tagInput}
              onChange={(event) => setTagsDraft(event.target.value)}
              placeholder="태그 입력 예: 음성 가능, 연습 목표, 빠른 매칭"
              value={tagsDraft}
            />
            <textarea
              aria-label="모집 메모"
              onChange={(event) => setDescriptionDraft(event.target.value)}
              placeholder="플레이 스타일이나 요청사항을 적어주세요."
              value={descriptionDraft}
            />
            <button type="submit" className={styles.primaryButton}>
              등록
            </button>
            {composeError && <p className={styles.composeError}>{composeError}</p>}
          </form>

          <div className={styles.partyList}>
            {filteredPartyPosts.length > 0 ? (
              filteredPartyPosts.map((post) => {
                const Icon = partyIconMap[post.icon]
                const { current, total } = parseCapacity(post.capacity)
                const isJoined = joinedPostId === post.id
                const hasJoinedOtherPost = joinedPostId !== null && !isJoined
                const isFull = current >= total

                return (
                  <article className={styles.partyCard} key={post.id}>
                    <div className={`${styles.partyIcon} ${styles[post.tone]}`}>
                      <Icon size={28} strokeWidth={2.2} />
                    </div>
                    <div className={styles.partyContent}>
                      <div className={styles.partyTitleLine}>
                        <h3>{post.title}</h3>
                        <span>{isFull ? '마감' : post.status}</span>
                      </div>
                      <p>{post.description}</p>
                      <div className={styles.partyMeta}>
                        <span>{post.mode}</span>
                        <span>{post.tier}</span>
                        <span>
                          <Users size={15} />
                          {post.capacity}
                        </span>
                        <span>
                          <Clock3 size={15} />
                          {post.close}
                        </span>
                      </div>
                      <div className={styles.partyTags}>
                        {post.tags.map((tag, index) => (
                          <small key={`${post.id}-${tag}-${index}`}>{tag}</small>
                        ))}
                      </div>
                    </div>
                    <button
                      type="button"
                      aria-pressed={isJoined}
                      className={styles.joinButton}
                      disabled={(isFull && !isJoined) || hasJoinedOtherPost}
                      onClick={() => handleJoinToggle(post.id)}
                    >
                      {isJoined ? '참여중' : isFull ? '마감' : hasJoinedOtherPost ? '잠김' : '참여'}
                    </button>
                  </article>
                )
              })
            ) : (
              <p className={styles.emptyState}>조건에 맞는 모집글이 없습니다.</p>
            )}
          </div>
        </section>

        <section className={`${styles.panel} ${styles.chatPanel}`}>
          <div className={styles.panelHeader}>
            <div>
              <h2>실시간 채팅</h2>
              <p>현재 접속 중인 유저들과 빠르게 정보를 나눠보세요.</p>
            </div>
            <span className={styles.onlineBadge}>{connectionLabel}</span>
          </div>

          <div className={styles.chatLayout}>
            <aside className={styles.channelList} aria-label="채팅 채널">
              {rooms.map((room) => (
                <button
                  aria-pressed={activeRoomId === room.id}
                  className={activeRoomId === room.id ? styles.activeChannel : undefined}
                  onClick={() => setActiveRoomId(room.id)}
                  type="button"
                  key={room.id}
                >
                  <strong># {room.name}</strong>
                  <span>
                    <Users size={14} />
                    {room.users}
                  </span>
                  <small>{room.lastMessage}</small>
                </button>
              ))}
            </aside>

            <div className={styles.chatWindow}>
              <div className={styles.chatWindowHeader}>
                <strong># {activeRoom?.name ?? '채팅'}</strong>
                <span>
                  {isChatLoading
                    ? '메시지 불러오는 중'
                    : activeMessages.length > 0
                    ? `새 메시지 ${activeMessages.length}개`
                    : '대화를 시작해보세요'}
                </span>
              </div>
              {chatNotice && (
                <p className={styles.chatStatus} role="status" aria-live="polite">
                  {chatNotice}
                </p>
              )}
              <div className={styles.messageList}>
                {activeMessages.length > 0 ? (
                  activeMessages.map((chat) => (
                    <article
                      className={chat.senderName === currentUserName ? styles.myMessage : undefined}
                      key={chat.id}
                    >
                      <div>
                        <strong>{chat.senderName}</strong>
                        <span>{chat.tier}</span>
                        <time>{formatMessageTime(chat.createdAt)}</time>
                      </div>
                      <p>{chat.content}</p>
                    </article>
                  ))
                ) : (
                  <p className={styles.chatEmpty}>아직 이 채널에는 메시지가 없습니다.</p>
                )}
              </div>
              <form className={styles.messageForm} onSubmit={handleMessageSubmit}>
                <MessageCircle size={19} />
                <input
                  aria-label="채팅 메시지 입력"
                  disabled={isMessageDisabled}
                  onChange={(event) => setChatInput(event.target.value)}
                  placeholder={isChatAvailable ? '메시지를 입력하세요' : '로그인 후 채팅 가능'}
                  value={chatInput}
                />
                <button
                  type="submit"
                  aria-disabled={isMessageDisabled}
                  aria-label="메시지 보내기"
                  disabled={isMessageDisabled}
                >
                  <Send size={18} />
                </button>
              </form>
            </div>
          </div>
        </section>
      </div>
    </AppLayout>
  )
}

export default Party
