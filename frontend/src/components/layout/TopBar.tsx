import { Bell, ChevronDown, CircleHelp, LogIn, LogOut, Mail, MessageCircle, UserRound } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { communityDragonProfileIconUrl } from '../../api/communityDragonAssets'
import useAuthStore from '../../store/useAuthStore'
import styles from './Layout.module.css'

const profileIconUrl = communityDragonProfileIconUrl(588)

type ActiveMenu = 'notifications' | 'mail' | 'profile' | null

const notifications = [
  { title: '파티 신청 도착', body: '다이아 구간 야부/연습 파티에 새 신청이 들어왔어요.', time: '방금 전' },
  { title: '추천 메타 갱신', body: '17.3 기준 선봉대 벡스 지표가 업데이트되었습니다.', time: '12분 전' },
  { title: '전적 분석 완료', body: '최근 20게임 기준 평균 등수와 TOP4 비율을 다시 계산했어요.', time: '31분 전' },
]

const mailbox = [
  { from: '정동글', subject: '오늘 랭크 듀오 가능하세요?', time: '14:58', unread: true },
  { from: '새벽의달', subject: '선봉대 벡스 운영 질문 답장 왔습니다.', time: '14:43', unread: true },
  { from: 'TFTgogo 운영팀', subject: '17.3 패치 요약 리포트가 도착했어요.', time: '13:20', unread: false },
]

function TopBar() {
  const user = useAuthStore((state) => state.user)
  const clearUser = useAuthStore((state) => state.clearUser)
  const isLoggedIn = Boolean(user)
  const [activeMenu, setActiveMenu] = useState<ActiveMenu>(null)
  const actionsRef = useRef<HTMLDivElement>(null)
  const displayName = user?.summonerTag?.split('#')[0] ?? user?.email.split('@')[0] ?? 'TFTgogo'

  useEffect(() => {
    const closeMenu = (event: MouseEvent) => {
      if (!actionsRef.current?.contains(event.target as Node)) {
        setActiveMenu(null)
      }
    }

    document.addEventListener('mousedown', closeMenu)

    return () => {
      document.removeEventListener('mousedown', closeMenu)
    }
  }, [])

  const toggleMenu = (menu: ActiveMenu) => {
    setActiveMenu((currentMenu) => (currentMenu === menu ? null : menu))
  }

  return (
    <header className={styles.topBar}>
      <div className={styles.patchBrief} aria-label="패치 한줄 요약">
        <span>17.3 패치 요약</span>
        <strong>상위권은 선봉대 벡스와 6암흑의 별 진 중심으로 압축 중</strong>
      </div>
      <div className={styles.topActions} ref={actionsRef}>
        {isLoggedIn && (
          <>
            <div className={styles.actionGroup}>
              <button
                type="button"
                className={`${styles.notificationButton} ${activeMenu === 'notifications' ? styles.activeIconButton : ''}`}
                aria-label="알림"
                aria-expanded={activeMenu === 'notifications'}
                onClick={() => toggleMenu('notifications')}
              >
                <Bell size={23} />
                <span>{notifications.length}</span>
              </button>
              {activeMenu === 'notifications' && (
                <section className={styles.topPopover} aria-label="최근 알림">
                  <div className={styles.popoverHeader}>
                    <strong>최근 알림</strong>
                    <small>{notifications.length}개</small>
                  </div>
                  <div className={styles.notificationList}>
                    {notifications.map((notification) => (
                      <article key={notification.title}>
                        <div>
                          <strong>{notification.title}</strong>
                          <time>{notification.time}</time>
                        </div>
                        <p>{notification.body}</p>
                      </article>
                    ))}
                  </div>
                </section>
              )}
            </div>
            <div className={styles.actionGroup}>
              <button
                type="button"
                className={`${styles.iconButton} ${activeMenu === 'mail' ? styles.activeIconButton : ''}`}
                aria-label="메일"
                aria-expanded={activeMenu === 'mail'}
                onClick={() => toggleMenu('mail')}
              >
                <Mail size={24} />
              </button>
              {activeMenu === 'mail' && (
                <section className={styles.topPopover} aria-label="받은 메시지">
                  <div className={styles.popoverHeader}>
                    <strong>받은 메시지</strong>
                    <small>{mailbox.filter((mail) => mail.unread).length}개 안 읽음</small>
                  </div>
                  <div className={styles.mailList}>
                    {mailbox.map((mail) => (
                      <Link to="/party" key={`${mail.from}-${mail.time}`} onClick={() => setActiveMenu(null)}>
                        <span className={mail.unread ? styles.unreadDot : undefined} />
                        <div>
                          <strong>{mail.from}</strong>
                          <p>{mail.subject}</p>
                        </div>
                        <time>{mail.time}</time>
                      </Link>
                    ))}
                  </div>
                  <Link className={styles.popoverAction} to="/party" onClick={() => setActiveMenu(null)}>
                    <MessageCircle size={16} />
                    메시지함 열기
                  </Link>
                </section>
              )}
            </div>
          </>
        )}
        <button type="button" className={styles.iconButton} aria-label="도움말">
          <CircleHelp size={24} />
        </button>
        {isLoggedIn ? (
          <div className={styles.profileGroup}>
            <Link className={styles.profileButton} to="/mypage" onClick={() => setActiveMenu(null)}>
              <span>
                <img src={profileIconUrl} alt="" />
              </span>
              {displayName}
            </Link>
            <button
              type="button"
              className={styles.profileMenuButton}
              aria-label="프로필 메뉴"
              aria-expanded={activeMenu === 'profile'}
              onClick={() => toggleMenu('profile')}
            >
              <ChevronDown size={14} />
            </button>
            {activeMenu === 'profile' && (
              <section className={`${styles.topPopover} ${styles.profilePopover}`} aria-label="프로필 메뉴">
                <div className={styles.profileSummary}>
                  <span>
                    <img src={profileIconUrl} alt="" />
                  </span>
                  <div>
                    <strong>{displayName}</strong>
                    <p>{user?.email}</p>
                  </div>
                </div>
                <Link to="/mypage" onClick={() => setActiveMenu(null)}>
                  <UserRound size={17} />
                  마이페이지
                </Link>
                <Link to="/party" onClick={() => setActiveMenu(null)}>
                  <MessageCircle size={17} />
                  메시지함
                </Link>
                <button
                  type="button"
                  onClick={() => {
                    clearUser()
                    setActiveMenu(null)
                  }}
                >
                  <LogOut size={17} />
                  로그아웃
                </button>
              </section>
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
