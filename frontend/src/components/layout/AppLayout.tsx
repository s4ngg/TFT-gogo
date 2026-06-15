import type { ReactNode } from 'react'
import useLayoutStore from '../../store/useLayoutStore'
import Sidebar from './Sidebar'
import TopBar from './TopBar'
import styles from './Layout.module.css'

interface AppLayoutProps {
  children: ReactNode
}

function AppLayout({ children }: AppLayoutProps) {
  const isSidebarCollapsed = useLayoutStore((state) => state.isSidebarCollapsed)
  const toggleSidebarCollapsed = useLayoutStore((state) => state.toggleSidebarCollapsed)

  return (
    <div className={`${styles.appShell} ${isSidebarCollapsed ? styles.sidebarCollapsed : ''}`}>
      <Sidebar
        isCollapsed={isSidebarCollapsed}
        onToggleCollapsed={toggleSidebarCollapsed}
      />
      <main className={styles.main}>
        <TopBar />
        {children}
      </main>
    </div>
  )
}

export default AppLayout
