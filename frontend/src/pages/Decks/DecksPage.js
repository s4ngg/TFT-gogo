import { shell } from '../../components/Header/Header.js'
import { decks } from '../../data/staticData.js'
import { state } from '../../store/staticState.js'
import { esc, focusInput, pct, renderChampion, renderTier, renderTrait } from '../../components/common/renderers.js'

function filterDecks() {
  const query = state.deckQuery.trim().toLowerCase()
  const filtered = decks.filter((deck) => {
    const text = [deck.name, deck.grade, ...deck.traits.map((trait) => trait.name), ...deck.champions.map((unit) => unit.name)].join(' ').toLowerCase()
    return !query || text.includes(query)
  })

  return filtered.sort((a, b) => {
    const av = state.deckSort === 'rank' ? a.rank : pct(a[state.deckSort])
    const bv = state.deckSort === 'rank' ? b.rank : pct(b[state.deckSort])
    const base = av < bv ? -1 : av > bv ? 1 : 0
    const natural = state.deckSort === 'rank' || state.deckSort === 'avgPlace'
    return (natural ? base : -base) * (state.deckDir === 'asc' ? 1 : -1)
  })
}

export function renderDeckTable(items, showRank = true, showTier = true) {
  const th = (label, key) => `<th class="sortable sort-th" data-sort="${key}">${label}${state.deckSort === key ? (state.deckDir === 'asc' ? ' ▲' : ' ▼') : ''}</th>`
  const colSpan = Number(showRank) + Number(showTier) + 6
  return `
    <div class="table-wrap">
      <table class="table">
        <thead><tr>${showRank ? th('순위', 'rank') : ''}${showTier ? '<th>티어</th>' : ''}<th class="name-col">덱 이름 / 시너지</th><th class="champ-col">챔피언 구성</th>${th('승률', 'winRate')}${th('TOP 4', 'top4')}${th('평균 등수', 'avgPlace')}${th('픽률', 'pickRate')}</tr></thead>
        <tbody>
          ${items.length === 0 ? `<tr><td class="empty" colspan="${colSpan}">검색 결과가 없습니다.</td></tr>` : items.map((deck) => `
            <tr class="deck-row" data-id="${deck.rank}">
              ${showRank ? `<td><strong class="rank" data-top="${deck.rank <= 3 ? deck.rank : ''}">${deck.rank}</strong></td>` : ''}
              ${showTier ? `<td>${renderTier(deck.grade)}</td>` : ''}
              <td class="name-col"><div class="deck-title"><strong>${esc(deck.name)}</strong><span class="traits">${deck.traits.map(renderTrait).join('')}</span></div></td>
              <td class="champ-col"><span class="champions">${deck.champions.map(renderChampion).join('')}</span></td>
              <td class="win">${deck.winRate}</td><td class="top4">${deck.top4}</td><td class="avg">#${deck.avgPlace}</td><td class="pick">${deck.pickRate}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `
}

function renderHeroCards() {
  return `
    <section class="special-section">
      <div class="special-header">
        <span class="special-badge">영웅 증강</span>
        <div class="special-header-text">
          <h2>영웅 증강 특수 덱</h2>
          <p>특정 영웅 증강을 보유했을 때만 가능한 고승률 전략</p>
        </div>
      </div>
      <div class="augment-carousel carousel">
        ${decks.slice(0, 4).map((deck, index) => `
          <article class="augment-card card" data-recommended="${index < 3 ? 'true' : 'false'}">
            <div class="aug-card-top card-top">
              <div><div class="aug-hero-name">${esc(deck.champions[0].name)}</div><div class="aug-name">[${esc(deck.name)}]</div></div>
              <span class="${index < 3 ? 'aug-recommend-badge' : 'aug-not-recommend-badge'}">${index < 3 ? '추천' : '비추천'}</span>
            </div>
            <p class="aug-desc">${index === 0 ? '최근 메타와 개인 전적 양쪽에서 안정적인 선택입니다.' : '특정 아이템 완성 시 고점이 높은 조합입니다.'}</p>
            <div class="aug-tags">${deck.traits.slice(0, 3).map((trait) => `<span class="aug-tag">${esc(trait.name)}</span>`).join('')}</div>
            <div class="aug-champs champions">${deck.champions.slice(0, 4).map(renderChampion).join('')}</div>
            <div class="aug-stats"><div><small>승률</small><strong class="win">${deck.winRate}</strong></div><div><small>평균 등수</small><strong class="avg">#${deck.avgPlace}</strong></div><div><small>픽률</small><strong class="pick">${deck.pickRate}</strong></div></div>
          </article>
        `).join('')}
      </div>
    </section>
  `
}

export function bindDeckEvents(renderCurrentPage = renderDecks) {
  document.querySelectorAll('[data-tab]').forEach((button) => button.addEventListener('click', () => {
    state.deckTab = button.dataset.tab
    renderCurrentPage()
  }))
  document.querySelectorAll('[data-sort]').forEach((header) => header.addEventListener('click', () => {
    const key = header.dataset.sort
    if (state.deckSort === key) state.deckDir = state.deckDir === 'asc' ? 'desc' : 'asc'
    else {
      state.deckSort = key
      state.deckDir = key === 'rank' || key === 'avgPlace' ? 'asc' : 'desc'
    }
    renderCurrentPage()
  }))
  document.querySelectorAll('.deck-row').forEach((row) => row.addEventListener('click', () => {
    window.location.href = `../DeckDetail/index.html?id=${row.dataset.id}`
  }))
  const search = document.getElementById('deckSearch')
  if (search) search.addEventListener('input', (event) => {
    state.deckQuery = event.target.value
    renderCurrentPage()
    focusInput('#deckSearch')
  })
}

export function renderDecks() {
  const items = filterDecks()
  const meta = ['S', 'A+', 'A', 'B', 'C', 'D'].map((tier) => {
    const tierDecks = decks.filter((deck) => deck.grade === tier)
    if (!tierDecks.length) return ''
    return `<section class="tier-section"><div class="tier-header"><span>${renderTier(tier)}</span><strong class="tier-label">${tier} 티어</strong><span class="tier-desc">현재 패치 기준 티어 덱</span><span class="tier-count">${tierDecks.length}개</span></div>${renderDeckTable(tierDecks)}</section>`
  }).join('')

  shell('decks', `
    <div class="page">
      <header class="page-header">
        <div class="title-block"><h1>덱모음</h1><p>현재 패치 기준 전체 메타 덱 · 승률 · 픽률 · 평균 등수</p></div>
        <div class="tabbar"><button class="tab-button ${state.deckTab === 'list' ? 'active' : ''}" data-tab="list">덱 모음</button><button class="tab-button ${state.deckTab === 'meta' ? 'active' : ''}" data-tab="meta">메타 통계</button></div>
      </header>
      ${state.deckTab === 'list' ? `
        <div class="tool-bar"><label class="search-box">검색 <input id="deckSearch" value="${esc(state.deckQuery)}" placeholder="덱 이름 검색" /></label><span class="count-label">${items.length}개 덱</span></div>
        ${renderDeckTable(items, false, false)}
        ${renderHeroCards()}
      ` : meta}
    </div>
  `)
  bindDeckEvents()
}
