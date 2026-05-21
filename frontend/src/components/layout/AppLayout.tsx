import type { ReactNode } from 'react'
import Sidebar from './Sidebar'
import TopBar from './TopBar'
import styles from './Layout.module.css'

interface AppLayoutProps {
  children: ReactNode
}

function AppLayout({ children }: AppLayoutProps) {
  return (
    <div className={styles.appShell}>
      <Sidebar />
      <main className={styles.main}>
        <TopBar />
        {children}
      </main>
    </div>
  )
}

export default AppLayout
