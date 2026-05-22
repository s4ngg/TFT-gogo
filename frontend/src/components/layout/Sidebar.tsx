import {
  Bookmark,
  Bot,
  ClipboardList,
  Home,
  LayoutGrid,
  LineChart,
  RefreshCcw,
  Users,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'
import useSummonerStore from '../../store/useSummonerStore'
import { navItems, type NavItem } from './layoutData'
import styles from './Layout.module.css'

const navIcons: Record<NavItem['key'], typeof Home> = {
  home: Home,
  decks: LayoutGrid,
  meta: LineChart,
  ai: Bot,
  guide: Bookmark,
  party: Users,
  patch: ClipboardList,
}

function BrandLogo() {
  return <div className={styles.brandLogo} aria-hidden="true" />
}

function Sidebar() {
  const summoner = useSummonerStore((s) => s.summoner)

  return (
    <aside className={styles.sidebar}>
      <NavLink to="/" className={styles.brand}>
        <BrandLogo />
        <strong>TFTgogo</strong>
      </NavLink>

      <nav className={styles.navList} aria-label="메인 메뉴">
        {navItems.map((item) => {
          const Icon = navIcons[item.key]

          return (
            <NavLink
              className={({ isActive }) =>
                `${styles.navItem} ${isActive ? styles.activeNav : ''}`
              }
              to={item.path}
              end={item.path === '/'}
              key={item.key}
            >
              <Icon size={25} strokeWidth={2.2} />
              <span>{item.label}</span>
            </NavLink>
          )
        })}
      </nav>

      <div className={styles.sidebarFill} />

      {summoner && (
        <section className={styles.rankCard} aria-label="내 랭크 요약">
          <div className={styles.rankEmblem} />
          <div className={styles.rankName}>
            <strong>{summoner.name}</strong>
            <span className={styles.rankPoint}>{summoner.tier} {summoner.lp}LP</span>
          </div>
          <div className={styles.rankRecord}>
            <span>{summoner.wins} 승</span>
            <span>{summoner.losses} 패 ({Math.round(summoner.wins / (summoner.wins + summoner.losses) * 100)}%)</span>
          </div>
          <div className={styles.rankProgress}>
            <span />
          </div>
          <button type="button" className={styles.secondaryButton}>
            <RefreshCcw size={19} />
            전적 업데이트
          </button>
        </section>
      )}
    </aside>
  )
}

export default Sidebar
