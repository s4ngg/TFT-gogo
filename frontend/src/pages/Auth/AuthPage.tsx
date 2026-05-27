import { ArrowRight, LockKeyhole, Mail, UserRound } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import useAuthStore from '../../store/useAuthStore'
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
  const navigate = useNavigate()
  const { setUser } = useAuthStore()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [summonerTag, setSummonerTag] = useState('')
  const [emailError, setEmailError] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [passwordConfirmError, setPasswordConfirmError] = useState('')

  const validate = () => {
    let isValid = true

    if (!email.includes('@')) {
      setEmailError('이메일 형식이 올바르지 않습니다.')
      isValid = false
    } else {
      setEmailError('')
    }

    if (password.length < 8) {
      setPasswordError('비밀번호는 8자 이상이어야 합니다.')
      isValid = false
    } else {
      setPasswordError('')
    }

    if (isSignup && password !== passwordConfirm) {
      setPasswordConfirmError('비밀번호가 일치하지 않습니다.')
      isValid = false
    } else {
      setPasswordConfirmError('')
    }

    return isValid
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!validate()) return

    if (isSignup) {
      console.log('회원가입 시도:', { email, summonerTag })
      setUser({ email, summonerTag })
      navigate('/')
    } else {
      console.log('로그인 시도:', { email })
      setUser({ email })
      navigate('/')
    }
  }

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

        <form className={styles.authForm} onSubmit={handleSubmit} noValidate>
          <label>
            <span>이메일</span>
            <div className={styles.inputBox}>
              <Mail size={18} />
              <input
                type="email"
                placeholder="tftgogo@example.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </div>
            {emailError && <p className={styles.errorText}>{emailError}</p>}
          </label>

          <label>
            <span>비밀번호</span>
            <div className={styles.inputBox}>
              <LockKeyhole size={18} />
              <input
                type="password"
                placeholder="비밀번호 입력"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </div>
            {passwordError && <p className={styles.errorText}>{passwordError}</p>}
          </label>

          {isSignup && (
            <>
              <label>
                <span>비밀번호 확인</span>
                <div className={styles.inputBox}>
                  <LockKeyhole size={18} />
                  <input
                    type="password"
                    placeholder="비밀번호 재입력"
                    value={passwordConfirm}
                    onChange={(event) => setPasswordConfirm(event.target.value)}
                  />
                </div>
                {passwordConfirmError && <p className={styles.errorText}>{passwordConfirmError}</p>}
              </label>

              <label>
                <span>소환사명#태그</span>
                <div className={styles.inputBox}>
                  <UserRound size={18} />
                  <input
                    placeholder="정동글#KR1"
                    value={summonerTag}
                    onChange={(event) => setSummonerTag(event.target.value)}
                  />
                </div>
              </label>
            </>
          )}

          <button type="submit" className={styles.submitButton}>
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
