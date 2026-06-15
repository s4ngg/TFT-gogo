import {
  Bookmark,
  Bot,
  ClipboardList,
  Home,
  LayoutGrid,
  PanelLeftClose,
  PanelLeftOpen,
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

interface SidebarProps {
  isCollapsed: boolean
  onToggleCollapsed: () => void
}

function Sidebar({ isCollapsed, onToggleCollapsed }: SidebarProps) {
  const ToggleIcon = isCollapsed ? PanelLeftOpen : PanelLeftClose

  return (
    <aside className={styles.sidebar}>
      <div className={styles.sidebarHeader}>
        <NavLink to="/" className={styles.brand} aria-label="TFTgogo 홈">
          <BrandLogo />
          <strong>TFTgogo</strong>
        </NavLink>
        <button
          type="button"
          className={styles.sidebarToggle}
          aria-label={isCollapsed ? '사이드바 펼치기' : '사이드바 접기'}
          aria-expanded={!isCollapsed}
          onClick={onToggleCollapsed}
        >
          <ToggleIcon size={18} />
        </button>
      </div>

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
              aria-label={item.label}
              title={isCollapsed ? item.label : undefined}
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
