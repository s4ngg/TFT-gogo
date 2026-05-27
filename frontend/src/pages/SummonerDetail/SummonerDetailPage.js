import { profileIcon } from '../../constants/index.js'
import { shell } from '../../components/Header/Header.js'
import { esc, renderChampion, renderTrait } from '../../components/common/renderers.js'
import { decks, matches } from '../../data/staticData.js'
import { state } from '../../store/staticState.js'

function players(match) {
  const ranks = [1, 2, 3, 4, 5, 6, 7, 8].filter((rank) => rank !== match.placement)
  const others = decks.slice(0, 5).map((deck, index) => ({ rank: ranks[index], name: ['Faker', 'Mint', 'MetaKing', 'Roller', 'TFTgogo'][index], stage: ['6-3', '5-6', '6-2', '5-5', '5-3'][index], traits: deck.traits.slice(0, 2), units: deck.champions, gold: (index + 2) * 3 }))
  return [...others, { rank: match.placement, name: 'SanChess', stage: '6-1', traits: decks[0].traits.slice(0, 2), units: match.units, gold: 11, me: true }].sort((a, b) => a.rank - b.rank)
}

function renderRecentSummary() {
  return `
    <section class="summary-section">
      <div class="win-rate-donut" style="--pct:66.7%">
        <div class="win-rate-inner"><strong class="win-rate-pct">66.7%</strong></div>
      </div>
      <div class="summary-stats">
        <p class="summary-stat-label">순방 확률</p>
        <p class="summary-stat-value">20W 10L <span class="summary-stat-sub">(66.7%)</span></p>
        <p class="summary-stat-label">평균 순위</p>
        <p class="summary-stat-value">3.6<span class="summary-stat-th">th</span> / 8</p>
      </div>
    </section>
  `
}

function renderTopTraits() {
  const traits = decks.flatMap((deck) => deck.traits).slice(0, 6)
  return `
    <section class="stat-section">
      <h2 class="stat-section-title">많이 플레이한 시너지</h2>
      <div class="top-trait-list">
        ${traits.map((trait, index) => `<div class="top-trait-row"><span class="top-rank">${index + 1}</span>${renderTrait(trait)}<span class="top-name">${esc(trait.name)}</span><span class="top-games">${32 - index * 3}게임</span><span class="top-avg">평균 ${(3.2 + index * 0.2).toFixed(1)}등</span></div>`).join('')}
      </div>
    </section>
  `
}

function renderTopChampions() {
  const champions = decks.flatMap((deck) => deck.champions).slice(0, 6)
  return `
    <section class="stat-section">
      <h2 class="stat-section-title">많이 플레이한 챔피언</h2>
      <div class="top-champ-list">
        ${champions.map((unit, index) => `<div class="top-champ-row"><span class="top-rank">${index + 1}</span><span class="champ-thumb-wrap"><img class="champ-thumb" src="${unit.imageUrl}" alt="${esc(unit.name)}" /><span class="champ-cost">${Math.min(5, index + 1)}</span></span><span class="top-name">${esc(unit.name)}</span><span class="top-games">${38 - index * 3}게임</span><span class="top-avg">평균 ${(3.1 + index * 0.2).toFixed(1)}등</span></div>`).join('')}
      </div>
    </section>
  `
}

function renderMatchDetail(match) {
  return `<div class="match-detail ${state.expandedMatch === match.id ? 'open' : ''}"><div class="match-detail-header"><span>#</span><span>소환사</span><span>스테이지</span><span>시너지</span><span>챔피언</span><span>잔여골드</span></div>${players(match).map((player) => `<div class="player-row ${player.me ? 'me' : ''}"><strong>${player.rank}위</strong><span>${esc(player.name)}</span><span>${esc(player.stage)}</span><span class="traits">${player.traits.map(renderTrait).join('')}</span><span class="champions">${player.units.slice(0, 4).map(renderChampion).join('')}</span><span>${player.gold}G</span></div>`).join('')}</div>`
}

function renderMatch(match) {
  const open = state.expandedMatch === match.id
  const resultClass = match.placement === 1 ? 'gold-row' : match.placement <= 4 ? 'top4-row' : 'bot4-row'
  return `<div class="match-item"><article class="match-row ${resultClass} ${open ? 'match-row-open' : ''}" data-match="${match.id}"><div class="placement-badge placement ${match.placement > 4 ? 'bot4' : ''}"><span>${match.placement}위</span></div><div class="match-meta"><p class="deck-name">${esc(match.deckName)}</p><p class="time-ago">${esc(match.timeAgo)}</p></div><div class="unit-list champions">${match.units.slice(0, 5).map(renderChampion).join('')}</div><div class="lp-delta ${match.lpDelta >= 0 ? 'lp-gain win' : 'lp-loss pick'}"><span>${match.lpDelta >= 0 ? '+' : ''}${match.lpDelta} LP</span><span class="chevron">${open ? '⌃' : '⌄'}</span></div></article>${renderMatchDetail(match)}</div>`
}

function bindSummonerEvents() {
  document.getElementById('summonerForm').addEventListener('submit', (event) => {
    event.preventDefault()
    const value = document.getElementById('summonerSearch').value.trim()
    if (!value) return
    const [name = value, tag = 'KR1'] = value.split('#')
    window.location.href = `../SummonerDetail/index.html?name=${encodeURIComponent(name)}&tag=${encodeURIComponent(tag || 'KR1')}`
  })
  document.querySelectorAll('[data-match]').forEach((row) => row.addEventListener('click', () => {
    const id = Number(row.dataset.match)
    state.expandedMatch = state.expandedMatch === id ? null : id
    renderSummonerDetail()
  }))
  const load = document.querySelector('[data-load-more]')
  if (load) load.addEventListener('click', () => {
    state.visibleMatches = Math.min(matches.length, state.visibleMatches + 30)
    renderSummonerDetail()
  })
}

export function renderSummonerDetail() {
  const params = new URLSearchParams(window.location.search)
  const name = params.get('name') || 'SanChess'
  const tag = params.get('tag') || 'KR1'
  const visible = matches.slice(0, state.visibleMatches)
  const dist = [18, 22, 21, 20, 19, 17, 16, 10]
  const max = Math.max(...dist)

  shell('summoner', `
    <div class="page">
      <form class="top-search panel toolbar" id="summonerForm"><label class="search-box">검색 <input id="summonerSearch" placeholder="소환사명#태그 검색" /></label><button class="primary-button" type="submit">검색</button></form>
      <section class="profile-card"><div class="profile-icon-wrap"><img src="${profileIcon(29)}" alt="프로필" /><span>387</span></div><div class="profile-info"><h1>${esc(name)} <span class="summoner-tag">#${esc(tag)}</span></h1><p class="tier-line">Diamond IV · 45 LP</p><div class="record-line"><span>256승 137패</span><span class="win-rate-text">승률 65%</span><span class="avg-place-text">평균 3.6등</span><span class="top4-text">TOP4 67%</span></div></div><div class="profile-right"><button class="update-btn" type="button">전적 업데이트</button><div class="rank-dist">${dist.map((count, index) => `<div class="rank-bar"><i style="height:${Math.max(6, count / max * 56)}px"></i><span>${index + 1}</span><small>${count}</small></div>`).join('')}</div></div></section>
      <div class="stat-grid summoner-stat-grid">${renderTopTraits()}${renderTopChampions()}</div>
      <section class="match-section"><h2>최근 30게임</h2>${renderRecentSummary()}<div class="match-list">${visible.map(renderMatch).join('')}</div>${state.visibleMatches < matches.length ? `<button class="load-more-btn ghost-button" data-load-more>30개 더 보기 (${matches.length - state.visibleMatches}개 남음)</button>` : ''}</section>
    </div>
  `)
  bindSummonerEvents()
}
