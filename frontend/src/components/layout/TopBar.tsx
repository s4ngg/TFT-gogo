import { useEffect, useId, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ChevronDown, LogIn, LogOut } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { communityDragonProfileIconUrl } from '../../api/communityDragonAssets'
import { logout } from '../../api/memberApi'
import { useAuthSession } from '../../hooks/useAuthSession'
import { useLatestPatchNote } from '../../pages/Decks/hooks/useLatestPatchVersion'
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
  const latestPatch = useLatestPatchNote()
  const patchBriefText = latestPatch ? `${latestPatch.version} 패치 요약` : '패치 요약'
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
          <span>{patchBriefText}</span>
        </div>
        <RiotApiStatusBadge />
      </div>
      <div className={styles.topActions}>
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
