import { NavLink, useNavigate } from 'react-router-dom'
import { LayoutGrid, Sword, BookOpen, FileText, Users, MessageSquare, Activity, LogOut } from 'lucide-react'
import { clearAdminToken } from '../../api/adminApi'
import styles from './AdminSidebar.module.css'

const NAV_ITEMS = [
  { path: '/admin/decks',          label: '메타덱 관리',      icon: LayoutGrid,    ready: true  },
  { path: '/admin/hero-augments',  label: '영웅증강 덱 관리', icon: Sword,         ready: true  },
  { path: '/admin/guides',         label: '게임가이드 관리',  icon: BookOpen,      ready: true  },
  { path: '/admin/match-monitor',  label: '전적 모니터링',    icon: Activity,      ready: true  },
  { path: '/admin/patch-notes',    label: '패치노트 관리',    icon: FileText,      ready: true  },
  { path: '/admin/members',        label: '회원 관리',        icon: Users,         ready: false },
  { path: '/admin/community',      label: '커뮤니티 관리',    icon: MessageSquare, ready: false },
]

function AdminSidebar() {
  const navigate = useNavigate()

  function handleLogout() {
    clearAdminToken()
    navigate('/admin')
  }

  return (
    <aside className={styles.sidebar}>
      <div className={styles.brand}>
        <div className={styles.brandLogo} />
        <strong>관리자</strong>
      </div>

      <nav className={styles.navList}>
        {NAV_ITEMS.map(({ path, label, icon: Icon, ready }) =>
          ready ? (
            <NavLink
              key={path}
              to={path}
              className={({ isActive }) =>
                `${styles.navItem} ${isActive ? styles.active : ''}`
              }
            >
              <Icon size={20} strokeWidth={2} />
              <span>{label}</span>
            </NavLink>
          ) : (
            <div
              key={path}
              className={`${styles.navItem} ${styles.disabled}`}
              aria-disabled="true"
            >
              <Icon size={20} strokeWidth={2} />
              <span>{label}</span>
              <span className={styles.badge}>준비 중</span>
            </div>
          )
        )}
      </nav>

      <div className={styles.fill} />

      <button className={styles.logoutBtn} onClick={handleLogout}>
        <LogOut size={16} strokeWidth={2} />
        로그아웃
      </button>
    </aside>
  )
}

export default AdminSidebar
