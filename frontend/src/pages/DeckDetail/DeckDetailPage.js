import { shell } from '../../components/Header/Header.js'
import { renderItem, renderChampion, renderTier, renderTrait } from '../../components/common/renderers.js'
import { decks } from '../../data/staticData.js'

export function renderDeckDetail() {
  const id = Number(new URLSearchParams(window.location.search).get('id') || '1')
  const deck = decks.find((item) => item.rank === id)
  if (!deck) {
    shell('decks', '<div class="page"><a class="ghost-button" href="../Decks/index.html">덱 모음으로</a><section class="panel"><p class="empty">덱 정보를 찾을 수 없습니다.</p></section></div>')
    return
  }

  shell('decks', `
    <div class="page">
      <a class="back-btn ghost-button" href="../Decks/index.html">← 덱 모음으로</a>
      <header class="detail-header">
        ${renderTier(deck.grade)}
        <h1 class="deck-name">${deck.name}</h1>
        <span class="rank-label">메타 #${deck.rank}</span>
      </header>
      <section class="stats-row">
        <div class="stat-item"><small>승률</small><strong class="green win">${deck.winRate}</strong></div>
        <div class="stat-divider"></div>
        <div class="stat-item"><small>TOP 4</small><strong class="cyan top4">${deck.top4}</strong></div>
        <div class="stat-divider"></div>
        <div class="stat-item"><small>평균 등수</small><strong class="purple avg">${deck.avgPlace}등</strong></div>
        <div class="stat-divider"></div>
        <div class="stat-item"><small>픽률</small><strong class="gold pick">${deck.pickRate}</strong></div>
      </section>
      <section class="detail-stack">
        <article class="panel detail-panel"><div class="panel-head"><span class="panel-icon">♛</span><h2>시너지 구성</h2></div><div class="trait-list">${deck.traits.map((trait) => `<div class="trait-item">${renderTrait(trait)}<span class="trait-name">${trait.name}</span><span class="trait-count">${trait.count}조각</span></div>`).join('')}</div></article>
        <article class="panel detail-panel"><div class="panel-head"><span class="panel-icon">▦</span><h2>챔피언 구성</h2><span class="panel-sub">추천 아이템 포함</span></div><div class="champion-grid">${deck.champions.map((unit) => `<div class="champion-item champion-card">${renderChampion(unit)}<span class="champion-name">${unit.name}</span><span class="items">${unit.items.map((item) => renderItem(item, unit.name)).join('')}</span></div>`).join('')}</div></article>
      </section>
    </div>
  `)
}
