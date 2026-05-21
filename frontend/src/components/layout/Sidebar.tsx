import {
  Bookmark,
  Box,
  ClipboardList,
  Home,
  MessageCircle,
  RefreshCcw,
  Users,
} from 'lucide-react'
import { NavLink, useLocation } from 'react-router-dom'
import { navItems, type NavItem } from './layoutData'
import styles from './Layout.module.css'

const navIcons: Record<NavItem['key'], typeof Home> = {
  decks: Home,
  ai: Box,
  guide: Bookmark,
  party: Users,
  patch: ClipboardList,
}

function BrandLogo() {
  return <div className={styles.brandLogo} aria-hidden="true" />
}

function Sidebar() {
  const currentLocation = useLocation()

  return (
    <aside className={styles.sidebar}>
      <div className={styles.brand}>
        <BrandLogo />
        <strong>TFTgogo</strong>
      </div>

      <nav className={styles.navList} aria-label="메인 메뉴">
        {navItems.map((item, index) => {
          const Icon = navIcons[item.key]

          return (
            <NavLink
              className={({ isActive }) =>
                `${styles.navItem} ${
                  isActive || (index === 0 && currentLocation.pathname === '/') ? styles.activeNav : ''
                }`
              }
              to={item.path}
              key={item.key}
            >
              <Icon size={25} strokeWidth={2.2} />
              <span>{item.label}</span>
            </NavLink>
          )
        })}
      </nav>

      <div className={styles.sidebarFill} />

      <section className={styles.rankCard} aria-label="내 랭크 요약">
        <div className={styles.rankEmblem} />
        <div className={styles.rankName}>
          <strong>플래티넘 II</strong>
          <span className={styles.rankPoint}>45LP</span>
        </div>
        <div className={styles.rankRecord}>
          <span>123 승</span>
          <span>98 패 (55.6%)</span>
        </div>
        <div className={styles.rankProgress}>
          <span />
        </div>
        <button type="button" className={styles.secondaryButton}>
          <RefreshCcw size={19} />
          전적 업데이트
        </button>
      </section>

      <button className={styles.feedbackButton} type="button">
        <MessageCircle size={21} />
        의견 보내기
      </button>
    </aside>
  )
}

export default Sidebar
