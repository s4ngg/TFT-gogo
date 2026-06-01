import { useState, useEffect } from 'react'
import { AppLayout } from '../../components/layout'
import {
  fetchAdminDecks,
  updateDeckCuration,
  resetDeckCuration,
  getAdminToken,
  setAdminToken,
  clearAdminToken,
  type AdminDeck,
  type DeckCurationRequest,
} from '../../api/adminApi'
import type { RankFilter } from '../Dashboard/dashboardData'
import styles from './Admin.module.css'

const RANK_OPTIONS: { label: string; value: RankFilter }[] = [
  { label: '마스터+', value: 'MASTER_PLUS' },
  { label: '다이아+', value: 'DIAMOND_PLUS' },
  { label: '에메랄드+', value: 'EMERALD_PLUS' },
]

const TIER_COLOR: Record<string, string> = {
  S: '#04f3e5', 'A+': '#f7d26d', A: '#a78bfa', B: '#60a5fa', C: '#818cf8', D: '#6b7280',
}

/* ── 토큰 입력 화면 ── */
function TokenGate({ onSuccess }: { onSuccess: () => void }) {
  const [input, setInput] = useState('')
  const [error, setError] = useState('')

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setAdminToken(input.trim())
    try {
      await fetchAdminDecks()  // 토큰 검증
      onSuccess()
    } catch {
      clearAdminToken()
      setError('토큰이 올바르지 않습니다.')
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

/* ── 덱 행 ── */
interface DeckRowState {
  customName: string
  hidden: boolean
  sortPriority: string
  dirty: boolean
  saving: boolean
}

function DeckRow({ deck, onSaved }: { deck: AdminDeck; onSaved: (updated: AdminDeck) => void }) {
  const [state, setState] = useState<DeckRowState>({
    customName: deck.customName ?? '',
    hidden: deck.hidden,
    sortPriority: deck.sortPriority != null ? String(deck.sortPriority) : '',
    dirty: false,
    saving: false,
  })

  function markDirty(patch: Partial<DeckRowState>) {
    setState((s) => ({ ...s, ...patch, dirty: true }))
  }

  async function handleSave() {
    setState((s) => ({ ...s, saving: true }))
    const req: DeckCurationRequest = {
      customName: state.customName.trim() || null,
      hidden: state.hidden,
      sortPriority: state.sortPriority !== '' ? Number(state.sortPriority) : null,
      curatorNote: null,
    }
    try {
      const updated = await updateDeckCuration(deck.id, req)
      onSaved(updated)
      setState((s) => ({ ...s, dirty: false, saving: false }))
    } catch {
      setState((s) => ({ ...s, saving: false }))
      alert('저장 실패')
    }
  }

  async function handleReset() {
    if (!confirm('큐레이션을 초기화하고 자동 이름으로 되돌릴까요?')) return
    await resetDeckCuration(deck.id)
    setState({
      customName: '',
      hidden: false,
      sortPriority: '',
      dirty: false,
      saving: false,
    })
    onSaved({ ...deck, customName: null, displayName: deck.autoName, hidden: false, sortPriority: null })
  }

  return (
    <tr className={state.hidden ? styles.hiddenRow : ''}>
      <td>
        <span className={styles.grade} style={{ color: TIER_COLOR[deck.grade] ?? '#fff' }}>
          {deck.grade}
        </span>
      </td>
      <td>
        <div className={styles.nameCell}>
          <span className={styles.autoName}>자동: {deck.autoName}</span>
          <input
            className={styles.nameInput}
            value={state.customName}
            onChange={(e) => markDirty({ customName: e.target.value })}
            placeholder="커스텀 이름 (비워두면 자동 이름 사용)"
          />
          {state.dirty && <span className={styles.modified}>● 수정됨</span>}
        </div>
      </td>
      <td className={styles.stat}>{deck.winRate}</td>
      <td className={styles.stat}>{deck.pickRate}</td>
      <td className={styles.stat}>n={deck.sampleSize}</td>
      <td>
        <input
          type="number"
          className={styles.nameInput}
          style={{ width: 60 }}
          value={state.sortPriority}
          onChange={(e) => markDirty({ sortPriority: e.target.value })}
          placeholder="순서"
          min={1}
        />
      </td>
      <td>
        <label className={styles.toggle}>
          <input
            type="checkbox"
            checked={state.hidden}
            onChange={(e) => markDirty({ hidden: e.target.checked })}
          />
          <span className={styles.toggleTrack} />
          <span className={styles.toggleThumb} />
        </label>
      </td>
      <td style={{ display: 'flex', gap: 6 }}>
        <button
          className={styles.saveBtn}
          onClick={handleSave}
          disabled={!state.dirty || state.saving}
        >
          {state.saving ? '저장중...' : '저장'}
        </button>
        {(deck.customName != null || deck.hidden || deck.sortPriority != null) && (
          <button className={styles.resetBtn} onClick={handleReset}>초기화</button>
        )}
      </td>
    </tr>
  )
}

/* ── 메인 관리 페이지 ── */
function AdminPage() {
  const [decks, setDecks] = useState<AdminDeck[]>([])
  const [rankFilter, setRankFilter] = useState<RankFilter>('MASTER_PLUS')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    fetchAdminDecks(rankFilter)
      .then(setDecks)
      .finally(() => setLoading(false))
  }, [rankFilter])

  function handleSaved(updated: AdminDeck) {
    setDecks((prev) => prev.map((d) => (d.id === updated.id ? { ...d, ...updated } : d)))
  }

  function handleLogout() {
    clearAdminToken()
    window.location.reload()
  }

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h1 className={styles.title}>덱 관리자</h1>
        <select
          className={styles.rankSelect}
          value={rankFilter}
          onChange={(e) => setRankFilter(e.target.value as RankFilter)}
        >
          {RANK_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
        <button className={styles.logoutBtn} onClick={handleLogout}>로그아웃</button>
      </div>

      {loading ? (
        <p style={{ color: 'var(--text-muted)' }}>불러오는 중...</p>
      ) : (
        <table className={styles.table}>
          <thead>
            <tr>
              <th>티어</th>
              <th>덱 이름</th>
              <th>승률</th>
              <th>픽률</th>
              <th>표본</th>
              <th>순서</th>
              <th>숨김</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            {decks.map((deck) => (
              <DeckRow key={deck.id} deck={deck} onSaved={handleSaved} />
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

/* ── 진입점: 토큰 없으면 TokenGate, 있으면 AdminPage ── */
function Admin() {
  const [authed, setAuthed] = useState(() => getAdminToken() !== '')

  if (!authed) {
    return (
      <AppLayout>
        <TokenGate onSuccess={() => setAuthed(true)} />
      </AppLayout>
    )
  }

  return (
    <AppLayout>
      <AdminPage />
    </AppLayout>
  )
}

export default Admin
