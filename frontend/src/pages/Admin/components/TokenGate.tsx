import { type FormEvent, useState } from 'react'
import {
  clearAdminToken,
  isAdminAuthFailure,
  setAdminToken,
  validateAdminToken,
} from '../../../api/adminApi'
import styles from '../Admin.module.css'

export default function TokenGate({ onSuccess }: { onSuccess: () => void }) {
  const [input, setInput] = useState('')
  const [error, setError] = useState('')

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setAdminToken(input.trim())
    try {
      await validateAdminToken()
      onSuccess()
    } catch (error: unknown) {
      if (isAdminAuthFailure(error)) {
        clearAdminToken()
        setError('토큰이 올바르지 않습니다.')
        return
      }

      setError('인증 서버에 일시적인 문제가 있습니다. 잠시 후 다시 시도해주세요.')
    }
  }

  return (
    <div className={styles.page}>
      <form className={styles.tokenForm} onSubmit={handleSubmit}>
        <h2 className={styles.title}>관리자 인증</h2>
        <label className={styles.tokenLabel}>Admin Token</label>
        <input
          type="password"
          className={styles.tokenInput}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="관리자 토큰 입력"
          autoFocus
        />
        {error && <span className={styles.tokenError}>{error}</span>}
        <button type="submit" className={styles.tokenBtn}>확인</button>
      </form>
    </div>
  )
}

/* ── 운영방법 편집 모달 ── */
