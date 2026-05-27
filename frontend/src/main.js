import { renderAiRecommend } from './pages/AiRecommend/AiRecommendPage.js'
import { renderAuth } from './pages/Auth/AuthPage.js'
import { renderDeckDetail } from './pages/DeckDetail/DeckDetailPage.js'
import { renderDecks } from './pages/Decks/DecksPage.js'
import { renderDashboard } from './pages/Dashboard/DashboardPage.js'
import { renderGuide } from './pages/Guide/GuidePage.js'
import { renderMetaStats } from './pages/MetaStats/MetaStatsPage.js'
import { renderPatchNotes } from './pages/PatchNotes/PatchNotesPage.js'
import { renderParty } from './pages/Party/PartyPage.js'
import { renderSummonerDetail } from './pages/SummonerDetail/SummonerDetailPage.js'

const pageRenderers = {
  dashboard: renderDashboard,
  auth: renderAuth,
  decks: renderDecks,
  'deck-detail': renderDeckDetail,
  'ai-recommend': renderAiRecommend,
  guide: renderGuide,
  'meta-stats': renderMetaStats,
  party: renderParty,
  'patch-notes': renderPatchNotes,
  'summoner-detail': renderSummonerDetail,
}

function boot() {
  const page = document.body.dataset.page
  pageRenderers[page]?.()
}

boot()
