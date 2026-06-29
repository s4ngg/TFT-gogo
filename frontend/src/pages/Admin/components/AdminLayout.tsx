import { Navigate, Outlet } from 'react-router-dom'
<<<<<<<< HEAD:frontend/src/pages/Admin/components/AdminLayout.tsx
import AdminSidebar from './AdminSidebar'
import { useAdminAuth } from '../../../hooks/useAdminAuth'
========
import AdminSidebar from '../admin/AdminSidebar'
import { useAdminAuth } from '../../hooks/useAdminAuth'
>>>>>>>> 07c0792e (refactor: 도메인 패키지 구조 통일 · summoner→search 이관):frontend/src/components/layout/AdminLayout.tsx
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
