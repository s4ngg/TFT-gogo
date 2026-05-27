import { shell } from '../../components/Header/Header.js'
import { esc, focusInput, renderTags } from '../../components/common/renderers.js'
import { patchChanges } from '../../data/staticData.js'
import { state } from '../../store/staticState.js'

const PATCH_PAGE_SIZE = 7
const categoryCounts = ['챔피언', '시너지', '아이템', '증강체', '시스템'].reduce((acc, category) => {
  acc[category] = patchChanges.filter((change) => change.category === category).length
  return acc
}, { 전체: patchChanges.length })

function clampPage(page, totalPages) {
  return Math.min(Math.max(page, 1), totalPages)
}

function pagedChanges(changes) {
  const totalPages = Math.max(1, Math.ceil(changes.length / PATCH_PAGE_SIZE))
  state.patchPage = clampPage(state.patchPage, totalPages)
  const start = (state.patchPage - 1) * PATCH_PAGE_SIZE
  return {
    totalPages,
    changes: changes.slice(start, start + PATCH_PAGE_SIZE),
  }
}

function renderPatchPagination(totalPages) {
  if (totalPages <= 1) return ''

  return `
    <nav class="pagination patch-pagination" aria-label="패치노트 페이지">
      ${Array.from({ length: totalPages }, (_, index) => {
        const page = index + 1
        return `<button class="page-button ${state.patchPage === page ? 'active' : ''}" data-patch-page="${page}">${page}</button>`
      }).join('')}
    </nav>
  `
}

function typeClass(type) {
  return {
    상향: 'buff',
    하향: 'nerf',
    조정: 'adjust',
    신규: 'new',
  }[type] ?? ''
}

function impactClass(impact) {
  return {
    높음: 'high-impact',
    중간: 'mid-impact',
    낮음: 'low-impact',
  }[impact] ?? ''
}

function renderPatchItem(change) {
  const open = state.expandedPatch.includes(change.id)
  return `<article class="change-item"><div class="change-top"><span class="category-badge">${esc(change.category)}</span><span class="change-type ${typeClass(change.type)}">${esc(change.type)}</span><span class="impact-badge ${impactClass(change.impact)}">영향 ${esc(change.impact)}</span></div><div class="change-body"><div><h3>${esc(change.target)}</h3><p>${esc(change.summary)}</p></div><button class="detail-button ghost-button" data-expand="${change.id}">${open ? '접기' : '상세 보기'} ${open ? '▾' : '›'}</button></div>${open ? `<div class="compare-grid"><div><span>이전</span><p>${esc(change.before)}</p></div><div><span>변경</span><p>${esc(change.after)}</p></div></div>` : ''}${renderTags(change.tags)}</article>`
}

function bindPatchEvents() {
  document.querySelectorAll('[data-patch-category]').forEach((button) => button.addEventListener('click', () => {
    state.patchCategory = button.dataset.patchCategory
    state.patchPage = 1
    state.expandedPatch = []
    renderPatchNotes()
  }))
  document.querySelectorAll('[data-patch-type]').forEach((button) => button.addEventListener('click', () => {
    state.patchType = button.dataset.patchType
    state.patchPage = 1
    state.expandedPatch = []
    renderPatchNotes()
  }))
  const high = document.querySelector('[data-patch-high]')
  if (high) high.addEventListener('click', () => {
    state.patchHighOnly = !state.patchHighOnly
    state.patchPage = 1
    renderPatchNotes()
  })
  const search = document.getElementById('patchSearch')
  if (search) search.addEventListener('input', (event) => {
    state.patchQuery = event.target.value
    state.patchPage = 1
    renderPatchNotes()
    focusInput('#patchSearch')
  })
  document.querySelectorAll('[data-patch-page]').forEach((button) => button.addEventListener('click', () => {
    state.patchPage = Number(button.dataset.patchPage)
    state.expandedPatch = []
    renderPatchNotes()
  }))
  document.querySelectorAll('[data-expand]').forEach((button) => button.addEventListener('click', () => {
    const id = Number(button.dataset.expand)
    state.expandedPatch = state.expandedPatch.includes(id) ? state.expandedPatch.filter((item) => item !== id) : [...state.expandedPatch, id]
    renderPatchNotes()
  }))
}

export function renderPatchNotes() {
  const categories = ['전체', '챔피언', '시너지', '아이템', '증강체', '시스템']
  const types = ['전체 변경', '상향', '하향', '조정', '신규']
  const query = state.patchQuery.trim().toLowerCase()
  const filtered = patchChanges.filter((change) => {
    const category = state.patchCategory === '전체' || change.category === state.patchCategory
    const type = state.patchType === '전체 변경' || change.type === state.patchType
    const impact = !state.patchHighOnly || change.impact === '높음'
    const text = [change.target, change.summary, change.category, change.type, ...change.tags].join(' ').toLowerCase()
    return category && type && impact && (!query || text.includes(query))
  })
  const page = pagedChanges(filtered)

  shell('patch', `
    <div class="page">
      <header class="hero"><div class="hero-copy"><span class="kicker">17.3 PATCH NOTE</span><h1>패치 노트</h1><p>챔피언, 시너지, 아이템, 증강체 변경사항을 한 화면에서 확인하고 현재 메타 흐름을 빠르게 파악합니다.</p><div class="hero-meta"><span>적용일 2026.05.26</span><span>변경 ${patchChanges.length}건</span><span>핵심 영향 ${patchChanges.filter((change) => change.impact === '높음').length}건</span></div></div><aside class="release-card"><div class="release-art"></div><div><span class="section-label">현재 버전</span><h2>v17.3</h2><p>AP 전환 조합과 후반 캐리 라인 조정</p></div></aside></header>
      <section class="summary-grid">
        <article class="summary-card"><span class="summary-icon">↗</span><div><strong>${patchChanges.filter((change) => change.type === '상향').length}</strong><p>상향 항목</p></div></article>
        <article class="summary-card"><span class="summary-icon warn-icon">!</span><div><strong>${patchChanges.filter((change) => change.type === '하향').length}</strong><p>하향 항목</p></div></article>
        <article class="summary-card"><span class="summary-icon gold-icon">★</span><div><strong>AP</strong><p>주요 상승 메타</p></div></article>
        <article class="summary-card"><span class="summary-icon blue-icon">⌁</span><div><strong>중반</strong><p>운영 전환 구간</p></div></article>
      </section>
      <div class="content-grid">
        <section class="change-panel panel">
          <div class="panel-header"><div><span class="section-label">변경사항</span><h2>패치 상세 목록</h2></div><span class="small-pill">${filtered.length}건 · ${state.patchPage}/${page.totalPages}</span></div>
          <div class="tool-bar"><label class="search-box">검색 <input id="patchSearch" value="${esc(state.patchQuery)}" placeholder="챔피언, 아이템, 키워드 검색" /></label><span class="filter-label">카테고리</span></div>
          <div class="category-tabs filter-row">${categories.map((category) => `<button class="filter-button ${state.patchCategory === category ? 'active' : ''}" data-patch-category="${category}">${category}<span>${categoryCounts[category] ?? 0}</span></button>`).join('')}</div>
          <div class="quick-filters filter-row"><button class="filter-button ${state.patchHighOnly ? 'active' : ''}" data-patch-high>영향 높음만</button><div class="type-filters">${types.map((type) => `<button class="filter-button ${state.patchType === type ? 'active' : ''}" data-patch-type="${type}">${type}</button>`).join('')}</div></div>
          <div class="change-list">${page.changes.map(renderPatchItem).join('') || '<p class="empty">조건에 맞는 변경사항이 없습니다.</p>'}</div>
          ${renderPatchPagination(page.totalPages)}
        </section>
        <aside class="side-rail">
          <section class="insight-panel panel"><span class="section-label">요약</span><h2>이번 패치 핵심</h2><ul><li>마법사와 AP 캐리 조합의 중반 전환 가치가 상승했습니다.</li><li>저코스트 리롤 증강체는 초반 압박력이 완화됐습니다.</li><li>공속 기반 장기전 캐리는 아이템 의존도를 더 확인해야 합니다.</li></ul></section>
          <section class="history-panel panel"><span class="section-label">히스토리</span><h2>이전 패치</h2><div class="history-list"><article class="current-patch"><div><strong>17.3</strong><span>2026.05.26</span></div><p>AP 전환 조합 보강</p><small>마법사, 아펠리오스, 리롤 증강체 조정</small></article><article><div><strong>17.2</strong><span>2026.05.12</span></div><p>리롤 덱 속도 조절</p><small>저코스트 캐리, 초반 골드 증강체 하향</small></article><article><div><strong>17.1</strong><span>2026.04.29</span></div><p>세트 오픈 밸런스</p><small>신규 시너지 안정화</small></article></div></section>
        </aside>
      </div>
    </div>
  `)
  bindPatchEvents()
}
