import { type FormEvent, useState } from 'react'
import { adminLogin, setAccessToken, isAdminAuthFailure } from '../../../api/adminApi'
import { useAdminSession } from '../../../hooks/useAdminSession'
import styles from '../Admin.module.css'

export default function TokenGate({ onSuccess }: { onSuccess: () => void }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { setSession } = useAdminSession()

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const result = await adminLogin({ username: username.trim(), password })
      setAccessToken(result.accessToken)
      setSession({ username: result.username, role: result.role })
      onSuccess()
    } catch (err: unknown) {
      if (isAdminAuthFailure(err)) {
        setError('아이디 또는 비밀번호가 올바르지 않습니다.')
      } else {
        setError('인증 서버에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.page}>
      <form className={styles.tokenForm} onSubmit={handleSubmit}>
        <h2 className={styles.title}>관리자 로그인</h2>
        <label className={styles.tokenLabel} htmlFor="tg-username">아이디</label>
        <input
          id="tg-username"
          type="text"
          className={styles.tokenInput}
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="관리자 아이디"
          autoFocus
          autoComplete="username"
          required
          disabled={loading}
        />
        <label className={styles.tokenLabel} htmlFor="tg-password">비밀번호</label>
        <input
          id="tg-password"
          type="password"
          className={styles.tokenInput}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="비밀번호"
          autoComplete="current-password"
          required
          disabled={loading}
        />
        {error && <span className={styles.tokenError}>{error}</span>}
        <button
          type="submit"
          className={styles.tokenBtn}
          disabled={loading || !username.trim() || !password}
        >
          {loading ? '로그인 중...' : '로그인'}
        </button>
      </form>
    </div>
  )
}
