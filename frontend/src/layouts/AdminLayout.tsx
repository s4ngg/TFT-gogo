import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import AdminSidebar from '../components/admin/AdminSidebar'
import { getAdminToken } from '../api/adminApi'
import styles from './AdminLayout.module.css'

interface AdminLayoutProps {
  children: ReactNode
}

function AdminLayout({ children }: AdminLayoutProps) {
  if (!getAdminToken()) {
    return <Navigate to="/admin" replace />
  }

  return (
    <div className={styles.shell}>
      <AdminSidebar />
      <main className={styles.content}>
        {children}
      </main>
    </div>
  )
}

export default AdminLayout
