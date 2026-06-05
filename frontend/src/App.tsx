import { Navigate, Route, Routes } from 'react-router-dom'
import AiRecommend from './pages/AiRecommend/AiRecommend'
import Admin from './pages/Admin/Admin'
import AuthPage from './pages/Auth/AuthPage'
import Decks from './pages/Decks/Decks'
import DeckDetail from './pages/DeckDetail/DeckDetail'
import Dashboard from './pages/Dashboard/Dashboard'
import Guide from './pages/Guide/Guide'
import Party from './pages/Party/Party'
import PatchNotes from './pages/PatchNotes/PatchNotes'
import SummonerDetail from './pages/SummonerDetail/SummonerDetail'

function App() {
  return (
    <Routes>
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
      <Route path="/admin" element={<Admin />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App

