import { Route, Routes } from 'react-router-dom'
import AiRecommend from './pages/AiRecommend/AiRecommend'
import AdminLogin from './pages/Admin/AdminLogin'
import AdminDecks from './pages/Admin/AdminDecks'
import AdminHeroAugments from './pages/Admin/AdminHeroAugments'
import AdminGuides from './pages/Admin/AdminGuides'
import AdminPatchNotes from './pages/Admin/AdminPatchNotes'
import AdminMembers from './pages/Admin/AdminMembers'
import AdminCommunity from './pages/Admin/AdminCommunity'
import AdminMatchMonitor from './pages/Admin/AdminMatchMonitor'
import AuthPage from './pages/Auth/AuthPage'
import Decks from './pages/Decks/Decks'
import DeckDetail from './pages/DeckDetail/DeckDetail'
import Dashboard from './pages/Dashboard/Dashboard'
import Guide from './pages/Guide/Guide'
import Party from './pages/Party/Party'
import PatchNotes from './pages/PatchNotes/PatchNotes'
import SummonerDetail from './pages/SummonerDetail/SummonerDetail'
import AdminLayout from './layouts/AdminLayout'
import NotFound from './pages/NotFound/NotFound'

function App() {
  return (
    <Routes>
      {/* 일반 페이지 */}
      <Route path="/" element={<Dashboard />} />
      <Route path="/login" element={<AuthPage mode="login" />} />
      <Route path="/signup" element={<AuthPage mode="signup" />} />
      <Route path="/summoner/:gameName/:tagLine" element={<SummonerDetail />} />
      <Route path="/decks" element={<Decks />} />
      <Route path="/decks/:rankFilter/:deckId" element={<DeckDetail />} />
      <Route path="/ai-recommend" element={<AiRecommend />} />
      <Route path="/guide" element={<Guide />} />
      <Route path="/party" element={<Party />} />
      <Route path="/patch-notes" element={<PatchNotes />} />

      {/* 관리자 로그인 (레이아웃 없음) */}
      <Route path="/admin" element={<AdminLogin />} />

      {/* 관리자 전용 레이아웃 (중첩 라우트) */}
      <Route element={<AdminLayout />}>
        <Route path="/admin/decks" element={<AdminDecks />} />
        <Route path="/admin/hero-augments" element={<AdminHeroAugments />} />
        <Route path="/admin/guides" element={<AdminGuides />} />
        <Route path="/admin/patch-notes" element={<AdminPatchNotes />} />
        <Route path="/admin/members" element={<AdminMembers />} />
        <Route path="/admin/community" element={<AdminCommunity />} />
        <Route path="/admin/match-monitor" element={<AdminMatchMonitor />} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}

export default App
