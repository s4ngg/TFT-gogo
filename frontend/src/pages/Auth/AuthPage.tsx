import { ArrowRight, LockKeyhole, Mail, UserRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import styles from './AuthPage.module.css'

interface AuthPageProps {
  mode: 'login' | 'signup'
}

const socialProviders = [
  { label: 'Google', mark: 'G' },
  { label: 'Kakao', mark: 'K' },
  { label: 'Naver', mark: 'N' },
]

function AuthPage({ mode }: AuthPageProps) {
  const isSignup = mode === 'signup'

  return (
    <main className={styles.authShell}>
      <section className={styles.formPanel}>
        <Link className={styles.brand} to="/">
          <span aria-hidden="true" />
          <strong>TFTgogo</strong>
        </Link>

        <div className={styles.authTabs}>
          <Link className={!isSignup ? styles.activeTab : undefined} to="/login">
            로그인
          </Link>
          <Link className={isSignup ? styles.activeTab : undefined} to="/signup">
            회원가입
          </Link>
        </div>

        <div className={styles.formHeading}>
          <h2>{isSignup ? '회원가입' : '로그인'}</h2>
        </div>

        <div className={styles.socialLogin} aria-label="소셜 로그인">
          {socialProviders.map((provider) => (
            <button
              type="button"
              className={styles.socialButton}
              key={provider.label}
              aria-label={`${provider.label} 로그인`}
            >
              <span>{provider.mark}</span>
              {provider.label}
            </button>
          ))}
        </div>

        <div className={styles.divider}>
          <span>또는</span>
        </div>

        <form className={styles.authForm}>
          <label>
            <span>이메일</span>
            <div className={styles.inputBox}>
              <Mail size={18} />
              <input type="email" placeholder="tftgogo@example.com" />
            </div>
          </label>

          <label>
            <span>비밀번호</span>
            <div className={styles.inputBox}>
              <LockKeyhole size={18} />
              <input type="password" placeholder="비밀번호 입력" />
            </div>
          </label>

          {isSignup && (
            <>
              <label>
                <span>비밀번호 확인</span>
                <div className={styles.inputBox}>
                  <LockKeyhole size={18} />
                  <input type="password" placeholder="비밀번호 재입력" />
                </div>
              </label>

              <label>
                <span>소환사명#태그</span>
                <div className={styles.inputBox}>
                  <UserRound size={18} />
                  <input placeholder="정동글#KR1" />
                </div>
              </label>
            </>
          )}

          <button type="button" className={styles.submitButton}>
            {isSignup ? '회원가입' : '로그인'}
            <ArrowRight size={19} />
          </button>
        </form>

        <p className={styles.switchText}>
          {isSignup ? '이미 계정이 있으신가요?' : '아직 계정이 없으신가요?'}
          <Link to={isSignup ? '/login' : '/signup'}>{isSignup ? '로그인' : '회원가입'}</Link>
        </p>
      </section>
    </main>
  )
}

export default AuthPage
