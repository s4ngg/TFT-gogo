import { useState, useEffect, useMemo, useCallback } from 'react'
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
  type UnitInfo,
  type PlayGuide,
} from '../../api/adminApi'
import { useCDragonLocale } from '../../hooks/useCDragonLocale'
import { getTraitName, getChampionName } from '../../api/cdragonLocale'
import type { TFTLocale } from '../../api/cdragonLocale'
import type { RankFilter } from '../Dashboard/dashboardData'
import styles from './Admin.module.css'

const BOARD_ROWS = 4
const BOARD_COLS = 7

/* ── 배치판 편집 모달 ── */
interface BoardEditorProps {
  deck: AdminDeck
  onClose: () => void
  onSave: (boardPositionsJson: string | null) => Promise<void>
}

interface CellPos { row: number; col: number }

function BoardEditorModal({ deck, onClose, onSave }: BoardEditorProps) {
  const [selected, setSelected] = useState<UnitInfo | null>(null)
  const [saving, setSaving] = useState(false)

  // imageUrl → CellPos
  const [posMap, setPosMap] = useState<Map<string, CellPos>>(() => {
    if (!deck.boardPositions) return new Map()
    try {
      const obj = JSON.parse(deck.boardPositions) as Record<string, CellPos>
      return new Map(Object.entries(obj))
    } catch {
      return new Map()
    }
  })

  // 역방향: "row-col" → imageUrl
  const cellMap = useMemo(() => {
    const m = new Map<string, string>()
    posMap.forEach((pos, imageUrl) => m.set(`${pos.row}-${pos.col}`, imageUrl))
    return m
  }, [posMap])

  function champAt(row: number, col: number): UnitInfo | undefined {
    const url = cellMap.get(`${row}-${col}`)
    if (!url) return undefined
    return deck.units.find((u) => u.imageUrl === url)
  }

  function handleCellClick(row: number, col: number) {
    const existingUrl = cellMap.get(`${row}-${col}`)

    if (existingUrl) {
      // 이미 챔피언 있음 → 제거
      setPosMap((prev) => {
        const next = new Map(prev)
        next.delete(existingUrl)
        return next
      })
      return
    }

    if (!selected) return

    setPosMap((prev) => {
      const next = new Map(prev)
      // 기존 위치 제거
      next.delete(selected.imageUrl)
      next.set(selected.imageUrl, { row, col })
      return next
    })
    setSelected(null)
  }

  const handleChampClick = useCallback((unit: UnitInfo) => {
    setSelected((prev) => (prev?.imageUrl === unit.imageUrl ? null : unit))
  }, [])

  async function handleSave() {
    setSaving(true)
    const json = posMap.size > 0
      ? JSON.stringify(Object.fromEntries(posMap))
      : null
    try {
      await onSave(json)
      onClose()
    } finally {
      setSaving(false)
    }
  }

  function handleClear() {
    setPosMap(new Map())
  }

  return (
    <div className={styles.modalOverlay} onClick={onClose}>
      <div className={styles.modalBox} onClick={(e) => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          <span className={styles.modalTitle}>배치판 편집 — {deck.displayName}</span>
          <button className={styles.modalClose} onClick={onClose}>✕</button>
        </div>

        <div className={styles.boardEditorBody}>
          {/* 챔피언 팔레트 */}
          <div className={styles.champPalette}>
            <p className={styles.paletteLabel}>챔피언 선택 후 셀 클릭으로 배치</p>
            <div className={styles.champGrid}>
              {deck.units.map((unit) => {
                const placed = posMap.has(unit.imageUrl)
                const isSelected = selected?.imageUrl === unit.imageUrl
                return (
                  <button
                    key={unit.imageUrl}
                    className={`${styles.champChip} ${isSelected ? styles.champChipSelected : ''} ${placed ? styles.champChipPlaced : ''}`}
                    onClick={() => handleChampClick(unit)}
                    title={unit.name}
                  >
                    <img src={unit.imageUrl} alt={unit.name} className={styles.champChipImg} />
                    <span className={styles.champChipName}>{unit.name}</span>
                  </button>
                )
              })}
            </div>
          </div>

          {/* 헥스 그리드 */}
          <div className={styles.editorBoard}>
            {Array.from({ length: BOARD_ROWS }, (_, vi) => {
              const row = BOARD_ROWS - 1 - vi
              const isOffset = row % 2 !== 0
              return (
                <div
                  key={row}
                  className={`${styles.editorRow} ${isOffset ? styles.editorRowOffset : ''}`}
                >
                  {Array.from({ length: BOARD_COLS }, (_, col) => {
                    const unit = champAt(row, col)
                    return (
                      <button
                        key={col}
                        className={`${styles.editorCell} ${unit ? styles.editorCellFilled : styles.editorCellEmpty}`}
                        onClick={() => handleCellClick(row, col)}
                        title={unit ? `${unit.name} 제거` : selected ? `${selected.name} 배치` : ''}
                      >
                        {unit && (
                          <img src={unit.imageUrl} alt={unit.name} className={styles.editorCellImg} />
                        )}
                      </button>
                    )
                  })}
                </div>
              )
            })}
          </div>
        </div>

        <div className={styles.modalFooter}>
          <button className={styles.resetBtn} onClick={handleClear}>전체 초기화</button>
          <button className={styles.saveBtn} onClick={handleSave} disabled={saving}>
            {saving ? '저장중...' : '저장'}
          </button>
        </div>
      </div>
    </div>
  )
}

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

/* ── 운영방법 편집 모달 ── */
interface PlayGuideModalProps {
  deck: AdminDeck
  onClose: () => void
  onSave: (guide: string | null) => Promise<void>
}

function PlayGuideModal({ deck, onClose, onSave }: PlayGuideModalProps) {
  const initial = (): PlayGuide => {
    if (!deck.playGuide) return { early: '', mid: '', late: '' }
    try { return JSON.parse(deck.playGuide) as PlayGuide } catch { return { early: '', mid: '', late: '' } }
  }
  const [guide, setGuide] = useState<PlayGuide>(initial)
  const [saving, setSaving] = useState(false)

  function patch(key: keyof PlayGuide, value: string) {
    setGuide((g) => ({ ...g, [key]: value }))
  }

  async function handleSave() {
    setSaving(true)
    const isEmpty = !guide.early.trim() && !guide.mid.trim() && !guide.late.trim()
    const json = isEmpty ? null : JSON.stringify(guide)
    try {
      await onSave(json)
      onClose()
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={styles.modalOverlay} onClick={onClose}>
      <div className={styles.modalBox} onClick={(e) => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          <span className={styles.modalTitle}>운영방법 편집 — {deck.displayName}</span>
          <button className={styles.modalClose} onClick={onClose}>✕</button>
        </div>

        <div className={styles.guideEditorBody}>
          {(['early', 'mid', 'late'] as const).map((phase) => (
            <div key={phase} className={styles.guidePhase}>
              <label className={styles.guideLabel}>
                {phase === 'early' ? '초반' : phase === 'mid' ? '중반' : '후반'}
              </label>
              <textarea
                className={styles.guideTextarea}
                value={guide[phase]}
                onChange={(e) => patch(phase, e.target.value)}
                placeholder={`${phase === 'early' ? '초반' : phase === 'mid' ? '중반' : '후반'} 운영 방법을 입력하세요`}
                rows={4}
              />
            </div>
          ))}
        </div>

        <div className={styles.modalFooter}>
          <button className={styles.resetBtn} onClick={() => setGuide({ early: '', mid: '', late: '' })}>
            전체 초기화
          </button>
          <button className={styles.saveBtn} onClick={handleSave} disabled={saving}>
            {saving ? '저장중...' : '저장'}
          </button>
        </div>
      </div>
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
  boardEditorOpen: boolean
  guideEditorOpen: boolean
}

function buildKoreanName(deck: AdminDeck, locale: TFTLocale | undefined): string {
  if (!locale) return deck.autoName
  const traitNames = deck.traitSuffixes.slice(0, 2).map((s) => getTraitName(s, locale)).filter(Boolean)
  const carryNames = deck.units
    .filter((u) => u.imageUrl)
    .slice(-2)
    .map((u) => getChampionName(u.imageUrl, locale, ''))
    .filter(Boolean)
  return [...traitNames, ...carryNames].join(' ') || deck.autoName
}

function DeckRow({ deck, onSaved, locale }: { deck: AdminDeck; onSaved: (updated: AdminDeck) => void; locale: TFTLocale | undefined }) {
  const [state, setState] = useState<DeckRowState>({
    customName: deck.customName ?? '',
    hidden: deck.hidden,
    sortPriority: deck.sortPriority != null ? String(deck.sortPriority) : '',
    dirty: false,
    saving: false,
    boardEditorOpen: false,
    guideEditorOpen: false,
  })

  function markDirty(patch: Partial<DeckRowState>) {
    setState((s) => ({ ...s, ...patch, dirty: true }))
  }

  function buildRequest(boardPositions?: string | null, playGuide?: string | null): DeckCurationRequest {
    return {
      customName: state.customName.trim() || null,
      hidden: state.hidden,
      sortPriority: state.sortPriority !== '' ? Number(state.sortPriority) : null,
      curatorNote: null,
      boardPositions: boardPositions !== undefined ? boardPositions : deck.boardPositions,
      playGuide: playGuide !== undefined ? playGuide : deck.playGuide,
    }
  }

  async function handleSave() {
    setState((s) => ({ ...s, saving: true }))
    try {
      const updated = await updateDeckCuration(deck.id, buildRequest())
      onSaved(updated)
      setState((s) => ({ ...s, dirty: false, saving: false }))
    } catch {
      setState((s) => ({ ...s, saving: false }))
      alert('저장 실패')
    }
  }

  async function handleBoardSave(boardPositionsJson: string | null) {
    const updated = await updateDeckCuration(deck.id, buildRequest(boardPositionsJson))
    onSaved(updated)
  }

  async function handleGuideSave(playGuideJson: string | null) {
    const updated = await updateDeckCuration(deck.id, buildRequest(undefined, playGuideJson))
    onSaved(updated)
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
      boardEditorOpen: false,
      guideEditorOpen: false,
    })
    onSaved({ ...deck, customName: null, displayName: deck.autoName, hidden: false, sortPriority: null, boardPositions: null, playGuide: null })
  }

  return (
    <>
      <tr className={state.hidden ? styles.hiddenRow : ''}>
        <td>
          <span className={styles.grade} style={{ color: TIER_COLOR[deck.grade] ?? '#fff' }}>
            {deck.grade}
          </span>
        </td>
        <td>
          <div className={styles.nameCell}>
            <span className={styles.autoName}>자동: {buildKoreanName(deck, locale)}</span>
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
        <td style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
          <button
            className={styles.saveBtn}
            onClick={handleSave}
            disabled={!state.dirty || state.saving}
          >
            {state.saving ? '저장중...' : '저장'}
          </button>
          <button
            className={`${styles.boardBtn} ${deck.boardPositions ? styles.boardBtnActive : ''}`}
            onClick={() => setState((s) => ({ ...s, boardEditorOpen: true }))}
          >
            배치판{deck.boardPositions ? ' ✓' : ''}
          </button>
          <button
            className={`${styles.boardBtn} ${deck.playGuide ? styles.boardBtnActive : ''}`}
            onClick={() => setState((s) => ({ ...s, guideEditorOpen: true }))}
          >
            운영방법{deck.playGuide ? ' ✓' : ''}
          </button>
          {(deck.customName != null || deck.hidden || deck.sortPriority != null) && (
            <button className={styles.resetBtn} onClick={handleReset}>초기화</button>
          )}
        </td>
      </tr>

      {state.boardEditorOpen && (
        <BoardEditorModal
          deck={deck}
          onClose={() => setState((s) => ({ ...s, boardEditorOpen: false }))}
          onSave={handleBoardSave}
        />
      )}
      {state.guideEditorOpen && (
        <PlayGuideModal
          deck={deck}
          onClose={() => setState((s) => ({ ...s, guideEditorOpen: false }))}
          onSave={handleGuideSave}
        />
      )}
    </>
  )
}

/* ── 메인 관리 페이지 ── */
function AdminPage() {
  const [decks, setDecks] = useState<AdminDeck[]>([])
  const [rankFilter, setRankFilter] = useState<RankFilter>('MASTER_PLUS')
  const [loading, setLoading] = useState(true)
  const { data: locale } = useCDragonLocale()

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
              <DeckRow key={deck.id} deck={deck} onSaved={handleSaved} locale={locale} />
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
