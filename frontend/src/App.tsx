import { Navigate, Route, Routes } from 'react-router-dom'
import Dashboard from './pages/Dashboard/Dashboard'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/decks" element={<Dashboard />} />
      <Route path="/ai-recommend" element={<Dashboard />} />
      <Route path="/guide" element={<Dashboard />} />
      <Route path="/party" element={<Dashboard />} />
      <Route path="/patch-notes" element={<Dashboard />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
