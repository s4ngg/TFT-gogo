import { Navigate, Route, Routes } from 'react-router-dom'
import AiRecommend from './pages/AiRecommend/AiRecommend'
import Decks from './pages/Decks/Decks'
import Dashboard from './pages/Dashboard/Dashboard'
import Guide from './pages/Guide/Guide'
import MetaStats from './pages/MetaStats/MetaStats'
import Party from './pages/Party/Party'
import PatchNotes from './pages/PatchNotes/PatchNotes'
import SummonerPage from './pages/Summoner/SummonerPage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/summoner/:summonerName/:tagLine" element={<SummonerPage />} />
      <Route path="/decks" element={<Decks />} />
      <Route path="/meta-stats" element={<MetaStats />} />
      <Route path="/ai-recommend" element={<AiRecommend />} />
      <Route path="/guide" element={<Guide />} />
      <Route path="/party" element={<Party />} />
      <Route path="/patch-notes" element={<PatchNotes />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App

