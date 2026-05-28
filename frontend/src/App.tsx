import { Navigate, Route, Routes } from 'react-router-dom'
import AiRecommend from './pages/AiRecommend/AiRecommend'
import Decks from './pages/Decks/Decks'
import Dashboard from './pages/Dashboard/Dashboard'
import Guide from './pages/Guide/Guide'
import Community from './pages/Community/Community'
import PatchNotes from './pages/PatchNotes/PatchNotes'
import SummonerDetail from './pages/SummonerDetail/SummonerDetail'
import AdminPage from './pages/Admin/AdminPage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/summoner/:gameName/:tagLine" element={<SummonerDetail />} />
      <Route path="/decks" element={<Decks />} />
      <Route path="/ai-recommend" element={<AiRecommend />} />
      <Route path="/guide" element={<Guide />} />
      <Route path="/community" element={<Community />} />
      <Route path="/patch-notes" element={<PatchNotes />} />
      <Route path="/adminPage" element={<AdminPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App

