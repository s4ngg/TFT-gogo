import { shell } from '../../components/Header/Header.js'
import { renderTier } from '../../components/common/renderers.js'
import { decks } from '../../data/staticData.js'
import { state } from '../../store/staticState.js'
import { bindDeckEvents, renderDeckTable } from '../Decks/DecksPage.js'

export function renderMetaStats() {
  state.deckTab = 'meta'
  shell('meta', `
    <div class="page">
      <header class="page-header"><div><span class="kicker">META STATS</span><h1>메타 통계</h1><p>정적 HTML 기준의 티어별 메타 통계입니다.</p></div></header>
      ${['S', 'A+', 'A', 'B', 'C'].map((tier) => {
        const tierDecks = decks.filter((deck) => deck.grade === tier)
        return tierDecks.length ? `<section class="panel"><div class="panel-header"><h2>${renderTier(tier)} ${tier} 티어</h2><span class="small-pill">${tierDecks.length}개</span></div>${renderDeckTable(tierDecks)}</section>` : ''
      }).join('')}
    </div>
  `)
  bindDeckEvents(renderMetaStats)
}
