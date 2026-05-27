import { shell } from '../../components/Header/Header.js'
import { esc, pct, renderChampion, renderTier, renderTrait } from '../../components/common/renderers.js'
import { decks } from '../../data/staticData.js'
import { state } from '../../store/staticState.js'

const quickTags = ['정동글#KR1', '새벽의달#KR', '응의자#KR1', 'TFT잘하고싶다#1234']
const metaFilters = [
  { label: '종합', value: 'overall' },
  { label: '상위권', value: 'upper' },
  { label: '마스터+', value: 'master' },
]
const sortOptions = [
  { label: '승률', value: 'winRate' },
  { label: 'TOP 4', value: 'top4' },
  { label: '평균 등수', value: 'avgPlace' },
]
const partyFilters = ['전체', '랭크', '일반', '커스텀']
const partyPosts = [
  { title: '마스터 이상 듀오 구합니다', mode: '랭크', tier: '마스터+', count: '2/2', close: '마감 15분 전', icon: '♛', tone: 'purple' },
  { title: '다이아 구간 야부/연습 같이해요', mode: '랭크', tier: '다이아+', count: '1/2', close: '마감 42분 전', icon: '◆', tone: 'green' },
  { title: '저녁 근접, 편하게 즐기실 분!', mode: '일반', tier: '제한 없음', count: '3/4', close: '마감 1시간 전', icon: '✦', tone: 'cyan' },
  { title: '주말 마스터 달성 목표!', mode: '랭크', tier: '플래티넘+', count: '2/3', close: '마감 2시간 전', icon: '⚔', tone: 'gold' },
]
const chatChannels = [
  { name: '일반', users: '1,234', message: '새로운 패치 적응 중입니다!', time: '14:58' },
  { name: '덱 공략', users: '856', message: '증강 추천 부탁드려요', time: '14:57' },
  { name: '자유 채팅', users: '2,102', message: '오늘 운 진짜 좋다 ㅋㅋ', time: '14:57' },
  { name: '파티 모집', users: '622', message: '마스터 듀오 구해요~', time: '14:56' },
  { name: '질문 & 답변', users: '741', message: '초보 운영 질문 있습니다', time: '14:56' },
  { name: '아이템 토론', users: '489', message: '쇼진 먼저 잡는 판이 많네요', time: '14:55' },
  { name: '증강 연구', users: '1,018', message: '전투 증강 첫 선택 괜찮나요?', time: '14:55' },
  { name: '초보방', users: '334', message: '연패 운영 언제 끊어야 해요?', time: '14:54' },
]

function goTo(path) {
  window.location.href = path
}

function searchSummoner(input) {
  const trimmed = input.trim()
  if (!trimmed) return
  const [name = trimmed, tag = 'KR1'] = trimmed.split('#')
  goTo(`../SummonerDetail/index.html?name=${encodeURIComponent(name)}&tag=${encodeURIComponent(tag || 'KR1')}`)
}

function filterDecks(items) {
  if (state.dashboardMetaFilter === 'upper') {
    return items.filter((deck) => deck.grade === 'S' || deck.grade === 'A+')
  }

  if (state.dashboardMetaFilter === 'master') {
    return items.filter((deck) => pct(deck.top4) >= 58)
  }

  return items
}

function sortDecks(items) {
  const direction = state.dashboardMetaSort === 'avgPlace' ? 1 : -1
  return [...items].sort((a, b) => {
    const result = pct(a[state.dashboardMetaSort]) - pct(b[state.dashboardMetaSort])
    return result === 0 ? a.rank - b.rank : result * direction
  })
}

function renderMetaFilters() {
  return `
    ${metaFilters.map((filter) => `
      <button type="button" class="${state.dashboardMetaFilter === filter.value ? 'selectedFilter' : ''}" data-dashboard-filter="${filter.value}">${filter.label}</button>
    `).join('')}
    <span aria-hidden="true" class="metaFilterSpacer"></span>
    ${sortOptions.map((option) => `
      <button type="button" class="sortFilter ${state.dashboardMetaSort === option.value ? 'selectedSort' : ''}" data-dashboard-sort="${option.value}">${option.label}</button>
    `).join('')}
  `
}

function renderDeckRows() {
  const visibleDecks = sortDecks(filterDecks(decks)).slice(0, 8)

  if (!visibleDecks.length) return '<p class="emptyState">조건에 맞는 추천 덱이 없습니다.</p>'

  return visibleDecks.map((deck) => `
    <article class="deckRow">
      <strong class="rankNumber">${deck.rank}</strong>
      ${renderTier(deck.grade)}
      <div class="deckInfo">
        <h3>${esc(deck.name)}</h3>
        <div class="traits">${deck.traits.slice(0, 6).map(renderTrait).join('')}</div>
      </div>
      <div class="champions">${deck.champions.slice(0, 6).map(renderChampion).join('')}</div>
      <b class="winRate">${esc(deck.winRate)}</b>
      <b class="top4">${esc(deck.top4)}</b>
      <b class="avgPlace">${esc(deck.avgPlace)}</b>
      <span class="rowArrow">›</span>
    </article>
  `).join('')
}

function renderPartyFilterTabs() {
  return partyFilters.map((filter) => `
    <button type="button" class="${state.dashboardPartyFilter === filter ? 'selectedFilter' : ''}" data-dashboard-party-filter="${filter}">${filter}</button>
  `).join('')
}

function renderPartyRows() {
  const visiblePosts = (state.dashboardPartyFilter === '전체'
    ? partyPosts
    : partyPosts.filter((post) => post.mode === state.dashboardPartyFilter)).slice(0, 4)

  if (!visiblePosts.length) return '<p class="emptyState">조건에 맞는 모집글이 없습니다.</p>'

  return visiblePosts.map((post) => `
    <article class="partyRow">
      <span class="partyIcon ${post.tone}"><span>${post.icon}</span></span>
      <div>
        <h3>${esc(post.title)}</h3>
        <p><span>${esc(post.mode)}</span><span>${esc(post.tier)}</span>${esc(post.count)}</p>
      </div>
      <em>${esc(post.close)}</em>
    </article>
  `).join('')
}

function renderChatRows() {
  return chatChannels.map((channel) => `
    <article class="chatRow">
      <strong>#</strong>
      <b>${esc(channel.name)}</b>
      <span>${esc(channel.users)}</span>
      <p>${esc(channel.message)}</p>
      <time>${esc(channel.time)}</time>
    </article>
  `).join('')
}

function renderDashboardShell() {
  return `
    <div class="dashboardGrid">
      <section class="panel patchCard">
        <div class="patchEmblemArt" aria-hidden="true"></div>
        <div class="patchCopy">
          <h2>17.3 추천 메타</h2>
          <p>5월 20일 업데이트</p>
        </div>
        <button type="button" data-link="../PatchNotes/index.html">패치 노트 보기</button>
      </section>

      <section class="panel searchPanel">
        <h1>소환사 전적 검색</h1>
        <p>소환사명, 태그#KR 등을 입력하세요</p>
        <form class="searchBox" id="summonerSearchForm">
          <input id="summonerSearchInput" aria-label="소환사명 검색" placeholder="소환사명#태그 입력" />
          <button type="submit" aria-label="검색">검색</button>
        </form>
        <div class="searchTags">
          <span>인기 검색</span>
          ${quickTags.map((tag) => `<button type="button" data-quick-summoner="${esc(tag)}">${esc(tag)}</button>`).join('')}
        </div>
      </section>

      <section class="panel metaPanel">
        <div class="panelHeading">
          <div>
            <h2>추천 메타 스냅샷</h2>
            <span>업데이트: 3분 전</span>
          </div>
          <button type="button" data-link="../Decks/index.html">전체 보기 ›</button>
        </div>
        <div class="metaFilters">${renderMetaFilters()}</div>
        <div class="deckList">${renderDeckRows()}</div>
      </section>

      <aside class="rightColumn">
        <section class="panel partyPanel">
          <div class="sideHeading">
            <h2>파티원 찾기</h2>
            <button type="button" data-link="../Party/index.html">더 보기 ›</button>
          </div>
          <div class="smallTabs">${renderPartyFilterTabs()}</div>
          <div class="partyList">${renderPartyRows()}</div>
        </section>

        <section class="panel chatPanel">
          <div class="sideHeading">
            <h2>실시간 채팅</h2>
            <button type="button" data-link="../Party/index.html">더 보기 ›</button>
          </div>
          <div class="chatList">${renderChatRows()}</div>
          <button type="button" class="chatButton" data-link="../Party/index.html">채팅 열기</button>
        </section>

        <section class="panel aiPanel">
          <div>
            <h2><span>AI</span> 덱추천</h2>
            <p>AI가 당신의 덱을 분석하고 최적의 전략을 제안해 드려요.</p>
          </div>
          <div class="aiOptions">
            ${['증강', '아이템', '상성', '배치'].map((label) => `
              <div class="aiOption"><span class="aiOptionIcon">✦</span><span class="aiOptionLabel">${label}</span></div>
            `).join('')}
          </div>
          <button type="button" data-link="../AiRecommend/index.html">분석 시작</button>
        </section>
      </aside>
    </div>
  `
}

function bindDashboardEvents() {
  document.querySelectorAll('[data-link]').forEach((button) => button.addEventListener('click', () => {
    goTo(button.dataset.link)
  }))

  document.getElementById('summonerSearchForm')?.addEventListener('submit', (event) => {
    event.preventDefault()
    searchSummoner(document.getElementById('summonerSearchInput')?.value ?? '')
  })

  document.querySelectorAll('[data-quick-summoner]').forEach((button) => button.addEventListener('click', () => {
    searchSummoner(button.dataset.quickSummoner ?? '')
  }))

  document.querySelectorAll('[data-dashboard-filter]').forEach((button) => button.addEventListener('click', () => {
    state.dashboardMetaFilter = button.dataset.dashboardFilter
    renderDashboard()
  }))

  document.querySelectorAll('[data-dashboard-sort]').forEach((button) => button.addEventListener('click', () => {
    state.dashboardMetaSort = button.dataset.dashboardSort
    renderDashboard()
  }))

  document.querySelectorAll('[data-dashboard-party-filter]').forEach((button) => button.addEventListener('click', () => {
    state.dashboardPartyFilter = button.dataset.dashboardPartyFilter
    renderDashboard()
  }))
}

export function renderDashboard() {
  shell('dashboard', renderDashboardShell())
  bindDashboardEvents()
}
