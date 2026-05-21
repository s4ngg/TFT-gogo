import { Bell, ChevronDown, CircleHelp, Mail } from 'lucide-react'
import { communityDragonProfileIconUrl } from '../../api/communityDragonAssets'
import styles from './Layout.module.css'

const profileIconUrl = communityDragonProfileIconUrl(588)

function TopBar() {
  return (
    <header className={styles.topBar}>
      <div className={styles.patchBrief} aria-label="패치 한줄 요약">
        <span>17.3 패치 요약</span>
        <strong>상위권은 선봉대 벡스와 6암흑의 별 진 중심으로 압축 중</strong>
      </div>
      <div className={styles.topActions}>
        <button type="button" className={styles.notificationButton} aria-label="알림">
          <Bell size={23} />
          <span>3</span>
        </button>
        <button type="button" className={styles.iconButton} aria-label="메일">
          <Mail size={24} />
        </button>
        <button type="button" className={styles.iconButton} aria-label="도움말">
          <CircleHelp size={24} />
        </button>
        <button type="button" className={styles.profileButton}>
          <span>
            <img src={profileIconUrl} alt="" />
          </span>
          TFTgogo
          <ChevronDown size={14} />
        </button>
      </div>
    </header>
  )
}

export default TopBar
