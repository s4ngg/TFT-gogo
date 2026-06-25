import { Navigate, Outlet } from 'react-router-dom'
import AdminSidebar from '../components/admin/AdminSidebar'
import { getAccessToken } from '../api/adminApi'
import styles from './AdminLayout.module.css'

function AdminLayout() {
  if (!getAccessToken()) {
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
