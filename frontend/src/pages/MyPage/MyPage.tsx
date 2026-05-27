import { Bell, LogOut, Mail, MessageCircle, ShieldCheck, UserRound } from 'lucide-react'
import { Link, Navigate } from 'react-router-dom'
import { communityDragonProfileIconUrl } from '../../api/communityDragonAssets'
import { AppLayout } from '../../components/layout'
import useAuthStore from '../../store/useAuthStore'
import styles from './MyPage.module.css'

const profileIconUrl = communityDragonProfileIconUrl(588)

const profileStats = [
  { label: '최근 알림', value: '3' },
  { label: '안 읽은 메시지', value: '2' },
  { label: '저장한 덱', value: '12' },
]

const recentActivities = [
  '선봉대 벡스 추천 메타를 저장했습니다.',
  '다이아 구간 야부/연습 파티에 관심 표시했습니다.',
  '17.3 패치 요약 리포트를 확인했습니다.',
]

function MyPage() {
  const user = useAuthStore((state) => state.user)
  const clearUser = useAuthStore((state) => state.clearUser)

  if (!user) {
    return <Navigate to="/login" replace />
  }

  const displayName = user.summonerTag?.split('#')[0] ?? user.email.split('@')[0]

  return (
    <AppLayout>
      <div className={styles.myPage}>
        <header className={styles.profileHero}>
          <div className={styles.profileIdentity}>
            <span className={styles.profileIcon}>
              <img src={profileIconUrl} alt="" />
            </span>
            <div>
              <p>My Page</p>
              <h1>{displayName}</h1>
              <span>{user.summonerTag ?? user.email}</span>
            </div>
          </div>
          <button type="button" className={styles.logoutButton} onClick={clearUser}>
            <LogOut size={18} />
            로그아웃
          </button>
        </header>

        <section className={styles.statGrid} aria-label="내 활동 요약">
          {profileStats.map((stat) => (
            <article key={stat.label}>
              <strong>{stat.value}</strong>
              <span>{stat.label}</span>
            </article>
          ))}
        </section>

        <div className={styles.contentGrid}>
          <section className={styles.panel}>
            <div className={styles.panelHeading}>
              <UserRound size={20} />
              <h2>계정 정보</h2>
            </div>
            <dl className={styles.infoList}>
              <div>
                <dt>이메일</dt>
                <dd>{user.email}</dd>
              </div>
              <div>
                <dt>소환사명</dt>
                <dd>{user.summonerTag ?? '아직 연결되지 않음'}</dd>
              </div>
              <div>
                <dt>보안 상태</dt>
                <dd>
                  <ShieldCheck size={16} />
                  기본 인증 사용 중
                </dd>
              </div>
            </dl>
          </section>

          <section className={styles.panel}>
            <div className={styles.panelHeading}>
              <Mail size={20} />
              <h2>메시지 바로가기</h2>
            </div>
            <div className={styles.quickLinks}>
              <Link to="/party">
                <MessageCircle size={18} />
                메시지함 열기
              </Link>
              <Link to="/">
                <Bell size={18} />
                최근 알림 확인
              </Link>
            </div>
          </section>

          <section className={`${styles.panel} ${styles.activityPanel}`}>
            <div className={styles.panelHeading}>
              <Bell size={20} />
              <h2>최근 활동</h2>
            </div>
            <ul>
              {recentActivities.map((activity) => (
                <li key={activity}>{activity}</li>
              ))}
            </ul>
          </section>
        </div>
      </div>
    </AppLayout>
  )
}

export default MyPage
