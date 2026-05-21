import { AppLayout } from '../../components/layout'
import AiDeckRecommend from './components/AiDeckRecommend'
import LiveChat from './components/LiveChat'
import MetaSnapshot from './components/MetaSnapshot'
import PartyFinderCard from './components/PartyFinderCard'
import TopSummaryCards from './components/TopSummaryCards'
import styles from './Dashboard.module.css'

function Dashboard() {
  return (
    <AppLayout>
      <div className={styles.dashboardGrid}>
        <TopSummaryCards />
        <MetaSnapshot />
        <aside className={styles.rightColumn}>
          <PartyFinderCard />
          <LiveChat />
          <AiDeckRecommend />
        </aside>
      </div>
    </AppLayout>
  )
}

export default Dashboard
