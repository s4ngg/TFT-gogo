import { ArrowRight, LockKeyhole, Mail, UserRound } from 'lucide-react'
import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login as loginMember, signup as signupMember } from '../../api/memberApi'
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

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

interface AuthMutationVariables {
  email: string
  password: string
  summonerTag?: string
}

function parseSummonerTag(value: string) {
  const [summonerName = '', tagLine = ''] = value.split('#').map((part) => part.trim())

  return {
    summonerName: summonerName || undefined,
    tagLine: tagLine || undefined,
  }
}

function mapAuthError(error: unknown, isSignup: boolean) {
  const message = error instanceof Error ? error.message : String(error)
  const normalizedMessage = message.toLowerCase()

  if (normalizedMessage.includes('network') || normalizedMessage.includes('timeout')) {
    return '서버와 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.'
  }

  if (
    normalizedMessage.includes('401') ||
    normalizedMessage.includes('unauthorized') ||
    normalizedMessage.includes('login failed')
  ) {
    return '이메일 또는 비밀번호가 올바르지 않습니다.'
  }

  if (
    normalizedMessage.includes('409') ||
    normalizedMessage.includes('conflict') ||
    normalizedMessage.includes('already') ||
    normalizedMessage.includes('duplicate')
  ) {
    return '이미 사용 중인 이메일입니다.'
  }

  if (normalizedMessage.includes('signup failed')) {
    return '회원가입 정보를 다시 확인해 주세요.'
  }

  return isSignup
    ? '회원가입 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    : '로그인 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.'
}

function AuthPage({ mode }: AuthPageProps) {
  const isSignup = mode === 'signup'
  const navigate = useNavigate()
  const setAuth = useAuthStore((state) => state.setAuth)

  // 입력값 상태 — 키 입력할 때마다 값 저장
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [summonerTag, setSummonerTag] = useState('')

  // 에러 메시지 상태 — 빈 문자열이면 에러 없음
  const [emailError, setEmailError] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [passwordConfirmError, setPasswordConfirmError] = useState('')

  const authMutation = useMutation({
    mutationFn: ({ email, password, summonerTag }: AuthMutationVariables) =>
      isSignup
        ? signupMember({
            email,
            password,
            ...parseSummonerTag(summonerTag ?? ''),
          })
        : loginMember({ email, password }),
    onSuccess: (auth) => {
      setAuth(auth)
      navigate('/', { replace: true })
    },
  })

  const apiError = authMutation.error ? mapAuthError(authMutation.error, isSignup) : ''

  // 입력값 유효성 검사
  const validate = () => {
    let isValid = true
    const trimmedEmail = email.trim()

    if (!emailPattern.test(trimmedEmail)) {
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

  // 폼 제출 처리
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // e.preventDefault() — 페이지 새로고침 되는 브라우저 기본 동작을 막는 것

    authMutation.reset()

    if (!validate()) return
    // validate() 실패하면 여기서 멈추고 에러 메시지 표시

    authMutation.mutate({
      email: email.trim(),
      password,
      summonerTag: isSignup ? summonerTag : undefined,
    })
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

          {/* onSubmit — 버튼 클릭 시 handleSubmit 실행 */}
          <form className={styles.authForm} onSubmit={handleSubmit} noValidate>
            <label>
              <span>이메일</span>
              <div className={styles.inputBox}>
                <Mail size={18} />
                <input
                    type="email"
                    placeholder="tftgogo@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
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
                    onChange={(e) => setPassword(e.target.value)}
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
                          onChange={(e) => setPasswordConfirm(e.target.value)}
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
                          onChange={(e) => setSummonerTag(e.target.value)}
                      />
                    </div>
                  </label>
                </>
            )}



            {apiError && <p className={styles.errorText}>{apiError}</p>}

            <button type="submit" className={styles.submitButton} disabled={authMutation.isPending}>
              {authMutation.isPending ? (isSignup ? '가입 중...' : '로그인 중...') : (isSignup ? '회원가입' : '로그인')}
              {!authMutation.isPending && <ArrowRight size={19} />}
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
