import { useState } from 'react'
import { AppLayout } from '../../components/layout'
import { getAccessToken } from '../../api/adminApi'
import AdminPage from './components/AdminPage'
import TokenGate from './components/TokenGate'

function Admin() {
  const [authed, setAuthed] = useState(() => getAccessToken() !== null)

  if (!authed) {
    return (
      <AppLayout sunTheme>
        <TokenGate onSuccess={() => setAuthed(true)} />
      </AppLayout>
    )
  }

  return (
    <AppLayout sunTheme>
      <AdminPage />
    </AppLayout>
  )
}

export default Admin
