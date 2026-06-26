import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { LayoutGrid, Sword, BookOpen, FileText, Users, MessageSquare, Activity, LogOut } from 'lucide-react'
import { clearAccessToken, adminLogout, getHttpStatus } from '../../api/adminApi'
import { useAdminSession } from '../../hooks/useAdminSession'
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
  const { session, setSession } = useAdminSession()
  const [logoutError, setLogoutError] = useState<string | null>(null)

  async function handleLogout() {
    setLogoutError(null)
    try {
      await adminLogout()
      clearAccessToken()
      setSession(null)
      navigate('/admin')
    } catch (err: unknown) {
      const status = getHttpStatus(err)
      // 세션이 이미 무효한 경우(401/403)는 로컬 상태 정리 후 이동
      if (status === 401 || status === 403) {
        clearAccessToken()
        setSession(null)
        navigate('/admin')
      } else {
        // 네트워크/5xx 오류: 서버 쿠키가 만료되지 않을 수 있으므로 경고만 표시
        setLogoutError('로그아웃 중 오류가 발생했습니다. 다시 시도해주세요.')
      }
    }
  }

  return (
    <aside className={styles.sidebar}>
      <div className={styles.brand}>
        <div className={styles.brandLogo} />
        <strong>관리자</strong>
        {session && (
          <span className={styles.brandUser}>{session.username}</span>
        )}
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

      {logoutError && (
        <div className={styles.logoutError}>{logoutError}</div>
      )}

      <button className={styles.logoutBtn} onClick={handleLogout}>
        <LogOut size={16} strokeWidth={2} />
        로그아웃
      </button>
    </aside>
  )
}

export default AdminSidebar
