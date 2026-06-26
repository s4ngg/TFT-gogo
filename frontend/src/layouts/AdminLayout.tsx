import { Navigate, Outlet } from 'react-router-dom'
import AdminSidebar from '../components/admin/AdminSidebar'
import { useAdminAuth } from '../hooks/useAdminAuth'
import styles from './AdminLayout.module.css'

function AdminLayout() {
  const status = useAdminAuth()

  if (status === 'loading') {
    return null  // refresh 시도 중 — 빈 화면 (깜빡임 방지)
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/admin" replace />
  }

  return (
    <div className={styles.shell}>
      <AdminSidebar />
      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  )
}

export default AdminLayout
