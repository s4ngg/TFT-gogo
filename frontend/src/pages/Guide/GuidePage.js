import { shell } from '../../components/Header/Header.js'
import { esc, focusInput, renderChampion, renderItem, renderTags, renderTrait } from '../../components/common/renderers.js'
import { decks, guideData } from '../../data/staticData.js'
import { state } from '../../store/staticState.js'

const tabs = [
  { key: 'traits', label: '시너지', meta: '활성 구간 / 핵심 기물' },
  { key: 'items', label: '아이템', meta: '추천 사용자 / 조합' },
  { key: 'augments', label: '증강체', meta: '선택 타이밍 / 보상' },
  { key: 'champions', label: '챔피언', meta: '코스트 / 추천 아이템' },
]

const plannerStages = ['2-1', '3-2', '4-2', '5-1']
const GUIDE_PAGE_SIZE = 6
const GUIDE_PAGE_GROUP_SIZE = 5
const GUIDE_FAVORITES_KEY = 'tftgogo:guide:favorites'

const championCostFilters = [
  { key: 'all', label: '전체' },
  { key: '1-2', label: '1-2코스트' },
  { key: '3-4', label: '3-4코스트' },
  { key: '5', label: '5코스트' },
]

let guideFavoritesLoaded = false

function visibleGuideTags(tags = []) {
  return tags.filter((tag) => !/^P\d+$/i.test(tag))
}

function renderGuideTags(tags = []) {
  return renderTags(visibleGuideTags(tags))
}

function activeTab() {
  return tabs.find((tab) => tab.key === state.guideTab) ?? tabs[0]
}

function loadGuideFavorites() {
  if (guideFavoritesLoaded) return
  guideFavoritesLoaded = true

  if (typeof window === 'undefined') return

  try {
    const stored = JSON.parse(window.localStorage.getItem(GUIDE_FAVORITES_KEY) ?? '[]')
    if (Array.isArray(stored)) {
      state.favorites = stored.filter((item) => typeof item === 'string').slice(0, 8)
    }
  } catch {
    state.favorites = []
  }
}

function saveGuideFavorites() {
  if (typeof window === 'undefined') return

  try {
    window.localStorage.setItem(GUIDE_FAVORITES_KEY, JSON.stringify(state.favorites))
  } catch {
    // localStorage can be unavailable depending on browser privacy settings.
  }
}

function clampPage(page, totalPages) {
  return Math.min(Math.max(page, 1), totalPages)
}

function pagedItems(items) {
  const totalPages = Math.max(1, Math.ceil(items.length / GUIDE_PAGE_SIZE))
  state.guidePage = clampPage(state.guidePage, totalPages)
  const start = (state.guidePage - 1) * GUIDE_PAGE_SIZE
  return {
    totalPages,
    items: items.slice(start, start + GUIDE_PAGE_SIZE),
  }
}

function renderGuidePagination(totalPages) {
  if (totalPages <= 1) return ''

  const groupStart = Math.floor((state.guidePage - 1) / GUIDE_PAGE_GROUP_SIZE) * GUIDE_PAGE_GROUP_SIZE + 1
  const groupEnd = Math.min(groupStart + GUIDE_PAGE_GROUP_SIZE - 1, totalPages)
  const pages = Array.from({ length: groupEnd - groupStart + 1 }, (_, index) => groupStart + index)
  const prevGroup = groupStart > 1
    ? `<button class="page-button guide-page-jump" data-guide-page="${groupStart - 1}">이전</button>`
    : ''
  const nextGroup = groupEnd < totalPages
    ? `<button class="page-button guide-page-jump" data-guide-page="${groupEnd + 1}">다음</button>`
    : ''

  return `
    <nav class="pagination guide-pagination" aria-label="가이드 페이지">
      ${prevGroup}
      ${pages.map((page) => {
        return `<button class="page-button ${state.guidePage === page ? 'active' : ''}" data-guide-page="${page}">${page}</button>`
      }).join('')}
      ${nextGroup}
    </nav>
  `
}

function uniqueChampions() {
  const seen = new Set()
  return decks.flatMap((deck) => deck.champions.map((champion) => ({ ...champion, deckName: deck.name })))
    .filter((champion) => {
      const key = `${champion.name}-${champion.imageUrl}`
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
}

function championGuideItems() {
  const deckChampions = uniqueChampions()
  const guidedNames = new Set(guideData.champions.map((item) => item.name))
  const guided = guideData.champions.map((item, index) => {
    const match = deckChampions.find((champion) => champion.name === item.name || champion.imageUrl === item.imageUrl)
    return {
      ...item,
      cost: (index % 5) + 1,
      deckName: match?.deckName ?? decks[index % decks.length].name,
      items: match?.items ?? decks[index % decks.length].champions[0].items,
    }
  })

  if (guided.length >= GUIDE_PAGE_SIZE * 7) return guided.slice(0, GUIDE_PAGE_SIZE * 7)

  const extra = deckChampions
    .filter((champion) => !guidedNames.has(champion.name))
    .slice(0, 9)
    .map((champion, index) => ({
      name: champion.name,
      imageUrl: champion.imageUrl,
      summary: `${champion.deckName} 조합에서 자주 쓰이는 핵심 기물입니다.`,
      tags: [`${(index % 5) + 1}코스트`, champion.items?.length ? '추천 아이템 보유' : '유틸 기물', champion.deckName],
      cost: (index % 5) + 1,
      deckName: champion.deckName,
      items: champion.items ?? [],
    }))

  return [...guided, ...extra].slice(0, GUIDE_PAGE_SIZE * 7)
}

function itemsForTab(tabKey) {
  if (tabKey === 'champions') return championGuideItems()
  return guideData[tabKey] ?? []
}

function filteredItems() {
  const query = state.guideQuery.trim().toLowerCase()
  return itemsForTab(state.guideTab).filter((item) => {
    const text = [item.name, item.summary, item.deckName, ...(item.tags ?? [])].join(' ').toLowerCase()
    const matchesQuery = !query || text.includes(query)
    const matchesCost = state.guideTab !== 'champions' || state.guideCost === 'all' || (
      state.guideCost === '1-2' && item.cost <= 2
    ) || (
      state.guideCost === '3-4' && item.cost >= 3 && item.cost <= 4
    ) || (
      state.guideCost === '5' && item.cost === 5
    )

    return matchesQuery && matchesCost
  })
}

function renderChampionCostFilters() {
  return championCostFilters.map((filter) => `
    <button class="filter-button ${state.guideCost === filter.key ? 'active' : ''}" type="button" data-guide-cost="${filter.key}">${filter.label}</button>
  `).join('')
}

function topDeckForItem(item) {
  return decks.find((deck) => deck.traits.some((trait) => item.tags?.some((tag) => trait.name.includes(tag) || tag.includes(trait.name)))) ?? decks[0]
}

function bestUsersForItem(item) {
  const users = decks.flatMap((deck) => deck.champions)
    .filter((champion) => champion.items?.includes(item.imageUrl))
  return (users.length ? users : decks[0].champions).slice(0, 4)
}

function renderGuideTabs() {
  return tabs.map((tab, index) => `
    <button class="guide-tab-button ${state.guideTab === tab.key ? 'active' : ''}" data-guide-tab="${tab.key}">
      <span class="guide-tab-index">0${index + 1}</span>
      <span>
        <strong>${tab.label}</strong>
        <small>${tab.meta}</small>
      </span>
    </button>
  `).join('')
}

function renderTraitGuide(items) {
  if (!items.length) return '<p class="empty">검색 결과가 없습니다.</p>'

  return `
    <div class="trait-layout">
      <div class="trait-grid">
        ${items.map((item, index) => {
          const deck = topDeckForItem(item)
          const levels = (item.tags?.[0] ?? '2/4/6').split('/').slice(0, 4)
          return `
            <article class="trait-card">
              <div class="trait-top">
                <img src="${item.imageUrl}" alt="" />
                <div>
                  <strong>${esc(item.name)}</strong>
                  <p>${esc(item.summary)}</p>
                </div>
              </div>
              <div class="level-track">
                ${levels.map((level, levelIndex) => `<span class="${levelIndex <= index ? 'level-active' : ''}">${esc(level)}</span>`).join('')}
              </div>
              <div class="champion-line">${deck.champions.slice(0, 5).map(renderChampion).join('')}</div>
              <div class="tip-line">
                <strong>추천 운영</strong>
                <span>${esc(deck.name)} 기준으로 ${esc(deck.traits[0]?.count ?? 2)}단계 활성화가 안정적입니다.</span>
              </div>
              ${renderGuideTags(item.tags ?? [])}
            </article>
          `
        }).join('')}
      </div>
    </div>
  `
}

function renderItemGuide(items) {
  if (!items.length) return '<p class="empty">검색 결과가 없습니다.</p>'

  return `
    <div class="table-wrap guide-table-wrap">
      <table class="item-table">
        <colgroup>
          <col class="item-info-col" />
          <col class="item-user-col" />
          <col class="item-role-col" />
          <col class="item-metric-col" />
          <col class="item-note-col" />
        </colgroup>
        <thead>
          <tr>
            <th class="name-col">아이템</th>
            <th>추천 챔피언</th>
            <th>사용 구간</th>
            <th>핵심 수치</th>
            <th>조합 메모</th>
          </tr>
        </thead>
        <tbody>
          ${items.map((item, index) => {
            const users = bestUsersForItem(item)
            return `
              <tr>
                <td class="name-col">
                  <div class="item-name-cell">
                    ${renderItem(item.imageUrl, item.name)}
                    <div>
                      <strong>${esc(item.name)}</strong>
                      <p>${esc(item.summary)}</p>
                    </div>
                  </div>
                </td>
                <td>
                  <div class="avatar-stack">${users.map(renderChampion).join('')}</div>
                </td>
                <td class="guide-role-cell">${index === 0 ? 'AD 캐리' : index === 1 ? '메인 탱커' : 'AP 캐리'}</td>
                <td class="guide-metric-cell">${renderGuideTags(item.tags ?? [])}</td>
                <td class="combo-cell guide-note-cell">${esc(decks[index % decks.length].name)}와 같이 쓰면 평균 등수가 안정적입니다.</td>
              </tr>
            `
          }).join('')}
        </tbody>
      </table>
    </div>
    <div class="metric-cards">
      ${items.slice(0, 3).map((item, index) => `
        <article>
          <span>${index === 0 ? '가장 높은 승률' : index === 1 ? '탱커 효율' : '후반 캐리력'}</span>
          <strong>${esc(visibleGuideTags(item.tags ?? [])[0] ?? '추천')}</strong>
          <p>${esc(item.name)}</p>
        </article>
      `).join('')}
    </div>
  `
}

function renderAugmentGuide(items) {
  if (!items.length) return '<p class="empty">검색 결과가 없습니다.</p>'

  return `
    <div class="augment-layout">
      <div class="table-wrap guide-table-wrap">
        <table class="augment-table">
          <colgroup>
            <col class="augment-info-col" />
            <col class="augment-deck-col" />
            <col class="augment-stage-col" />
            <col class="augment-effect-col" />
          </colgroup>
          <thead>
            <tr>
              <th class="name-col">증강체</th>
              <th>추천 덱</th>
              <th>타이밍</th>
              <th>기대 효과</th>
            </tr>
          </thead>
          <tbody>
            ${items.map((item, index) => {
              const deck = decks[index % decks.length]
              return `
                <tr>
                  <td class="name-col">
                    <div class="augment-name-cell">
                      <span class="augment-mark">${index + 1}</span>
                      <div>
                        <strong>${esc(item.name)}</strong>
                        <p>${esc(item.summary)}</p>
                        ${renderGuideTags(item.tags ?? [])}
                      </div>
                    </div>
                  </td>
                  <td>
                    <div class="mini-deck-cell">
                      <strong>${esc(deck.name)}</strong>
                      <div class="traits">${deck.traits.slice(0, 3).map(renderTrait).join('')}</div>
                    </div>
                  </td>
                  <td class="reward-cell guide-stage-cell">${plannerStages[index % plannerStages.length]}</td>
                  <td class="combo-cell guide-note-cell">전투력 보강과 리롤 타이밍을 동시에 잡는 선택지입니다.</td>
                </tr>
              `
            }).join('')}
          </tbody>
        </table>
      </div>
      <aside class="planner-panel">
        <div class="planner-top">
          <span class="kicker">PLANNER</span>
          <h3>증강 선택 흐름</h3>
        </div>
        <div class="stage-cards">
          ${plannerStages.map((stage, index) => `
            <article class="stage-card">
              <span>${stage}</span>
              <strong>${esc(items[index % items.length].name)}</strong>
              <p>${index < 2 ? '초반 방향성 확정' : '완성 덱 전투력 보강'}</p>
            </article>
          `).join('')}
        </div>
        <div class="board-tool">
          <strong>운영 메모</strong>
          <p>선택한 증강체가 현재 덱의 핵심 캐리, 탱커, 시너지 활성 구간을 동시에 밀어주는지 확인하세요.</p>
        </div>
      </aside>
    </div>
  `
}

function renderChampionGuide(items) {
  if (!items.length) return '<p class="empty">검색 결과가 없습니다.</p>'

  return `
    <div class="cost-filter">
      ${renderChampionCostFilters()}
    </div>
    <div class="champion-guide-grid">
      ${items.map((item) => {
        const active = state.favorites.includes(item.name)
        return `
          <article class="guide-champion-card">
            <button class="favorite-button champion-favorite ${active ? 'active' : ''}" data-favorite="${esc(item.name)}" aria-label="즐겨찾기">★</button>
            <div class="champion-portrait">
              <img src="${item.imageUrl}" alt="${esc(item.name)}" />
              <span>${esc(item.cost ?? 4)}</span>
            </div>
            <div class="champion-card-body">
              <strong>${esc(item.name)}</strong>
              <p>${esc(item.summary)}</p>
              <div class="item-icon-strip">${(item.items ?? []).slice(0, 3).map((url) => renderItem(url, item.name)).join('') || '<span class="small-pill">유틸</span>'}</div>
              ${renderGuideTags((item.tags ?? []).slice(0, 3))}
            </div>
          </article>
        `
      }).join('')}
    </div>
  `
}

function renderGuideContent(items) {
  if (state.guideTab === 'traits') return renderTraitGuide(items)
  if (state.guideTab === 'items') return renderItemGuide(items)
  if (state.guideTab === 'augments') return renderAugmentGuide(items)
  return renderChampionGuide(items)
}

function renderQuickAccess(items) {
  const recent = items.slice(0, 3)
  const selectedFavorite = state.guideTab === 'champions' ? state.guideQuery : ''
  const selectedShortcut = state.guideQuery
  return `
    <section class="panel quick-access">
      <div class="panel-header">
        <h2>빠른 접근</h2>
        <span class="small-pill">${activeTab().label}</span>
      </div>
      <div class="quick-group">
        <strong>즐겨찾기</strong>
        <div class="quick-links favorite-links">
          ${state.favorites.map((name) => {
            const selected = selectedFavorite === name
            return `
            <span class="quick-link-item ${selected ? 'active' : ''}">
              <button type="button" data-guide-favorite-shortcut="${esc(name)}">${esc(name)}</button>
              ${selected ? `<button class="quick-clear-button" type="button" data-guide-favorite-clear aria-label="${esc(name)} 선택 해제">x</button>` : ''}
            </span>
          `
          }).join('') || '<span class="muted-text">챔피언 즐겨찾기가 비어 있습니다.</span>'}
        </div>
      </div>
      <div class="quick-group">
        <strong>현재 탭 추천</strong>
        <div class="quick-links">
          ${recent.map((item) => {
            const selected = selectedShortcut === item.name
            return `
              <span class="quick-link-item ${selected ? 'active' : ''}">
                <button type="button" data-guide-shortcut="${esc(item.name)}">${esc(item.name)}</button>
                ${selected ? `<button class="quick-clear-button" type="button" data-guide-shortcut-clear aria-label="${esc(item.name)} 선택 해제">x</button>` : ''}
              </span>
            `
          }).join('')}
        </div>
      </div>
    </section>
  `
}

function renderRewardPanel() {
  return `
    <section class="panel reward-panel">
      <div class="panel-header">
        <h2>메타 체크</h2>
        <span class="badge">LIVE</span>
      </div>
      <div class="reward-list">
        ${decks.slice(0, 4).map((deck, index) => `
          <article class="reward-row">
            <span class="rank" data-top="${index + 1}">${index + 1}</span>
            <div>
              <strong>${esc(deck.name)}</strong>
              <p>${esc(deck.winRate)} 승률 · 평균 ${esc(deck.avgPlace)}</p>
            </div>
            <div class="traits">${deck.traits.slice(0, 2).map(renderTrait).join('')}</div>
          </article>
        `).join('')}
      </div>
    </section>
  `
}

function bindGuideEvents() {
  document.querySelectorAll('[data-guide-tab]').forEach((button) => button.addEventListener('click', () => {
    state.guideTab = button.dataset.guideTab
    state.guideQuery = ''
    state.guidePage = 1
    renderGuide()
  }))

  document.querySelectorAll('[data-guide-shortcut]').forEach((button) => button.addEventListener('click', () => {
    state.guideQuery = button.dataset.guideShortcut
    if (state.guideTab === 'champions') state.guideCost = 'all'
    state.guidePage = 1
    renderGuide()
    focusInput('#guideSearch')
  }))

  document.querySelectorAll('[data-guide-shortcut-clear]').forEach((button) => button.addEventListener('click', () => {
    state.guideQuery = ''
    if (state.guideTab === 'champions') state.guideCost = 'all'
    state.guidePage = 1
    renderGuide()
    focusInput('#guideSearch')
  }))

  document.querySelectorAll('[data-guide-favorite-shortcut]').forEach((button) => button.addEventListener('click', () => {
    state.guideTab = 'champions'
    state.guideQuery = button.dataset.guideFavoriteShortcut
    state.guideCost = 'all'
    state.guidePage = 1
    renderGuide()
    focusInput('#guideSearch')
  }))

  document.querySelectorAll('[data-guide-favorite-clear]').forEach((button) => button.addEventListener('click', () => {
    state.guideTab = 'champions'
    state.guideQuery = ''
    state.guideCost = 'all'
    state.guidePage = 1
    renderGuide()
    focusInput('#guideSearch')
  }))

  const search = document.getElementById('guideSearch')
  if (search) search.addEventListener('input', (event) => {
    state.guideQuery = event.target.value
    state.guidePage = 1
    renderGuide()
    focusInput('#guideSearch')
  })

  document.querySelectorAll('[data-guide-page]').forEach((button) => button.addEventListener('click', () => {
    state.guidePage = Number(button.dataset.guidePage)
    renderGuide()
  }))

  document.querySelectorAll('[data-guide-cost]').forEach((button) => button.addEventListener('click', () => {
    state.guideCost = button.dataset.guideCost
    state.guidePage = 1
    renderGuide()
  }))

  document.querySelectorAll('[data-favorite]').forEach((button) => button.addEventListener('click', () => {
    const name = button.dataset.favorite
    state.favorites = state.favorites.includes(name)
      ? state.favorites.filter((item) => item !== name)
      : [name, ...state.favorites].slice(0, 8)
    saveGuideFavorites()
    renderGuide()
  }))
}

export function renderGuide() {
  loadGuideFavorites()

  const tab = activeTab()
  const items = filteredItems()
  const page = pagedItems(items)

  shell('guide', `
    <div class="page">
      <header class="page-header guide-page-header">
        <div class="title-block">
          <span class="kicker">SET 17 GUIDE</span>
          <h1>게임 가이드</h1>
          <p>시너지, 아이템, 증강체, 챔피언 정보를 원본 화면처럼 탭별 상세 구조로 정리했습니다.</p>
        </div>
        <div class="header-stats">
          <article><span>GUIDE</span><strong>${itemsForTab(state.guideTab).length}</strong></article>
          <article><span>FAVORITE</span><strong>${state.favorites.length}</strong></article>
          <article><span>PATCH</span><strong>17.3</strong></article>
        </div>
      </header>
      <section class="panel guide-control-panel">
        <div class="guide-tabbar">${renderGuideTabs()}</div>
        <label class="search-box guide-search">검색<input id="guideSearch" value="${esc(state.guideQuery)}" placeholder="${tab.label} 검색" /></label>
      </section>
      <div class="guide-layout">
        <section class="panel guide-main-panel">
          <div class="panel-header">
            <div>
              <span class="section-label">${esc(tab.meta)}</span>
              <h2>${esc(tab.label)} 목록</h2>
            </div>
          </div>
          ${renderGuideContent(page.items)}
          ${renderGuidePagination(page.totalPages)}
        </section>
        <aside class="guide-side-stack">
          ${renderQuickAccess(itemsForTab(state.guideTab))}
          ${renderRewardPanel()}
        </aside>
      </div>
    </div>
  `)
  bindGuideEvents()
}
