import { useMemo, useState } from 'react'
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
import styles from './Party.module.css'

interface PartyPost {
  capacity: string
  close: string
  description: string
  icon: 'crown' | 'leaf' | 'spark' | 'swords'
  mode: string
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
  time: string
  tier: string
}

interface ChatRoom {
  active?: boolean
  lastMessage: string
  name: string
  users: string
}

type PartyFilter = '전체' | '랭크' | '일반' | '커스텀'

const partyFilters: PartyFilter[] = ['전체', '랭크', '일반', '커스텀']

const partyPosts: PartyPost[] = [
  {
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

const chatRooms: ChatRoom[] = [
  { name: '일반', users: '1,234', lastMessage: '새로운 패치 적응 중입니다!', active: true },
  { name: '덱 공략', users: '856', lastMessage: '증강 추천 부탁드려요' },
  { name: '파티 모집', users: '622', lastMessage: '마스터 듀오 구해요~' },
  { name: '질문 & 답변', users: '741', lastMessage: '초보 운영 질문 있습니다' },
]

const chatMessages: ChatMessage[] = [
  { name: '정동글', tier: 'Master', message: '선봉대 벡스 지금도 순방률 괜찮나요?', time: '14:58' },
  { name: '새벽의달', tier: 'Diamond', message: '초반에 벡스 2성만 빨리 붙으면 꽤 안정적이에요.', time: '14:59' },
  { name: '응의자', tier: 'Platinum', message: '아이템은 보건보다 블루 먼저 보는 게 나을까요?', time: '15:00' },
  { name: 'TFTgogo', tier: 'System', message: '17.3 패치 기준 추천 메타가 업데이트되었습니다.', time: '15:01' },
  { name: '나', tier: 'Diamond', message: '파티 모집 쪽에 같이 하실 분 있으면 바로 들어갈게요.', time: '15:02', isMine: true },
]

const partyIconMap = {
  crown: Crown,
  leaf: Leaf,
  spark: Sparkles,
  swords: Swords,
}

function Party() {
  const [selectedFilter, setSelectedFilter] = useState<PartyFilter>('전체')
  const [query, setQuery] = useState('')
  const filteredPartyPosts = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()

    return partyPosts.filter((post) => {
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
  }, [query, selectedFilter])

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
            <button type="button" className={styles.primaryButton}>
              모집글 작성
            </button>
          </div>

          <div className={styles.toolbar}>
            <div className={styles.searchBox}>
              <Search size={18} />
              <input
                aria-label="파티 모집 검색"
                onChange={(event) => setQuery(event.target.value)}
                placeholder="티어, 모드, 키워드 검색"
                value={query}
              />
            </div>
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
            onSubmit={(event) => {
              event.preventDefault()
            }}
          >
            <input aria-label="모집글 제목" placeholder="모집글 제목" />
            <select aria-label="모집 모드" defaultValue="rank">
              <option value="rank">랭크</option>
              <option value="normal">일반</option>
              <option value="custom">커스텀</option>
            </select>
            <select aria-label="티어 조건" defaultValue="master">
              <option value="master">마스터+</option>
              <option value="diamond">다이아+</option>
              <option value="platinum">플래티넘+</option>
              <option value="any">제한 없음</option>
            </select>
            <input aria-label="모집 인원" placeholder="모집 인원" />
            <textarea aria-label="모집 메모" placeholder="플레이 스타일이나 요청사항을 적어주세요." />
            <button type="submit" className={styles.primaryButton}>
              등록
            </button>
          </form>

          <div className={styles.partyList}>
            {filteredPartyPosts.length > 0 ? (
              filteredPartyPosts.map((post) => {
                const Icon = partyIconMap[post.icon]

                return (
                  <article className={styles.partyCard} key={post.title}>
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
                        {post.tags.map((tag) => (
                          <small key={tag}>{tag}</small>
                        ))}
                      </div>
                    </div>
                    <button type="button" className={styles.joinButton}>
                      참여
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
              {chatRooms.map((room) => (
                <button className={room.active ? styles.activeChannel : undefined} type="button" key={room.name}>
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
                <strong># 일반</strong>
                <span>14:58부터 새 메시지 5개</span>
              </div>
              <div className={styles.messageList}>
                {chatMessages.map((chat) => (
                  <article className={chat.isMine ? styles.myMessage : undefined} key={`${chat.name}-${chat.time}`}>
                    <div>
                      <strong>{chat.name}</strong>
                      <span>{chat.tier}</span>
                      <time>{chat.time}</time>
                    </div>
                    <p>{chat.message}</p>
                  </article>
                ))}
              </div>
              <form className={styles.messageForm}>
                <MessageCircle size={19} />
                <input aria-label="채팅 메시지 입력" placeholder="메시지를 입력하세요" />
                <button type="button" aria-label="메시지 보내기">
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
