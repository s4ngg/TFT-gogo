import { useEffect, useId, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Bell, ChevronDown, CircleHelp, LogIn, LogOut, Mail } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { communityDragonProfileIconUrl } from '../../api/communityDragonAssets'
import { logout } from '../../api/memberApi'
import { useAuthSession } from '../../hooks/useAuthSession'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import useAuthStore from '../../store/useAuthStore'
import RiotApiStatusBadge from './RiotApiStatusBadge'
import { clearTopBarAuthSession } from './topBarAuth'
import styles from './Layout.module.css'

const profileIconUrl = communityDragonProfileIconUrl(588)

function TopBar() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const profileMenuId = useId()
  const profileMenuRef = useRef<HTMLDivElement>(null)
  const profileTriggerRef = useRef<HTMLButtonElement>(null)
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false)
  const token = useAuthStore((state) => state.token)
  const isLoggedIn = Boolean(token)
  const { data: member } = useAuthSession()
  const { data: metaDeckResponse } = useMetaSnapshot()
  const patchVersion = metaDeckResponse?.patchVersion ?? '집계 대기'
  const displayName = member?.nickname ?? member?.email ?? 'TFTgogo'
  const memberProfileIconUrl = member?.profileImage || profileIconUrl

  useEffect(() => {
    if (!isLoggedIn) {
      setIsProfileMenuOpen(false)
    }
  }, [isLoggedIn])

  useEffect(() => {
    if (!isProfileMenuOpen) {
      return undefined
    }

    function handlePointerDown(event: PointerEvent) {
      const target = event.target

      if (!(target instanceof Node)) {
        return
      }

      if (profileTriggerRef.current?.contains(target) || profileMenuRef.current?.contains(target)) {
        return
      }

      setIsProfileMenuOpen(false)
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key !== 'Escape') {
        return
      }

      setIsProfileMenuOpen(false)
      profileTriggerRef.current?.focus()
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)

    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [isProfileMenuOpen])

  function handleLogout() {
    const authState = useAuthStore.getState()
    void clearTopBarAuthSession(queryClient, authState.clearAuth, logout, authState.token)
    setIsProfileMenuOpen(false)
    navigate('/', { replace: true })
  }

  return (
    <header className={styles.topBar}>
      <div className={styles.topStatusGroup}>
        <div className={styles.patchBrief} aria-label="패치 한줄 요약">
          <span>{patchVersion} 패치 요약</span>
          <strong>상위권 선봉대 벡스와 6악복 중심으로 압축 중</strong>
        </div>
        <RiotApiStatusBadge />
      </div>
      <div className={styles.topActions}>
        {isLoggedIn && (
          <>
            <button type="button" className={styles.notificationButton} aria-label="알림">
              <Bell size={23} />
              <span>3</span>
            </button>
            <button type="button" className={styles.iconButton} aria-label="메일">
              <Mail size={24} />
            </button>
          </>
        )}
        <button type="button" className={styles.iconButton} aria-label="도움말">
          <CircleHelp size={24} />
        </button>
        {isLoggedIn ? (
          <div className={styles.profileMenuShell}>
            <button
              ref={profileTriggerRef}
              type="button"
              className={styles.profileButton}
              aria-controls={profileMenuId}
              aria-expanded={isProfileMenuOpen}
              aria-haspopup="true"
              onClick={() => setIsProfileMenuOpen((open) => !open)}
            >
              <span className={styles.profileAvatar}>
                <img src={memberProfileIconUrl} alt="" />
              </span>
              <span className={styles.profileName}>{displayName}</span>
              <ChevronDown className={styles.profileChevron} size={14} aria-hidden="true" />
            </button>
            {isProfileMenuOpen && (
              <div
                ref={profileMenuRef}
                id={profileMenuId}
                className={styles.profileMenu}
                aria-label="사용자 메뉴"
              >
                <div className={styles.profileMenuName}>{displayName}</div>
                <button type="button" className={styles.profileMenuDangerButton} onClick={handleLogout}>
                  <LogOut size={16} aria-hidden="true" />
                  로그아웃
                </button>
              </div>
            )}
          </div>
        ) : (
          <Link className={styles.loginButton} to="/login">
            <LogIn size={17} />
            로그인
          </Link>
        )}
      </div>
    </header>
  )
}

export default TopBar
