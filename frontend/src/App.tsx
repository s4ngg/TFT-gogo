import { Suspense, lazy } from 'react'
import { Route, Routes } from 'react-router-dom'
import Dashboard from './pages/Dashboard/Dashboard'
import styles from './App.module.css'

const AiRecommend = lazy(() => import('./pages/AiRecommend/AiRecommend'))
const AdminLogin = lazy(() => import('./pages/Admin/AdminLogin'))
const AdminDecks = lazy(() => import('./pages/Admin/AdminDecks'))
const AdminHeroAugments = lazy(() => import('./pages/Admin/AdminHeroAugments'))
const AdminGuides = lazy(() => import('./pages/Admin/AdminGuides'))
const AdminPatchNotes = lazy(() => import('./pages/Admin/AdminPatchNotes'))
const AdminMembers = lazy(() => import('./pages/Admin/AdminMembers'))
const AdminCommunity = lazy(() => import('./pages/Admin/AdminCommunity'))
const AdminMatchMonitor = lazy(() => import('./pages/Admin/AdminMatchMonitor'))
const AuthPage = lazy(() => import('./pages/Auth/AuthPage'))
const OAuthCallbackPage = lazy(() => import('./pages/Auth/OAuthCallbackPage'))
const Decks = lazy(() => import('./pages/Decks/Decks'))
const DeckDetail = lazy(() => import('./pages/DeckDetail/DeckDetail'))
const Guide = lazy(() => import('./pages/Guide/Guide'))
const Party = lazy(() => import('./pages/Party/Party'))
const PatchNotes = lazy(() => import('./pages/PatchNotes/PatchNotes'))
const SearchDetail = lazy(() => import('./pages/SearchDetail/SearchDetail'))
const AdminLayout = lazy(() => import('./pages/Admin/components/AdminLayout'))
const NotFound = lazy(() => import('./pages/NotFound/NotFound'))

const routeFallback = <div className={styles.routeFallback}>불러오는 중...</div>

function App() {
  return (
    <Suspense fallback={routeFallback}>
      <Routes>
        {/* 일반 페이지 */}
        <Route path="/" element={<Dashboard />} />
        <Route path="/login" element={<AuthPage mode="login" />} />
        <Route path="/signup" element={<AuthPage mode="signup" />} />
        <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
        <Route path="/summoner/:gameName/:tagLine" element={<SearchDetail />} />
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
    </Suspense>
  )
}

export default App
