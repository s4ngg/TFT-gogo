import { type FormEvent, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
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
import { partyFilters, type PartyFilter } from './partyFilters'
import {
  buildLocalPartyPost,
  cancelPartyJoin,
  createPartyPost,
  fallbackPartyPosts,
  getPartyPosts,
  joinPartyPost,
  type CreatePartyPostRequest,
  type PartyMode,
  type PartyPost,
  type PartyPostsResult,
} from '../../api/partyApi'
import styles from './Party.module.css'

interface ChatMessage {
  isMine?: boolean
  message: string
  name: string
  roomName: string
  time: string
  tier: string
}

interface ChatRoom {
  lastMessage: string
  name: string
  users: string
}

const initialChatRooms: ChatRoom[] = [
  { name: '일반', users: '1,234', lastMessage: '새로운 패치 적응 중입니다!' },
  { name: '덱 공략', users: '856', lastMessage: '증강 추천 부탁드려요' },
  { name: '파티 모집', users: '622', lastMessage: '마스터 듀오 구해요~' },
  { name: '질문 & 답변', users: '741', lastMessage: '초보 운영 질문 있습니다' },
]

const chatMessages: ChatMessage[] = [
  { roomName: '일반', name: '정동글', tier: 'Master', message: '선봉대 벡스 지금도 순방률 괜찮나요?', time: '14:58' },
  { roomName: '일반', name: '새벽의달', tier: 'Diamond', message: '초반에 벡스 2성만 빨리 붙으면 꽤 안정적이에요.', time: '14:59' },
  { roomName: '일반', name: '응의자', tier: 'Platinum', message: '아이템은 보건보다 블루 먼저 보는 게 나을까요?', time: '15:00' },
  { roomName: '일반', name: 'TFTgogo', tier: 'System', message: '17.3 패치 기준 추천 메타가 업데이트되었습니다.', time: '15:01' },
  {
    roomName: '일반',
    name: '나',
    tier: 'Diamond',
    message: '파티 모집 쪽에 같이 하실 분 있으면 바로 들어갈게요.',
    time: '15:02',
    isMine: true,
  },
  { roomName: '덱 공략', name: '운영연습', tier: 'Diamond', message: '전투 증강 첫 선택이면 어떤 조합이 좋아요?', time: '14:54' },
  { roomName: '덱 공략', name: '메타분석가', tier: 'Master', message: '초반에는 선봉대나 요새 기반으로 피 관리 추천해요.', time: '14:55' },
  { roomName: '파티 모집', name: '플레러너', tier: 'Platinum', message: '플래티넘 듀오 한 자리 남았습니다.', time: '14:50' },
  { roomName: '파티 모집', name: '순방중독', tier: 'Master', message: '마스터 이상 저녁 랭크 같이 하실 분?', time: '14:56' },
  { roomName: '질문 & 답변', name: '입문자', tier: 'Gold', message: '아이템 우선순위는 캐리부터 맞추면 되나요?', time: '14:48' },
  { roomName: '질문 & 답변', name: '코치봇', tier: 'System', message: '캐리 3신기와 앞라인 탱템 균형을 같이 보는 편이 좋아요.', time: '14:49' },
]

const partyIconMap = {
  crown: Crown,
  leaf: Leaf,
  spark: Sparkles,
  swords: Swords,
}

function getCurrentTime() {
  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date())
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

function replacePost(posts: PartyPost[], nextPost: PartyPost) {
  return posts.map((post) => (post.id === nextPost.id ? nextPost : post))
}

function restorePostOverride(
  overrides: Record<string, PartyPost>,
  postId: string,
  previousOverride: PartyPost | undefined,
) {
  if (previousOverride) {
    return {
      ...overrides,
      [postId]: previousOverride,
    }
  }

  const nextOverrides = { ...overrides }
  delete nextOverrides[postId]

  return nextOverrides
}

function removePostOverride(overrides: Record<string, PartyPost>, postId: string) {
  const nextOverrides = { ...overrides }
  delete nextOverrides[postId]

  return nextOverrides
}

function updatePostJoinState(post: PartyPost, isJoining: boolean): PartyPost {
  const { current, total } = parseCapacity(post.capacity)
  const nextCurrent = isJoining ? Math.min(total, current + 1) : Math.max(0, current - 1)

  return {
    ...post,
    capacity: `${nextCurrent}/${total}`,
    isJoined: isJoining,
    status: nextCurrent >= total ? '대기중' : '모집중',
  }
}

function getDefaultDeadlineInput() {
  const deadline = new Date(Date.now() + 1000 * 60 * 60 * 2)
  const year = deadline.getFullYear()
  const month = String(deadline.getMonth() + 1).padStart(2, '0')
  const day = String(deadline.getDate()).padStart(2, '0')
  const hour = String(deadline.getHours()).padStart(2, '0')
  const minute = String(deadline.getMinutes()).padStart(2, '0')

  return `${year}-${month}-${day}T${hour}:${minute}`
}

function Party() {
  const queryClient = useQueryClient()
  const [localPosts, setLocalPosts] = useState<PartyPost[]>([])
  const [postOverrides, setPostOverrides] = useState<Record<string, PartyPost>>({})
  const [rooms, setRooms] = useState<ChatRoom[]>(initialChatRooms)
  const [selectedFilter, setSelectedFilter] = useState<PartyFilter>('전체')
  const [searchDraft, setSearchDraft] = useState('')
  const [query, setQuery] = useState('')
  const [activeRoomName, setActiveRoomName] = useState(initialChatRooms[0]?.name ?? '일반')
  const [messages, setMessages] = useState<ChatMessage[]>(chatMessages)
  const [chatInput, setChatInput] = useState('')
  const [joinedPostId, setJoinedPostId] = useState<string | null>(null)
  const [titleDraft, setTitleDraft] = useState('')
  const [modeDraft, setModeDraft] = useState<PartyMode>('랭크')
  const [tierDraft, setTierDraft] = useState('마스터+')
  const [capacityDraft, setCapacityDraft] = useState('2')
  const [deadlineDraft, setDeadlineDraft] = useState(getDefaultDeadlineInput)
  const [tagsDraft, setTagsDraft] = useState('')
  const [descriptionDraft, setDescriptionDraft] = useState('')
  const [composeError, setComposeError] = useState('')
  const [partyStatusMessage, setPartyStatusMessage] = useState('')
  const titleInputRef = useRef<HTMLInputElement>(null)
  const joinRequestPostIdRef = useRef<string | null>(null)
  const partyQuery = useQuery({
    queryKey: ['community', 'parties'],
    queryFn: getPartyPosts,
    staleTime: 1000 * 60,
  })
  const createMutation = useMutation({
    mutationFn: createPartyPost,
  })
  const joinMutation = useMutation({
    mutationFn: ({ postId, isJoining }: { postId: string; isJoining: boolean }) =>
      isJoining ? joinPartyPost(postId) : cancelPartyJoin(postId),
  })
  const posts = useMemo(() => {
    const serverPosts = partyQuery.data?.data ?? fallbackPartyPosts
    const merged = [...localPosts]
    const localIds = new Set(localPosts.map((post) => post.id))

    serverPosts.forEach((post) => {
      if (!localIds.has(post.id)) {
        merged.push(postOverrides[post.id] ?? post)
      }
    })

    return merged.map((post) => postOverrides[post.id] ?? post)
  }, [localPosts, partyQuery.data?.data, postOverrides])
  const activeJoinedPostId = useMemo(
    () => joinedPostId ?? posts.find((post) => post.isJoined === true)?.id ?? null,
    [joinedPostId, posts],
  )
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
  const activeMessages = useMemo(
    () => messages.filter((message) => message.roomName === activeRoomName),
    [activeRoomName, messages],
  )
  const isShowingFallback = partyQuery.data?.source === 'fallback'

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setQuery(searchDraft)
  }

  const handleComposeFocus = () => {
    titleInputRef.current?.focus()
    setComposeError('')
    setPartyStatusMessage('')
  }

  const handlePartySubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    createMutation.reset()

    const title = titleDraft.trim()
    const description = descriptionDraft.trim()
    const deadline = new Date(deadlineDraft)

    if (!title || !description) {
      setComposeError('모집글 제목과 플레이 스타일을 입력해주세요.')
      return
    }

    if (Number.isNaN(deadline.getTime())) {
      setComposeError('마감 시간을 선택해주세요.')
      return
    }

    const parsedTags = tagsDraft
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean)
      .slice(0, 4)
    const capacity = normalizeCapacity(capacityDraft)
    const request: CreatePartyPostRequest = {
      title,
      mode: modeDraft,
      tier: tierDraft,
      capacity,
      deadline: deadline.toISOString(),
      description,
      tags: parsedTags,
    }
    const localId = `party-${crypto.randomUUID()}`
    const localPost = buildLocalPartyPost(request, localId)

    setLocalPosts((currentPosts) => [localPost, ...currentPosts])
    setSelectedFilter('전체')
    setSearchDraft('')
    setQuery('')
    setTitleDraft('')
    setModeDraft('랭크')
    setTierDraft('마스터+')
    setCapacityDraft('2')
    setDeadlineDraft(getDefaultDeadlineInput())
    setTagsDraft('')
    setDescriptionDraft('')
    setComposeError('')
    setPartyStatusMessage('모집글을 등록했습니다.')
    createMutation.mutate(request, {
      onSuccess: (createdPost) => {
        if (createdPost) {
          setLocalPosts((currentPosts) =>
            currentPosts.map((post) => (post.id === localId ? createdPost : post)),
          )
        }

        void queryClient.invalidateQueries({ queryKey: ['community', 'parties'] })
      },
      onError: () => {
        setPartyStatusMessage('API 연결 실패로 방금 등록한 글은 임시로만 표시됩니다.')
      },
    })
  }

  const handleJoinToggle = (postId: string) => {
    if (joinRequestPostIdRef.current !== null || joinMutation.isPending) {
      return
    }

    const targetPost = posts.find((post) => post.id === postId)

    if (!targetPost) {
      return
    }

    joinMutation.reset()
    const alreadyJoined = activeJoinedPostId === postId || targetPost.isJoined === true
    const previousJoinedPostId = joinedPostId
    const previousOverride = postOverrides[postId]
    const { current, total } = parseCapacity(targetPost.capacity)

    if (!alreadyJoined && (activeJoinedPostId !== null || current >= total)) {
      return
    }

    const nextCurrent = alreadyJoined ? Math.max(0, current - 1) : Math.min(total, current + 1)
    const nextMessage = alreadyJoined
      ? `${targetPost.title} 참여 신청을 취소했어요.`
      : `${targetPost.title} 참여 신청했습니다. (${nextCurrent}/${total})`
    const nextPost = updatePostJoinState(targetPost, !alreadyJoined)

    setLocalPosts((currentPosts) => replacePost(currentPosts, nextPost))
    setPostOverrides((currentOverrides) => ({
      ...currentOverrides,
      [postId]: nextPost,
    }))
    setJoinedPostId(alreadyJoined ? null : postId)
    setPartyStatusMessage(nextMessage)
    joinRequestPostIdRef.current = postId
    joinMutation.mutate(
      { postId, isJoining: !alreadyJoined },
      {
        onSuccess: async () => {
          setActiveRoomName('파티 모집')
          setMessages((currentMessages) => [
            ...currentMessages,
            {
              roomName: '파티 모집',
              name: '나',
              tier: 'Diamond',
              message: nextMessage,
              time: getCurrentTime(),
              isMine: true,
            },
          ])
          setRooms((currentRooms) =>
            currentRooms.map((room) =>
              room.name === '파티 모집' ? { ...room, lastMessage: nextMessage } : room,
            ),
          )

          await queryClient.invalidateQueries({ queryKey: ['community', 'parties'] })

          const refreshedPosts = queryClient.getQueryData<PartyPostsResult>(['community', 'parties'])

          if (refreshedPosts?.source === 'api') {
            setPostOverrides((currentOverrides) => removePostOverride(currentOverrides, postId))
          }
        },
        onError: () => {
          setLocalPosts((currentPosts) => replacePost(currentPosts, targetPost))
          setPostOverrides((currentOverrides) =>
            restorePostOverride(currentOverrides, postId, previousOverride),
          )
          setJoinedPostId(previousJoinedPostId)
          setPartyStatusMessage('API 연결 실패로 참여 상태를 되돌렸습니다.')
        },
        onSettled: (_data, error) => {
          joinRequestPostIdRef.current = null

          if (error) {
            void queryClient.invalidateQueries({ queryKey: ['community', 'parties'] })
          }
        },
      },
    )
  }

  const handleMessageSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const trimmedMessage = chatInput.trim()

    if (trimmedMessage.length === 0) {
      return
    }

    setMessages((currentMessages) => [
      ...currentMessages,
      {
        roomName: activeRoomName,
        name: '나',
        tier: 'Diamond',
        message: trimmedMessage,
        time: getCurrentTime(),
        isMine: true,
      },
    ])
    setRooms((currentRooms) =>
      currentRooms.map((room) =>
        room.name === activeRoomName ? { ...room, lastMessage: trimmedMessage } : room,
      ),
    )
    setChatInput('')
  }

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
              onChange={(event) => setModeDraft(event.target.value as PartyMode)}
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
              aria-label="마감 시간"
              className={styles.deadlineInput}
              min={getDefaultDeadlineInput()}
              onChange={(event) => setDeadlineDraft(event.target.value)}
              type="datetime-local"
              value={deadlineDraft}
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
            <button type="submit" className={styles.primaryButton} disabled={createMutation.isPending}>
              {createMutation.isPending ? '등록중' : '등록'}
            </button>
            {composeError && <p className={styles.composeError} role="alert">{composeError}</p>}
          </form>
          {(isShowingFallback || partyStatusMessage) && (
            <p className={styles.statusMessage} role="status">
              {partyStatusMessage || '파티 API 응답을 불러오지 못해 목업 모집글을 표시합니다.'}
            </p>
          )}

          <div className={styles.partyList}>
            {filteredPartyPosts.length > 0 ? (
              filteredPartyPosts.map((post) => {
                const Icon = partyIconMap[post.icon]
                const { current, total } = parseCapacity(post.capacity)
                const isJoined = activeJoinedPostId === post.id || post.isJoined === true
                const hasJoinedOtherPost = activeJoinedPostId !== null && !isJoined
                const isFull = current >= total
                const isJoinPending = joinMutation.isPending && joinMutation.variables?.postId === post.id

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
                      disabled={joinMutation.isPending || (isFull && !isJoined) || hasJoinedOtherPost}
                      onClick={() => handleJoinToggle(post.id)}
                    >
                      {isJoinPending ? '처리중' : isJoined ? '참여중' : isFull ? '마감' : hasJoinedOtherPost ? '잠김' : '참여'}
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
            <span className={styles.onlineBadge}>온라인 4,113</span>
          </div>

          <div className={styles.chatLayout}>
            <aside className={styles.channelList} aria-label="채팅 채널">
              {rooms.map((room) => (
                <button
                  aria-pressed={activeRoomName === room.name}
                  className={activeRoomName === room.name ? styles.activeChannel : undefined}
                  onClick={() => setActiveRoomName(room.name)}
                  type="button"
                  key={room.name}
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
                <strong># {activeRoomName}</strong>
                <span>
                  {activeMessages.length > 0
                    ? `새 메시지 ${activeMessages.length}개`
                    : '대화를 시작해보세요'}
                </span>
              </div>
              <div className={styles.messageList}>
                {activeMessages.length > 0 ? (
                  activeMessages.map((chat) => (
                    <article
                      className={chat.isMine ? styles.myMessage : undefined}
                      key={`${chat.roomName}-${chat.name}-${chat.time}-${chat.message}`}
                    >
                      <div>
                        <strong>{chat.name}</strong>
                        <span>{chat.tier}</span>
                        <time>{chat.time}</time>
                      </div>
                      <p>{chat.message}</p>
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
                  onChange={(event) => setChatInput(event.target.value)}
                  placeholder="메시지를 입력하세요"
                  value={chatInput}
                />
                <button type="submit" aria-label="메시지 보내기">
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
