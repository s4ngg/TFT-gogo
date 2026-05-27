import { shell } from '../../components/Header/Header.js'
import { esc } from '../../components/common/renderers.js'
import { state } from '../../store/staticState.js'

const partyFilters = ['전체', '랭크', '일반', '커스텀']
const toneIcons = {
  crown: '♛',
  leaf: '◆',
  spark: '✦',
  swords: '⚔',
}
const initialPartyPosts = [
  {
    id: 'party-master-duo',
    title: '마스터 이상 듀오 구합니다',
    mode: '랭크',
    tier: '마스터+',
    capacity: '2/2',
    close: '마감 15분 전',
    status: '모집중',
    description: '17.3 추천 메타 기준으로 빠르게 점수 올리실 분 찾아요.',
    tags: ['소통 가능', '연승 목표', '빠른 매칭'],
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
    tags: ['피드백 환영', '듀오 접속', '마이크 선택'],
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
    tags: ['초보 환영', '일반전', '테스트'],
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
    description: '초반 위주로 안정적인 운영 맞춰가실 분이면 좋습니다.',
    tags: ['주말 집중', '초반 운영', '멘탈 좋음'],
    icon: 'swords',
    tone: 'gold',
  },
]
const initialChatRooms = [
  { name: '일반', users: '1,234', lastMessage: '새로운 패치 적응 중입니다!' },
  { name: '덱 공략', users: '856', lastMessage: '증강 추천 부탁드려요' },
  { name: '파티 모집', users: '622', lastMessage: '마스터 듀오 구해요~' },
  { name: '질문 & 답변', users: '741', lastMessage: '초보 운영 질문 있습니다' },
]
const initialMessages = [
  { roomName: '일반', name: '정동글', tier: 'Master', message: '선봉대 벡스 지금도 초반을 괜찮게 받아요?', time: '14:58' },
  { roomName: '일반', name: '새벽의달', tier: 'Diamond', message: '초반에 벡스 2성만 빨리 붙으면 꽤 안정적이에요.', time: '14:59' },
  { roomName: '일반', name: '응의자', tier: 'Platinum', message: '아이템은 보건보다 블루 먼저 보는 게 나을까요?', time: '15:00' },
  { roomName: '일반', name: 'TFTgogo', tier: 'System', message: '17.3 패치 기준 추천 메타가 업데이트되었습니다.', time: '15:01' },
  { roomName: '덱 공략', name: '운영연습', tier: 'Diamond', message: '전투 증강 첫 선택이면 어떤 조합이 좋아요?', time: '14:54' },
  { roomName: '덱 공략', name: '메타분석가', tier: 'Master', message: '초반에는 선봉대와 푸른색 기반으로 피 관리 추천해요.', time: '14:55' },
  { roomName: '파티 모집', name: '플레러너', tier: 'Platinum', message: '플래티넘 듀오 자리 있나요?', time: '14:50' },
  { roomName: '파티 모집', name: '초반중독', tier: 'Master', message: '마스터 이상 듀오 랭크 같이 하실 분?', time: '14:56' },
  { roomName: '질문 & 답변', name: '입문자', tier: 'Gold', message: '아이템 우선순위는 캐리부터 맞추면 되나요?', time: '14:48' },
]

let partyPosts = [...initialPartyPosts]
let chatRooms = [...initialChatRooms]
let chatMessages = [...initialMessages]

function getCurrentTime() {
  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date())
}

function normalizeCapacity(value) {
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
  return Number.isFinite(total) && total > 0 ? `1/${total}` : '1/2'
}

function parseCapacity(capacity) {
  const [currentRaw, totalRaw] = capacity.split('/').map(Number)
  const total = Number.isFinite(totalRaw) && totalRaw > 0 ? totalRaw : 2
  const current = Number.isFinite(currentRaw) ? Math.min(Math.max(currentRaw, 0), total) : 0
  return { current, total }
}

function postStyle(mode, tier) {
  if (mode === '일반') return { icon: 'spark', tone: 'cyan' }
  if (mode === '커스텀') return { icon: 'swords', tone: 'gold' }
  if (tier.includes('다이아')) return { icon: 'leaf', tone: 'green' }
  return { icon: 'crown', tone: 'purple' }
}

function filteredPosts() {
  const normalizedQuery = state.partyQuery.trim().toLowerCase()
  return partyPosts.filter((post) => {
    const matchesFilter = state.partyFilter === '전체' || post.mode === state.partyFilter
    const searchableText = [post.title, post.mode, post.tier, post.description, ...post.tags].join(' ').toLowerCase()
    return matchesFilter && (!normalizedQuery || searchableText.includes(normalizedQuery))
  })
}

function activeMessages() {
  return chatMessages.filter((message) => message.roomName === state.partyActiveRoom)
}

function renderFilterTabs() {
  return partyFilters.map((filter) => `
    <button type="button" class="${state.partyFilter === filter ? 'selectedTab' : ''}" aria-pressed="${state.partyFilter === filter}" data-party-filter="${filter}">${filter}</button>
  `).join('')
}

function renderComposeBox() {
  return `
    <form class="composeBox" id="partyComposeForm">
      <input id="partyTitle" aria-label="모집글 제목" placeholder="모집글 제목" />
      <select id="partyMode" aria-label="모집 모드">
        <option value="랭크">랭크</option>
        <option value="일반">일반</option>
        <option value="커스텀">커스텀</option>
      </select>
      <select id="partyTier" aria-label="티어 조건">
        <option value="마스터+">마스터+</option>
        <option value="다이아+">다이아+</option>
        <option value="플래티넘+">플래티넘+</option>
        <option value="제한 없음">제한 없음</option>
      </select>
      <input id="partyCapacity" aria-label="모집 인원" placeholder="모집 인원" value="2" />
      <input id="partyTags" aria-label="모집 태그" class="tagInput" placeholder="태그 입력 예: 소통 가능, 연습 목표, 빠른 매칭" />
      <textarea id="partyDescription" aria-label="모집 메모" placeholder="플레이 스타일이나 요청사항을 적어주세요."></textarea>
      <button type="submit" class="primaryButton">등록</button>
      <p class="composeError" id="partyComposeError" hidden></p>
    </form>
  `
}

function renderPartyList() {
  const posts = filteredPosts()
  if (!posts.length) return '<p class="emptyState">조건에 맞는 모집글이 없습니다.</p>'

  return posts.map((post) => {
    const { current, total } = parseCapacity(post.capacity)
    const isJoined = state.partyJoinedId === post.id
    const hasJoinedOtherPost = state.partyJoinedId !== null && !isJoined
    const isFull = current >= total

    return `
      <article class="partyCard">
        <div class="partyIcon ${post.tone}"><span>${toneIcons[post.icon]}</span></div>
        <div class="partyContent">
          <div class="partyTitleLine">
            <h3>${esc(post.title)}</h3>
            <span>${isFull ? '마감' : esc(post.status)}</span>
          </div>
          <p>${esc(post.description)}</p>
          <div class="partyMeta">
            <span>${esc(post.mode)}</span>
            <span>${esc(post.tier)}</span>
            <span>${esc(post.capacity)}</span>
            <span>${esc(post.close)}</span>
          </div>
          <div class="partyTags">${post.tags.map((tag) => `<small>${esc(tag)}</small>`).join('')}</div>
        </div>
        <button type="button" class="joinButton" aria-pressed="${isJoined}" ${((isFull && !isJoined) || hasJoinedOtherPost) ? 'disabled' : ''} data-party-join="${esc(post.id)}">
          ${isJoined ? '참여중' : isFull ? '마감' : hasJoinedOtherPost ? '대기' : '참여'}
        </button>
      </article>
    `
  }).join('')
}

function renderChannels() {
  return chatRooms.map((room) => `
    <button type="button" class="${state.partyActiveRoom === room.name ? 'activeChannel' : ''}" aria-pressed="${state.partyActiveRoom === room.name}" data-party-room="${esc(room.name)}">
      <strong># ${esc(room.name)}</strong>
      <span>${esc(room.users)}</span>
      <small>${esc(room.lastMessage)}</small>
    </button>
  `).join('')
}

function renderMessages() {
  const messages = activeMessages()
  if (!messages.length) return '<p class="chatEmpty">아직 이 채널에는 메시지가 없습니다.</p>'

  return messages.map((chat) => `
    <article class="${chat.isMine ? 'myMessage' : ''}">
      <div>
        <strong>${esc(chat.name)}</strong>
        <span>${esc(chat.tier)}</span>
        <time>${esc(chat.time)}</time>
      </div>
      <p>${esc(chat.message)}</p>
    </article>
  `).join('')
}

function renderPartyShell() {
  return `
    <div class="communityPage">
      <header class="pageHeader">
        <div>
          <p>Community</p>
          <h1>커뮤니티</h1>
        </div>
        <span>파티 모집과 실시간 채팅을 한 화면에서 확인하세요</span>
      </header>

      <section class="panel partyPanel">
        <div class="panelHeader">
          <div>
            <h2>파티원 찾기</h2>
            <p>티어와 플레이 스타일에 맞는 TFT 듀오를 찾아보세요.</p>
          </div>
          <button type="button" class="primaryButton" id="focusCompose">모집글 작성</button>
        </div>

        <div class="toolbar">
          <form class="searchBox" id="partySearchForm">
            <span>⌕</span>
            <input id="partySearchInput" aria-label="파티 모집 검색" placeholder="티어, 모드, 키워드 검색" value="${esc(state.partyQuery)}" />
            <button type="submit">검색</button>
          </form>
          <div class="filterTabs" aria-label="파티원 찾기 필터">${renderFilterTabs()}</div>
        </div>

        ${renderComposeBox()}
        <div class="partyList">${renderPartyList()}</div>
      </section>

      <section class="panel chatPanel">
        <div class="panelHeader">
          <div>
            <h2>실시간 채팅</h2>
            <p>현재 접속 중인 유저들과 빠르게 정보를 나눠보세요.</p>
          </div>
          <span class="onlineBadge">온라인 4,113</span>
        </div>

        <div class="chatLayout">
          <aside class="channelList" aria-label="채팅 채널">${renderChannels()}</aside>
          <div class="chatWindow">
            <div class="chatWindowHeader">
              <strong># ${esc(state.partyActiveRoom)}</strong>
              <span>${activeMessages().length > 0 ? `총 메시지 ${activeMessages().length}개` : '대화를 시작해보세요'}</span>
            </div>
            <div class="messageList">${renderMessages()}</div>
            <form class="messageForm" id="partyMessageForm">
              <span>💬</span>
              <input id="partyMessageInput" aria-label="채팅 메시지 입력" placeholder="메시지를 입력하세요" />
              <button type="submit" aria-label="메시지 보내기">➤</button>
            </form>
          </div>
        </div>
      </section>
    </div>
  `
}

function bindPartyEvents() {
  document.getElementById('partySearchForm')?.addEventListener('submit', (event) => {
    event.preventDefault()
    state.partyQuery = document.getElementById('partySearchInput')?.value ?? ''
    renderParty()
  })

  document.getElementById('partySearchInput')?.addEventListener('input', (event) => {
    state.partyQuery = event.target.value
    renderParty()
  })

  document.querySelectorAll('[data-party-filter]').forEach((button) => button.addEventListener('click', () => {
    state.partyFilter = button.dataset.partyFilter
    renderParty()
  }))

  document.getElementById('focusCompose')?.addEventListener('click', () => {
    document.getElementById('partyTitle')?.focus()
  })

  document.getElementById('partyComposeForm')?.addEventListener('submit', (event) => {
    event.preventDefault()
    const title = document.getElementById('partyTitle')?.value.trim() ?? ''
    const description = document.getElementById('partyDescription')?.value.trim() ?? ''
    const error = document.getElementById('partyComposeError')

    if (!title || !description) {
      if (error) {
        error.hidden = false
        error.textContent = '모집글 제목과 플레이 스타일을 입력해주세요.'
      }
      return
    }

    const mode = document.getElementById('partyMode')?.value ?? '랭크'
    const tier = document.getElementById('partyTier')?.value ?? '마스터+'
    const tags = (document.getElementById('partyTags')?.value ?? '')
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean)
      .slice(0, 4)

    partyPosts = [{
      id: `party-${Date.now()}`,
      title,
      mode,
      tier,
      capacity: normalizeCapacity(document.getElementById('partyCapacity')?.value ?? '2'),
      close: '방금 등록',
      status: '모집중',
      description,
      tags: tags.length ? tags : [mode, tier, '방금 등록'],
      ...postStyle(mode, tier),
    }, ...partyPosts]
    state.partyFilter = '전체'
    state.partyQuery = ''
    renderParty()
  })

  document.querySelectorAll('[data-party-join]').forEach((button) => button.addEventListener('click', () => {
    const postId = button.dataset.partyJoin
    const targetPost = partyPosts.find((post) => post.id === postId)
    if (!targetPost) return

    const alreadyJoined = state.partyJoinedId === postId
    const { current, total } = parseCapacity(targetPost.capacity)
    if (!alreadyJoined && (state.partyJoinedId !== null || current >= total)) return

    const nextCurrent = alreadyJoined ? Math.max(0, current - 1) : Math.min(total, current + 1)
    const nextMessage = alreadyJoined
      ? `${targetPost.title} 참여 요청을 취소했어요.`
      : `${targetPost.title} 참여 요청했습니다. (${nextCurrent}/${total})`

    partyPosts = partyPosts.map((post) => post.id === postId
      ? { ...post, capacity: `${nextCurrent}/${total}`, status: nextCurrent >= total ? '대기중' : '모집중' }
      : post)
    state.partyJoinedId = alreadyJoined ? null : postId
    state.partyActiveRoom = '파티 모집'
    chatMessages = [...chatMessages, {
      roomName: '파티 모집',
      name: '나',
      tier: 'Diamond',
      message: nextMessage,
      time: getCurrentTime(),
      isMine: true,
    }]
    chatRooms = chatRooms.map((room) => room.name === '파티 모집' ? { ...room, lastMessage: nextMessage } : room)
    renderParty()
  }))

  document.querySelectorAll('[data-party-room]').forEach((button) => button.addEventListener('click', () => {
    state.partyActiveRoom = button.dataset.partyRoom
    renderParty()
  }))

  document.getElementById('partyMessageForm')?.addEventListener('submit', (event) => {
    event.preventDefault()
    const input = document.getElementById('partyMessageInput')
    const message = input?.value.trim() ?? ''
    if (!message) return

    chatMessages = [...chatMessages, {
      roomName: state.partyActiveRoom,
      name: '나',
      tier: 'Diamond',
      message,
      time: getCurrentTime(),
      isMine: true,
    }]
    chatRooms = chatRooms.map((room) => room.name === state.partyActiveRoom ? { ...room, lastMessage: message } : room)
    renderParty()
  })
}

export function renderParty() {
  shell('party', renderPartyShell())
  bindPartyEvents()
}
