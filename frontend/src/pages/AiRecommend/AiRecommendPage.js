import { shell } from '../../components/Header/Header.js'
import { esc, renderChampion, renderTags, renderTier, renderTrait } from '../../components/common/renderers.js'
import { decks } from '../../data/staticData.js'

const summaryStats = [
  { label: '평균 등수', value: '4.1', tone: 'avg', note: '최근 20게임' },
  { label: 'TOP4', value: '58.0%', tone: 'top4', note: '상위권 안정성' },
  { label: '1등 비율', value: '22.5%', tone: 'pick', note: '고점 확보력' },
  { label: '추천 신뢰도', value: '86', tone: 'win', note: '패치 메타 반영' },
]

const aiAugmentStats = [
  { name: '강철의 의지', icon: '방', avgPlace: '2.9', games: 4, score: 92, tags: ['탱커', '연승'], tip: '앞라인 중심 조합에서 가장 좋은 성적을 냈습니다.' },
  { name: '정의의 손길+', icon: '검', avgPlace: '3.2', games: 3, score: 88, tags: ['AD', '캐리'], tip: 'AD 캐리 아이템이 빠르게 잡힌 판에 효율적입니다.' },
  { name: '용의 불꽃', icon: '화', avgPlace: '3.5', games: 5, score: 82, tags: ['AP', '광역'], tip: '마법사 전환이 가능한 판에서 안정적인 선택입니다.' },
  { name: '별의 수호자', icon: '별', avgPlace: '4.8', games: 3, score: 68, tags: ['유틸', '후반'], tip: '후반 보강용으로는 좋지만 초반 방향성이 약합니다.' },
  { name: '전사의 용기', icon: '칼', avgPlace: '5.1', games: 4, score: 61, tags: ['근접', '리스크'], tip: '근접 캐리 의존도가 높아 아이템 조건을 많이 탑니다.' },
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
  const best = aiAugmentStats[0]

  return `
    <section class="panel augment-analysis-panel">
      <div class="panel-header">
        <div>
          <span class="section-label">AUGMENT ANALYSIS</span>
          <h2>증강 성적 분석</h2>
        </div>
        <span class="small-pill">평균 등수 낮을수록 좋음</span>
      </div>
      <div class="augment-list">
        ${aiAugmentStats.map((augment, index) => {
          const tone = index === 0 ? 'best' : index === aiAugmentStats.length - 1 ? 'worst' : 'default'
          return `
            <article class="aug-row" data-tone="${tone}">
              <span class="aug-rank">${index + 1}</span>
              <span class="aug-icon">${esc(augment.icon)}</span>
              <div class="aug-row-main">
                <strong>${esc(augment.name)}</strong>
                <p>${esc(augment.tip)}</p>
              </div>
              <div class="aug-progress" aria-hidden="true"><span style="width: ${augment.score}%"></span></div>
              <span class="aug-place">${esc(augment.avgPlace)}등</span>
              <span class="aug-games">${augment.games}게임</span>
              <div class="aug-tags">${renderTags(augment.tags)}</div>
            </article>
          `
        }).join('')}
      </div>
      <p class="aug-tip">추천: <b>${esc(best.name)}</b> 증강을 선택했을 때 평균 ${esc(best.avgPlace)}등으로 가장 좋은 성적을 냈습니다.</p>
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
      <div class="ai-deck-champions">${deck.champions.slice(0, 5).map(renderChampion).join('')}</div>
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
