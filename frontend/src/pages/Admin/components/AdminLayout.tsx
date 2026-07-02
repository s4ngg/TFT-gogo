import { Navigate, Outlet } from 'react-router-dom'
import { useAdminAuth } from '../../../hooks/useAdminAuth'
import AdminSidebar from './AdminSidebar'
import styles from './AdminLayout.module.css'

function AdminLayout() {
  const status = useAdminAuth()

  if (status === 'loading') {
    return null
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
