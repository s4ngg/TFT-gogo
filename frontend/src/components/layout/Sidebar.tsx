import {
  Bookmark,
  Bot,
  ClipboardList,
  Home,
  LayoutGrid,
  Users,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { navItems, type NavItem } from './layoutData'
import styles from './Layout.module.css'

const navIcons: Record<NavItem['key'], typeof Home> = {
  home: Home,
  decks: LayoutGrid,
  ai: Bot,
  guide: Bookmark,
  party: Users,
  patch: ClipboardList,
}

function BrandLogo() {
  return <div className={styles.brandLogo} aria-hidden="true" />
}

function Sidebar() {
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
    </aside>
  )
}

export default Sidebar
