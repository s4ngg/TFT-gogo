import { shell } from '../../components/Header/Header.js'
import { esc, renderChampion, renderTags, renderTier, renderTrait } from '../../components/common/renderers.js'
import { decks, guideData } from '../../data/staticData.js'

const summaryStats = [
  { label: '평균 등수', value: '4.1', tone: 'avg', note: '최근 20게임' },
  { label: 'TOP4', value: '58.0%', tone: 'top4', note: '상위권 안정성' },
  { label: '1등 비율', value: '22.5%', tone: 'pick', note: '고점 확보력' },
  { label: '추천 신뢰도', value: '86', tone: 'win', note: '패치 메타 반영' },
]

const augmentScores = [
  { score: 92, timing: '2-1', tip: '초반 방향성을 빠르게 확정할 때 좋습니다.' },
  { score: 84, timing: '3-2', tip: '현재 아이템이 AP 쪽으로 기울었을 때 효율적입니다.' },
  { score: 78, timing: '4-2', tip: '후반 완성 덱의 안정성을 보강합니다.' },
]

function renderSummaryPanel() {
  return `
    <section class="panel ai-summary-panel">
      <div class="panel-header">
        <div>
          <span class="section-label">PROFILE SNAPSHOT</span>
          <h2>SanChess#KR1 플레이 분석</h2>
        </div>
        <span class="summoner-badge"><span class="summoner-dot"></span>연동됨</span>
      </div>
      <div class="ai-summary-stats">
        ${summaryStats.map((stat) => `
          <article class="ai-stat-card">
            <small>${stat.label}</small>
            <strong class="${stat.tone}">${stat.value}</strong>
            <span>${stat.note}</span>
          </article>
        `).join('')}
      </div>
    </section>
  `
}

function renderTraitRow(deck, index) {
  return `
    <article class="trait-row">
      <span class="rank" data-top="${index + 1}">${index + 1}</span>
      <div class="trait-row-main">
        <strong>${esc(deck.name)}</strong>
        <div class="traits">${deck.traits.slice(0, 3).map(renderTrait).join('')}</div>
      </div>
      <div class="trait-row-stats">
        <span class="win">${esc(deck.winRate)}</span>
        <small>TOP4 ${esc(deck.top4)}</small>
      </div>
    </article>
  `
}

function renderDeckPerformance() {
  const strongDecks = decks.slice(0, 3)
  const watchDecks = decks.slice(3, 6)

  return `
    <section class="panel deck-performance-panel">
      <div class="panel-header">
        <div>
          <span class="section-label">DECK PERFORMANCE</span>
          <h2>최근 플레이 성향과 맞는 덱</h2>
        </div>
        <span class="small-pill">패치 17.3</span>
      </div>
      <div class="deck-perf-split">
        <div>
          <div class="deck-perf-col-head">
            <strong>추천 우선순위</strong>
            <span>숙련도와 메타 적합도가 높습니다.</span>
          </div>
          <div class="my-deck-list">${strongDecks.map(renderTraitRow).join('')}</div>
        </div>
        <div>
          <div class="deck-perf-col-head">
            <strong>주의할 선택지</strong>
            <span>아이템/상점 흐름이 맞을 때만 진입하세요.</span>
          </div>
          <div class="my-deck-list caution">${watchDecks.map(renderTraitRow).join('')}</div>
        </div>
      </div>
    </section>
  `
}

function renderAugmentAnalysis() {
  return `
    <section class="panel augment-analysis-panel">
      <div class="panel-header">
        <div>
          <span class="section-label">AUGMENT ANALYSIS</span>
          <h2>증강체 선택 경향</h2>
        </div>
      </div>
      <div class="augment-list">
        ${guideData.augments.map((augment, index) => {
          const score = augmentScores[index % augmentScores.length]
          return `
            <article class="aug-row">
              <div class="aug-row-main">
                <strong>${esc(augment.name)}</strong>
                <p>${esc(augment.summary)}</p>
                ${renderTags(augment.tags ?? [])}
              </div>
              <div class="aug-score">
                <span>${score.score}</span>
                <small>${score.timing}</small>
              </div>
              <div class="aug-bar" aria-hidden="true"><span style="width: ${score.score}%"></span></div>
              <p class="aug-tip">${score.tip}</p>
            </article>
          `
        }).join('')}
      </div>
    </section>
  `
}

function renderAiDeckCard(deck, index) {
  return `
    <article class="ai-deck-card">
      <div class="ai-deck-top">
        <div>
          <span class="badge">AI 추천 #${index + 1}</span>
          <h3>${esc(deck.name)}</h3>
        </div>
        ${renderTier(deck.grade)}
      </div>
      <div class="ai-deck-traits">${deck.traits.slice(0, 4).map(renderTrait).join('')}</div>
      <div class="ai-deck-champions">${deck.champions.slice(0, 6).map(renderChampion).join('')}</div>
      <div class="ai-deck-stats">
        <span><small>승률</small><strong class="win">${esc(deck.winRate)}</strong></span>
        <span><small>TOP4</small><strong class="top4">${esc(deck.top4)}</strong></span>
        <span><small>평균</small><strong class="avg">${esc(deck.avgPlace)}</strong></span>
      </div>
      <p class="ai-reason">최근 플레이가 ${esc(deck.traits[0]?.name ?? '핵심 시너지')} 운영과 잘 맞고, 보유 아이템 방향도 자연스럽게 이어집니다.</p>
      <div class="ai-card-actions">
        <span class="patch-badge">17.3 반영</span>
        <a class="ghost-button" href="../DeckDetail/index.html?id=${deck.rank}">상세 보기</a>
      </div>
    </article>
  `
}

function renderRecommendedDecks() {
  return `
    <section class="panel ai-recommend-panel">
      <div class="panel-header">
        <div>
          <span class="section-label">AI RECOMMENDED DECKS</span>
          <h2>지금 진입하기 좋은 조합</h2>
        </div>
        <a class="primary-button" href="../SummonerDetail/index.html?name=SanChess&tag=KR1">전적 보기</a>
      </div>
      <div class="ai-deck-list">
        ${decks.slice(0, 3).map(renderAiDeckCard).join('')}
      </div>
    </section>
  `
}

export function renderAiRecommend() {
  shell('ai', `
    <div class="page">
      <header class="page-header ai-page-header">
        <div class="title-block">
          <span class="kicker">AI RECOMMEND</span>
          <h1>AI 덱 추천</h1>
          <p>소환사 전적, 최근 덱 성향, 패치 메타를 함께 보고 다음 게임 후보를 추천합니다.</p>
        </div>
        <div class="header-stats">
          <article><span>GAMES</span><strong>20</strong></article>
          <article><span>META</span><strong>17.3</strong></article>
          <article><span>SYNC</span><strong>LIVE</strong></article>
        </div>
      </header>
      <div class="ai-content">
        ${renderSummaryPanel()}
        ${renderDeckPerformance()}
        ${renderAugmentAnalysis()}
        ${renderRecommendedDecks()}
      </div>
    </div>
  `)
}
