import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchAdminDecks, setAdminToken, clearAdminToken } from '../../api/adminApi'
import styles from './Admin.module.css'

function AdminLogin() {
  const [input, setInput] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setAdminToken(input.trim())
    try {
      await fetchAdminDecks()
      navigate('/admin/decks', { replace: true })
    } catch {
      clearAdminToken()
      setError('토큰이 올바르지 않습니다.')
    }
  }

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-main)', display: 'grid', placeItems: 'center' }}>
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

export default AdminLogin
