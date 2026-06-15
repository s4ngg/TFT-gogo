import { ArrowRight, LockKeyhole, Mail, UserRound } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { login as loginMember, signup as signupMember } from '../../api/memberApi'
import { getSocialLoginStart, type SocialProvider } from '../../api/socialAuth'
import { AUTH_ME_QUERY_KEY } from '../../hooks/useAuthSession'
import useAuthStore from '../../store/useAuthStore'
import { mapAuthError, mapOAuthErrorCode, mapSocialAuthError } from './utils/authUtils'
import styles from './AuthPage.module.css'

interface AuthPageProps {
  mode: 'login' | 'signup'
}

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

interface AuthMutationVariables {
  email: string
  nickname?: string
  password: string
}

interface SocialProviderConfig {
  id: SocialProvider
  label: string
  mark: string
}

const socialProviders: SocialProviderConfig[] = [
  { id: 'google', label: 'Google', mark: 'G' },
  { id: 'kakao', label: 'Kakao', mark: 'K' },
  { id: 'naver', label: 'Naver', mark: 'N' },
]

function AuthPage({ mode }: AuthPageProps) {
  const isSignup = mode === 'signup'
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const queryClient = useQueryClient()
  const setAuth = useAuthStore((state) => state.setAuth)
  const oauthErrorCode = searchParams.get('oauthError')

  // 입력값 상태 — 키 입력할 때마다 값 저장
  const [email, setEmail] = useState('')
  const [nickname, setNickname] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')

  // 에러 메시지 상태 — 빈 문자열이면 에러 없음
  const [emailError, setEmailError] = useState('')
  const [nicknameError, setNicknameError] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [passwordConfirmError, setPasswordConfirmError] = useState('')

  const authMutation = useMutation({
    mutationFn: ({ email, nickname, password }: AuthMutationVariables) =>
      isSignup
        ? signupMember({
            email,
            nickname: nickname ?? '',
            password,
          })
        : loginMember({ email, password }),
    onSuccess: (auth) => {
      queryClient.removeQueries({ queryKey: AUTH_ME_QUERY_KEY })
      setAuth(auth)
      navigate('/', { replace: true })
    },
  })

  const socialLoginMutation = useMutation({
    mutationFn: getSocialLoginStart,
    onSuccess: ({ authorizationUrl }) => {
      window.location.assign(authorizationUrl)
    },
  })

  const apiError = authMutation.error ? mapAuthError(authMutation.error, isSignup) : ''
  const socialError = socialLoginMutation.error
    ? mapSocialAuthError(socialLoginMutation.error)
    : mapOAuthErrorCode(oauthErrorCode)
  const isAuthActionPending = authMutation.isPending || socialLoginMutation.isPending

  const clearOAuthError = () => {
    if (oauthErrorCode) {
      setSearchParams({}, { replace: true })
    }
  }

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

    const trimmedNickname = nickname.trim()

    if (isSignup && !trimmedNickname) {
      setNicknameError('닉네임을 입력해 주세요.')
      isValid = false
    } else if (isSignup && trimmedNickname.length > 50) {
      setNicknameError('닉네임은 50자 이하여야 합니다.')
      isValid = false
    } else {
      setNicknameError('')
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

    if (isAuthActionPending) {
      return
    }

    authMutation.reset()
    socialLoginMutation.reset()
    clearOAuthError()

    if (!validate()) return
    // validate() 실패하면 여기서 멈추고 에러 메시지 표시

    authMutation.mutate({
      email: email.trim(),
      nickname: isSignup ? nickname.trim() : undefined,
      password,
    })
  }

  const handleSocialLogin = (provider: SocialProvider) => {
    if (isAuthActionPending) {
      return
    }

    authMutation.reset()
    socialLoginMutation.reset()
    clearOAuthError()
    socialLoginMutation.mutate(provider)
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
                    disabled={isAuthActionPending}
                    onClick={() => handleSocialLogin(provider.id)}
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
                    <span>닉네임</span>
                    <div className={styles.inputBox}>
                      <UserRound size={18} />
                      <input
                          maxLength={50}
                          placeholder="정동글"
                          value={nickname}
                          onChange={(e) => setNickname(e.target.value)}
                      />
                    </div>
                    {nicknameError && <p className={styles.errorText}>{nicknameError}</p>}
                  </label>
                </>
            )}



            {apiError && <p className={styles.errorText}>{apiError}</p>}
            {!apiError && socialError && <p className={styles.errorText}>{socialError}</p>}

            <button type="submit" className={styles.submitButton} disabled={isAuthActionPending}>
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
