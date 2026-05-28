import {
  Bookmark,
  Clock3,
  Crown,
  Heart,
  Leaf,
  MessageCircle,
  MessageSquare,
  Pencil,
  Search,
  Send,
  Sparkles,
  Swords,
  Trash2,
  Users,
  X,
} from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { AppLayout } from '../../components/layout'
import styles from './Community.module.css'

// ─── 타입 ────────────────────────────────────────────────────────────────────

interface PartyPost {
  capacity: string
  close: string
  description: string
  icon: 'crown' | 'leaf' | 'spark' | 'swords'
  id: number
  mode: '랭크' | '일반' | '커스텀'
  status: '모집중' | '대기중'
  tags: string[]
  tier: string
  title: string
  tone: 'purple' | 'green' | 'cyan' | 'gold'
}

interface ChatMessage {
  isMine?: boolean
  message: string
  name: string
  tier: string
  time: string
}

interface ChatRoom {
  lastMessage: string
  name: string
  users: string
}

interface DeckComment {
  author: string
  authorTier: string
  content: string
  id: number
  time: string
}

interface DeckPost {
  author: string
  authorTier: string
  comments: DeckComment[]
  createdAt: string
  deckName: string
  description: string
  id: number
  likes: number
  tags: string[]
  title: string
}

// ─── 채널 목록 (스펙 정의, 변경 없음) ──────────────────────────────────────

const chatRooms: ChatRoom[] = [
  { name: '일반', users: '-', lastMessage: '' },
  { name: '덱 공략', users: '-', lastMessage: '' },
  { name: '파티 모집', users: '-', lastMessage: '' },
  { name: '질문 & 답변', users: '-', lastMessage: '' },
]

const emptyChannelMessages: Record<string, ChatMessage[]> = {
  '일반': [], '덱 공략': [], '파티 모집': [], '질문 & 답변': [],
}

const partyIconMap = { crown: Crown, leaf: Leaf, spark: Sparkles, swords: Swords }
const iconCycle: PartyPost['icon'][] = ['crown', 'leaf', 'spark', 'swords']
const toneCycle: PartyPost['tone'][] = ['purple', 'green', 'cyan', 'gold']

const PARTY_FILTER_TABS = ['전체', '랭크', '일반', '커스텀'] as const
type PartyFilter = (typeof PARTY_FILTER_TABS)[number]

// ─── 컴포넌트 ────────────────────────────────────────────────────────────────

function Community() {
  // 메인 탭
  const [mainTab, setMainTab] = useState<'party' | 'decks'>('party')

  // ── 파티원 찾기 상태 ──────────────────────────────────────────────────────
  const [partyPosts, setPartyPosts] = useState<PartyPost[]>([])
  const [partyFilter, setPartyFilter] = useState<PartyFilter>('전체')
  const [partySearch, setPartySearch] = useState('')
  const [joinedIds, setJoinedIds] = useState<Set<number>>(new Set())
  const [showPartyForm, setShowPartyForm] = useState(false)
  // 폼 필드
  const [formTitle, setFormTitle] = useState('')
  const [formMode, setFormMode] = useState<'랭크' | '일반' | '커스텀'>('랭크')
  const [formTier, setFormTier] = useState('마스터+')
  const [formCapacity, setFormCapacity] = useState('')
  const [formMemo, setFormMemo] = useState('')

  // ── 실시간 채팅 상태 ──────────────────────────────────────────────────────
  const [activeChannel, setActiveChannel] = useState('일반')
  const [channelMessages, setChannelMessages] = useState<Record<string, ChatMessage[]>>(emptyChannelMessages)
  const [chatInput, setChatInput] = useState('')
  const messageListRef = useRef<HTMLDivElement>(null)

  // ── 덱 공유 상태 ──────────────────────────────────────────────────────────
  const [deckPosts, setDeckPosts] = useState<DeckPost[]>([])
  const [likedIds, setLikedIds] = useState<Set<number>>(new Set())
  const [bookmarkedIds, setBookmarkedIds] = useState<Set<number>>(new Set())
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [sortBy, setSortBy] = useState<'latest' | 'popular'>('latest')
  const [showBookmarksOnly, setShowBookmarksOnly] = useState(false)
  const [editingPost, setEditingPost] = useState<DeckPost | null>(null)
  const [composeOpen, setComposeOpen] = useState(false)
  const [composeTitle, setComposeTitle] = useState('')
  const [composeDeckName, setComposeDeckName] = useState('')
  const [composeDesc, setComposeDesc] = useState('')
  const [composeTags, setComposeTags] = useState('')
  const [commentTexts, setCommentTexts] = useState<Record<number, string>>({})

  // ── 채팅 자동 스크롤 ─────────────────────────────────────────────────────
  useEffect(() => {
    if (messageListRef.current) {
      messageListRef.current.scrollTop = messageListRef.current.scrollHeight
    }
  }, [channelMessages, activeChannel])

  // ─── 파티 핸들러 ─────────────────────────────────────────────────────────

  const filteredPartyPosts = partyPosts
    .filter((p) => partyFilter === '전체' || p.mode === partyFilter)
    .filter((p) => {
      const q = partySearch.trim().toLowerCase()
      if (!q) return true
      return (
        p.title.toLowerCase().includes(q) ||
        p.tier.toLowerCase().includes(q) ||
        p.mode.includes(q) ||
        p.tags.some((t) => t.includes(q))
      )
    })

  function submitPartyPost(e: React.FormEvent) {
    e.preventDefault()
    if (!formTitle.trim()) return
    const idx = partyPosts.length % 4
    const newPost: PartyPost = {
      id: Date.now(),
      title: formTitle,
      mode: formMode,
      tier: formTier,
      capacity: `1/${formCapacity || '4'}`,
      close: '마감 2시간 전',
      status: '모집중',
      description: formMemo || '함께 즐길 분 구합니다.',
      tags: [],
      icon: iconCycle[idx],
      tone: toneCycle[idx],
    }
    setPartyPosts((prev) => [newPost, ...prev])
    setFormTitle('')
    setFormMode('랭크')
    setFormTier('마스터+')
    setFormCapacity('')
    setFormMemo('')
    setShowPartyForm(false)
  }

  function toggleJoin(id: number) {
    setJoinedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  // ─── 채팅 핸들러 ─────────────────────────────────────────────────────────

  function sendChatMessage(e: React.FormEvent) {
    e.preventDefault()
    const text = chatInput.trim()
    if (!text) return
    const now = new Date()
    const time = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
    const newMsg: ChatMessage = { name: '나', tier: 'Diamond', message: text, time, isMine: true }
    setChannelMessages((prev) => ({
      ...prev,
      [activeChannel]: [...(prev[activeChannel] ?? []), newMsg],
    }))
    setChatInput('')
  }

  // ─── 덱 공유 핸들러 ──────────────────────────────────────────────────────

  function toggleLike(id: number) {
    setLikedIds((prev) => { const n = new Set(prev); n.has(id) ? n.delete(id) : n.add(id); return n })
  }

  function toggleBookmark(id: number) {
    setBookmarkedIds((prev) => { const n = new Set(prev); n.has(id) ? n.delete(id) : n.add(id); return n })
  }

  function deletePost(id: number) {
    setDeckPosts((prev) => prev.filter((p) => p.id !== id))
    if (expandedId === id) setExpandedId(null)
  }

  function openCompose() {
    setEditingPost(null); setComposeTitle(''); setComposeDeckName(''); setComposeDesc(''); setComposeTags(''); setComposeOpen(true)
  }

  function openEdit(post: DeckPost) {
    setEditingPost(post); setComposeTitle(post.title); setComposeDeckName(post.deckName)
    setComposeDesc(post.description); setComposeTags(post.tags.join(', ')); setComposeOpen(true)
  }

  function submitCompose(e: React.FormEvent) {
    e.preventDefault()
    const tags = composeTags.split(',').map((t) => t.trim()).filter(Boolean)
    if (!composeTitle.trim() || !composeDeckName.trim()) return
    if (editingPost) {
      setDeckPosts((prev) => prev.map((p) =>
        p.id === editingPost.id ? { ...p, title: composeTitle, deckName: composeDeckName, description: composeDesc, tags } : p
      ))
    } else {
      setDeckPosts((prev) => [{
        id: Date.now(), title: composeTitle, deckName: composeDeckName, description: composeDesc,
        tags, author: '나', authorTier: 'Diamond', likes: 0, createdAt: '방금 전', comments: [],
      }, ...prev])
    }
    setComposeOpen(false); setEditingPost(null)
  }

  function submitComment(postId: number) {
    const text = commentTexts[postId]?.trim()
    if (!text) return
    const newComment: DeckComment = { id: Date.now(), author: '나', authorTier: 'Diamond', content: text, time: '방금 전' }
    setDeckPosts((prev) => prev.map((p) => p.id === postId ? { ...p, comments: [...p.comments, newComment] } : p))
    setCommentTexts((prev) => ({ ...prev, [postId]: '' }))
  }

  const displayedDeckPosts = deckPosts
    .filter((p) => !showBookmarksOnly || bookmarkedIds.has(p.id))
    .sort((a, b) => sortBy === 'popular'
      ? (b.likes + (likedIds.has(b.id) ? 1 : 0)) - (a.likes + (likedIds.has(a.id) ? 1 : 0))
      : b.id - a.id
    )

  const currentMessages = channelMessages[activeChannel] ?? []

  // ─── 렌더 ────────────────────────────────────────────────────────────────

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

        {/* 메인 탭 */}
        <nav className={styles.mainTabBar}>
          <button type="button" className={mainTab === 'party' ? styles.activeMainTab : undefined} onClick={() => setMainTab('party')}>
            파티 &amp; 채팅
          </button>
          <button type="button" className={mainTab === 'decks' ? styles.activeMainTab : undefined} onClick={() => setMainTab('decks')}>
            덱 공유
          </button>
        </nav>

        {/* ── 파티 & 채팅 탭 ── */}
        {mainTab === 'party' && (
          <>
            {/* 파티원 찾기 */}
            <section className={`${styles.panel} ${styles.partyPanel}`}>
              <div className={styles.panelHeader}>
                <div>
                  <h2>파티원 찾기</h2>
                  <p>티어와 플레이 스타일에 맞는 TFT 듀오를 찾아보세요.</p>
                </div>
                <button type="button" className={styles.primaryButton} onClick={() => setShowPartyForm((v) => !v)}>
                  {showPartyForm ? '닫기' : '모집글 작성'}
                </button>
              </div>

              {/* 검색 + 필터 */}
              <div className={styles.toolbar}>
                <div className={styles.searchBox}>
                  <Search size={18} />
                  <input
                    aria-label="파티 모집 검색"
                    placeholder="티어, 모드, 키워드 검색"
                    value={partySearch}
                    onChange={(e) => setPartySearch(e.target.value)}
                  />
                  {partySearch && (
                    <button type="button" className={styles.clearSearch} onClick={() => setPartySearch('')} aria-label="검색어 지우기">
                      <X size={14} />
                    </button>
                  )}
                </div>
                <div className={styles.filterTabs} aria-label="파티원 찾기 필터">
                  {PARTY_FILTER_TABS.map((f) => (
                    <button
                      key={f}
                      type="button"
                      className={partyFilter === f ? styles.selectedTab : undefined}
                      onClick={() => setPartyFilter(f)}
                    >
                      {f}
                    </button>
                  ))}
                </div>
              </div>

              {/* 모집글 작성 폼 */}
              {showPartyForm && (
                <form className={styles.composeBox} onSubmit={submitPartyPost}>
                  <input
                    aria-label="모집글 제목"
                    placeholder="모집글 제목"
                    value={formTitle}
                    onChange={(e) => setFormTitle(e.target.value)}
                    required
                  />
                  <select
                    aria-label="모집 모드"
                    value={formMode}
                    onChange={(e) => setFormMode(e.target.value as typeof formMode)}
                  >
                    <option value="랭크">랭크</option>
                    <option value="일반">일반</option>
                    <option value="커스텀">커스텀</option>
                  </select>
                  <select
                    aria-label="티어 조건"
                    value={formTier}
                    onChange={(e) => setFormTier(e.target.value)}
                  >
                    <option value="마스터+">마스터+</option>
                    <option value="다이아+">다이아+</option>
                    <option value="플래티넘+">플래티넘+</option>
                    <option value="제한 없음">제한 없음</option>
                  </select>
                  <input
                    aria-label="모집 인원"
                    placeholder="최대 인원"
                    value={formCapacity}
                    onChange={(e) => setFormCapacity(e.target.value)}
                  />
                  <textarea
                    aria-label="모집 메모"
                    placeholder="플레이 스타일이나 요청사항을 적어주세요."
                    value={formMemo}
                    onChange={(e) => setFormMemo(e.target.value)}
                  />
                  <button type="submit" className={styles.primaryButton}>등록</button>
                </form>
              )}

              {/* 모집글 목록 */}
              <div className={styles.partyList}>
                {filteredPartyPosts.length === 0 ? (
                  <p className={styles.emptyState}>조건에 맞는 모집글이 없습니다.</p>
                ) : (
                  filteredPartyPosts.map((post) => {
                    const Icon = partyIconMap[post.icon]
                    const joined = joinedIds.has(post.id)
                    return (
                      <article className={styles.partyCard} key={post.id}>
                        <div className={`${styles.partyIcon} ${styles[post.tone]}`}>
                          <Icon size={28} strokeWidth={2.2} />
                        </div>
                        <div className={styles.partyContent}>
                          <div className={styles.partyTitleLine}>
                            <h3>{post.title}</h3>
                            <span>{post.status}</span>
                          </div>
                          <p>{post.description}</p>
                          <div className={styles.partyMeta}>
                            <span>{post.mode}</span>
                            <span>{post.tier}</span>
                            <span><Users size={15} />{post.capacity}</span>
                            <span><Clock3 size={15} />{post.close}</span>
                          </div>
                          <div className={styles.partyTags}>
                            {post.tags.map((tag) => <small key={tag}>{tag}</small>)}
                          </div>
                        </div>
                        <button
                          type="button"
                          className={`${styles.joinButton} ${joined ? styles.joinedButton : ''}`}
                          onClick={() => toggleJoin(post.id)}
                        >
                          {joined ? '참여중' : '참여'}
                        </button>
                      </article>
                    )
                  })
                )}
              </div>
            </section>

            {/* 실시간 채팅 */}
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
                  {chatRooms.map((room) => (
                    <button
                      key={room.name}
                      type="button"
                      className={activeChannel === room.name ? styles.activeChannel : undefined}
                      onClick={() => setActiveChannel(room.name)}
                    >
                      <strong># {room.name}</strong>
                      {room.users !== '-' && <span><Users size={14} />{room.users}</span>}
                      <small>{room.lastMessage}</small>
                    </button>
                  ))}
                </aside>

                <div className={styles.chatWindow}>
                  <div className={styles.chatWindowHeader}>
                    <strong># {activeChannel}</strong>
                    <span>메시지 {currentMessages.length}개</span>
                  </div>
                  <div className={styles.messageList} ref={messageListRef}>
                    {currentMessages.map((chat, i) => (
                      <article
                        className={chat.isMine ? styles.myMessage : undefined}
                        key={`${chat.name}-${chat.time}-${i}`}
                      >
                        <div>
                          <strong>{chat.name}</strong>
                          <span>{chat.tier}</span>
                          <time>{chat.time}</time>
                        </div>
                        <p>{chat.message}</p>
                      </article>
                    ))}
                  </div>
                  <form className={styles.messageForm} onSubmit={sendChatMessage}>
                    <MessageCircle size={19} />
                    <input
                      aria-label="채팅 메시지 입력"
                      placeholder={`#${activeChannel} 채널에 메시지 보내기`}
                      value={chatInput}
                      onChange={(e) => setChatInput(e.target.value)}
                    />
                    <button type="submit" aria-label="메시지 보내기">
                      <Send size={18} />
                    </button>
                  </form>
                </div>
              </div>
            </section>
          </>
        )}

        {/* ── 덱 공유 탭 ── */}
        {mainTab === 'decks' && (
          <section className={`${styles.panel} ${styles.deckPanel}`}>
            <div className={styles.deckPanelHeader}>
              <div>
                <h2>덱 공유</h2>
                <p>유저들이 직접 공유하는 TFT 덱 공략과 운영 팁을 확인하세요.</p>
              </div>
              <button type="button" className={styles.primaryButton} onClick={openCompose}>글 작성</button>
            </div>

            {/* 작성 / 수정 폼 */}
            {composeOpen && (
              <form className={styles.deckComposeForm} onSubmit={submitCompose}>
                <div className={styles.deckComposeHeader}>
                  <span>{editingPost ? '게시글 수정' : '새 게시글 작성'}</span>
                  <button type="button" aria-label="닫기" onClick={() => setComposeOpen(false)}><X size={16} /></button>
                </div>
                <div className={styles.deckComposeRow}>
                  <input className={styles.deckComposeTitle} aria-label="제목" placeholder="제목을 입력하세요" value={composeTitle} onChange={(e) => setComposeTitle(e.target.value)} required />
                  <input aria-label="덱 이름" placeholder="덱 이름" value={composeDeckName} onChange={(e) => setComposeDeckName(e.target.value)} required />
                  <input aria-label="태그 (쉼표 구분)" placeholder="태그 (쉼표로 구분)" value={composeTags} onChange={(e) => setComposeTags(e.target.value)} />
                </div>
                <textarea className={styles.deckComposeDesc} aria-label="설명" placeholder="덱 운영 방법, 아이템 우선순위 등을 자유롭게 적어주세요." value={composeDesc} onChange={(e) => setComposeDesc(e.target.value)} />
                <div className={styles.deckComposeActions}>
                  <button type="button" className={styles.cancelButton} onClick={() => setComposeOpen(false)}>취소</button>
                  <button type="submit" className={styles.primaryButton}>{editingPost ? '수정 완료' : '등록'}</button>
                </div>
              </form>
            )}

            {/* 정렬 / 필터 */}
            <div className={styles.deckToolbar}>
              <div className={styles.sortTabs}>
                <button type="button" className={sortBy === 'latest' ? styles.selectedTab : undefined} onClick={() => setSortBy('latest')}>최신순</button>
                <button type="button" className={sortBy === 'popular' ? styles.selectedTab : undefined} onClick={() => setSortBy('popular')}>인기순</button>
              </div>
              <button
                type="button"
                className={`${styles.bookmarkFilter} ${showBookmarksOnly ? styles.bookmarkFilterActive : ''}`}
                onClick={() => setShowBookmarksOnly((v) => !v)}
              >
                <Bookmark size={14} />내 북마크
              </button>
            </div>

            {/* 게시글 그리드 */}
            {displayedDeckPosts.length === 0 ? (
              <p className={styles.emptyState}>북마크한 게시글이 없습니다.</p>
            ) : (
              <div className={styles.deckGrid}>
                {displayedDeckPosts.map((post) => {
                  const liked = likedIds.has(post.id)
                  const bookmarked = bookmarkedIds.has(post.id)
                  const isExpanded = expandedId === post.id

                  return (
                    <article className={styles.deckCard} key={post.id}>
                      <div className={styles.deckCardHeader}>
                        <div className={styles.deckCardAuthor}>
                          <strong>{post.author}</strong>
                          <span className={styles.tierBadge}>{post.authorTier}</span>
                          <time>{post.createdAt}</time>
                        </div>
                        <button type="button" className={`${styles.iconButton} ${bookmarked ? styles.bookmarked : ''}`} aria-label="북마크" onClick={() => toggleBookmark(post.id)}>
                          <Bookmark size={15} fill={bookmarked ? 'currentColor' : 'none'} />
                        </button>
                      </div>

                      <h3 className={styles.deckCardTitle}>{post.title}</h3>
                      <span className={styles.deckName}>{post.deckName}</span>
                      <p className={styles.deckCardDesc}>{post.description}</p>

                      <div className={styles.deckCardTags}>
                        {post.tags.map((tag) => <small key={tag}>{tag}</small>)}
                      </div>

                      <div className={styles.deckCardFooter}>
                        <div className={styles.deckCardActions}>
                          <button type="button" className={`${styles.likeButton} ${liked ? styles.liked : ''}`} onClick={() => toggleLike(post.id)}>
                            <Heart size={14} fill={liked ? 'currentColor' : 'none'} />
                            {post.likes + (liked ? 1 : 0)}
                          </button>
                          <button type="button" className={styles.commentToggle} onClick={() => setExpandedId(isExpanded ? null : post.id)}>
                            <MessageSquare size={14} />
                            {post.comments.length}
                          </button>
                        </div>
                        <div className={styles.deckCardEdit}>
                          <button type="button" className={styles.iconButton} onClick={() => openEdit(post)} aria-label="수정"><Pencil size={14} /></button>
                          <button type="button" className={`${styles.iconButton} ${styles.deleteButton}`} onClick={() => deletePost(post.id)} aria-label="삭제"><Trash2 size={14} /></button>
                        </div>
                      </div>

                      {/* 댓글 */}
                      {isExpanded && (
                        <div className={styles.commentSection}>
                          {post.comments.length === 0 ? (
                            <p className={styles.noComments}>아직 댓글이 없어요. 첫 댓글을 남겨보세요!</p>
                          ) : (
                            <ul className={styles.commentList}>
                              {post.comments.map((c) => (
                                <li key={c.id} className={styles.commentItem}>
                                  <div className={styles.commentMeta}>
                                    <strong>{c.author}</strong>
                                    <span className={styles.tierBadge}>{c.authorTier}</span>
                                    <time>{c.time}</time>
                                  </div>
                                  <p>{c.content}</p>
                                </li>
                              ))}
                            </ul>
                          )}
                          <form className={styles.commentInputRow} onSubmit={(e) => { e.preventDefault(); submitComment(post.id) }}>
                            <input
                              aria-label="댓글 입력"
                              placeholder="댓글을 입력하세요"
                              value={commentTexts[post.id] ?? ''}
                              onChange={(e) => setCommentTexts((prev) => ({ ...prev, [post.id]: e.target.value }))}
                            />
                            <button type="submit" aria-label="댓글 전송"><Send size={15} /></button>
                          </form>
                        </div>
                      )}
                    </article>
                  )
                })}
              </div>
            )}
          </section>
        )}
      </div>
    </AppLayout>
  )
}

export default Community
