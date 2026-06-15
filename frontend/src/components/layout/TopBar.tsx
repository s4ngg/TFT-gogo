import { Bell, ChevronDown, CircleHelp, LogIn, Mail } from 'lucide-react'
import { Link } from 'react-router-dom'
import { communityDragonProfileIconUrl } from '../../api/communityDragonAssets'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import useAuthStore from '../../store/useAuthStore'
import RiotApiStatusBadge from './RiotApiStatusBadge'
import styles from './Layout.module.css'

const profileIconUrl = communityDragonProfileIconUrl(588)

function TopBar() {
  const user = useAuthStore((state) => state.user)
  const isLoggedIn = Boolean(user)
  const { data: metaDeckResponse } = useMetaSnapshot()
  const patchVersion = metaDeckResponse?.patchVersion ?? '집계 대기'

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
          <button type="button" className={styles.profileButton}>
            <span>
              <img src={profileIconUrl} alt="" />
            </span>
            TFTgogo
            <ChevronDown size={14} />
          </button>
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
