import { useState } from 'react'
import { AppLayout } from '../../components/layout'
import { getAdminToken } from '../../api/adminApi'
import AdminPage from './components/AdminPage'
import TokenGate from './components/TokenGate'

function Admin() {
  const [authed, setAuthed] = useState(() => getAdminToken() !== '')

  if (!authed) {
    return (
      <AppLayout>
        <TokenGate onSuccess={() => setAuthed(true)} />
      </AppLayout>
    )
  }

  return (
    <AppLayout>
      <AdminPage />
    </AppLayout>
  )
}

export default Admin
