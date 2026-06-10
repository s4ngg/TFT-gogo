import { useState, useEffect, useMemo, useCallback } from 'react'
import { AppLayout } from '../../components/layout'
import {
  fetchAdminDecks,
  updateDeckCuration,
  resetDeckCuration,
  getAdminToken,
  setAdminToken,
  clearAdminToken,
  fetchAdminHeroAugmentDecks,
  createHeroAugmentDeck,
  updateHeroAugmentDeck,
  deleteHeroAugmentDeck,
  type AdminDeck,
  type DeckCurationRequest,
  type PlayGuide,
  type HeroAugmentEntry,
  type HeroAugmentDeckItem,
  type HeroAugmentDeckPayload,
} from '../../api/adminApi'
import { useCDragonLocale } from '../../hooks/useCDragonLocale'
import { getTraitName, getChampionName } from '../../api/cdragonLocale'
import type { TFTLocale } from '../../api/cdragonLocale'
import { tftChampSquareUrl, tftItemIconUrl } from '../../api/communityDragonAssets'
import type { RankFilter } from '../Dashboard/dashboardData'
import styles from './Admin.module.css'

const BOARD_ROWS = 4
const BOARD_COLS = 7
const BOARD_LEVELS = [5, 6, 7, 8, 9, 10]
const COST_COLORS: Record<number, string> = { 1: '#8e9497', 2: '#4ade80', 3: '#60a5fa', 4: '#c084fc', 5: '#f9c860' }

interface CellPos { row: number; col: number; items?: string[] }
interface ChampInfo { apiName: string; name: string; imageUrl: string; cost: number }

function isCompleteItem(key: string): boolean {
  const k = key.toLowerCase()
  if (!k.startsWith('tft_item_')) return false
  return !k.includes('emptybag') && !k.includes('radiant') && !k.includes('artifact')
    && !k.includes('support') && !k.includes('emblem') && !k.includes('trait')
    && !k.includes('consumable') && !k.includes('temporary') && !k.includes('ornn')
    && !k.endsWith('bfsword') && !k.endsWith('recurvebow') && !k.endsWith('needlesslylargerod')
    && !k.endsWith('tearofthegoddess') && !k.endsWith('chainvest') && !k.endsWith('negatroncloak')
    && !k.endsWith('giantsbelt') && !k.endsWith('sparringgloves') && !k.endsWith('spatula')
    && !k.endsWith('fryingpan') && !k.endsWith('shimmerscale')
}

// level → (imageUrl → CellPos)
type LevelBoards = Map<number, Map<string, CellPos>>

function parseLevelBoards(json: string | null | undefined): LevelBoards {
  if (!json) return new Map()
  try {
    const obj = JSON.parse(json) as Record<string, Record<string, CellPos>>
    const result: LevelBoards = new Map()
    for (const [k, posObj] of Object.entries(obj)) {
      const lv = Number(k)
      if (BOARD_LEVELS.includes(lv))
        result.set(lv, new Map(Object.entries(posObj)))
    }
    return result
  } catch { return new Map() }
}

function serializeLevelBoards(boards: LevelBoards): string | null {
  const obj: Record<string, Record<string, CellPos>> = {}
  boards.forEach((posMap, lv) => {
    if (posMap.size > 0) obj[String(lv)] = Object.fromEntries(posMap)
  })
  return Object.keys(obj).length > 0 ? JSON.stringify(obj) : null
}

/* ── 배치판 편집 모달 ── */
interface BoardEditorProps {
  deck: AdminDeck
  locale: TFTLocale | undefined
  onClose: () => void
  onSave: (boardPositionsJson: string | null) => Promise<void>
}

function BoardEditorModal({ deck, locale, onClose, onSave }: BoardEditorProps) {
  const [activeLevel, setActiveLevel] = useState(5)
  const [selected, setSelected] = useState<ChampInfo | null>(null)
  const [editingItemsFor, setEditingItemsFor] = useState<string | null>(null) // apiName
  const [itemSearch, setItemSearch] = useState('')
  const [saving, setSaving] = useState(false)
  const [levelBoards, setLevelBoards] = useState<LevelBoards>(() => parseLevelBoards(deck.boardPositions))

  // CDragon 아이템 목록
  const allItems = useMemo(() => {
    if (!locale) return []
    return Array.from(locale.itemByApiName.entries())
      .filter(([key]) => isCompleteItem(key))
      .map(([key, name]) => ({ id: key, name }))
      .sort((a, b) => a.name.localeCompare(b.name))
  }, [locale])

  const filteredItems = useMemo(() =>
    itemSearch.trim()
      ? allItems.filter((it) => it.name.toLowerCase().includes(itemSearch.toLowerCase()))
      : allItems,
    [allItems, itemSearch],
  )

  // CDragon 전체 챔피언 → 코스트별 그룹
  const champsByCost = useMemo<Map<number, ChampInfo[]>>(() => {
    const map = new Map<number, ChampInfo[]>()
    if (!locale) return map
    locale.champDetailByApiName.forEach((detail) => {
      if (detail.cost < 1 || detail.cost > 5) return
      const info: ChampInfo = {
        apiName: detail.apiName,
        name: detail.name,
        imageUrl: tftChampSquareUrl(detail.apiName),
        cost: detail.cost,
      }
      const list = map.get(detail.cost) ?? []
      list.push(info)
      map.set(detail.cost, list)
    })
    map.forEach((list) => list.sort((a, b) => a.name.localeCompare(b.name)))
    return map
  }, [locale])

  const posMap = useMemo(
    () => levelBoards.get(activeLevel) ?? new Map<string, CellPos>(),
    [levelBoards, activeLevel],
  )

  const cellMap = useMemo(() => {
    const m = new Map<string, string>()
    posMap.forEach((pos, apiName) => m.set(`${pos.row}-${pos.col}`, apiName))
    return m
  }, [posMap])

  function apiNameToChamp(apiName: string): ChampInfo | undefined {
    for (const list of champsByCost.values()) {
      const found = list.find((c) => c.apiName === apiName)
      if (found) return found
    }
    return undefined
  }

  function champAt(row: number, col: number): ChampInfo | undefined {
    const apiName = cellMap.get(`${row}-${col}`)
    return apiName ? apiNameToChamp(apiName) : undefined
  }

  function setPosMap(updater: (prev: Map<string, CellPos>) => Map<string, CellPos>) {
    setLevelBoards((prev) => {
      const next = new Map(prev)
      next.set(activeLevel, updater(prev.get(activeLevel) ?? new Map()))
      return next
    })
  }

  function handleCellClick(row: number, col: number) {
    const existingKey = cellMap.get(`${row}-${col}`)
    if (existingKey) {
      if (selected) {
        // 챔피언 선택 중 → 기존 제거 + 해당 챔피언 편집 중이었으면 피커도 닫기
        setPosMap((prev) => { const n = new Map(prev); n.delete(existingKey); return n })
        setEditingItemsFor((prev) => prev === existingKey ? null : prev)
        setItemSearch('')
      } else {
        // 챔피언 미선택 → 아이템 편집 모드 토글
        setEditingItemsFor((prev) => prev === existingKey ? null : existingKey)
        setItemSearch('')
      }
      return
    }
    setEditingItemsFor(null)
    if (!selected) return
    setPosMap((prev) => {
      const n = new Map(prev)
      const prevPos = n.get(selected.apiName)
      n.delete(selected.apiName)
      n.set(selected.apiName, { row, col, items: prevPos?.items })
      return n
    })
    setSelected(null)
  }

  function toggleItem(apiName: string, itemId: string) {
    setPosMap((prev) => {
      const n = new Map(prev)
      const pos = n.get(apiName)
      if (!pos) return n
      const items = pos.items ?? []
      const has = items.includes(itemId)
      n.set(apiName, {
        ...pos,
        items: has ? items.filter((i) => i !== itemId) : items.length >= 3 ? items : [...items, itemId],
      })
      return n
    })
  }

  const handleChampClick = useCallback((champ: ChampInfo) => {
    setSelected((prev) => (prev?.apiName === champ.apiName ? null : champ))
  }, [])

  async function handleSave() {
    setSaving(true)
    try {
      await onSave(serializeLevelBoards(levelBoards))
      onClose()
    } finally { setSaving(false) }
  }

  return (
    <div className={styles.modalOverlay} onClick={onClose}>
      <div className={styles.modalBox} style={{ width: 'min(1100px, 96vw)' }} onClick={(e) => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          <span className={styles.modalTitle}>배치판 편집 — {deck.displayName}</span>
          <button className={styles.modalClose} onClick={onClose}>✕</button>
        </div>

        {/* 레벨 탭 */}
        <div className={styles.levelTabs}>
          {BOARD_LEVELS.map((lv) => {
            const hasData = (levelBoards.get(lv)?.size ?? 0) > 0
            return (
              <button
                key={lv}
                className={`${styles.levelTab} ${lv === activeLevel ? styles.levelTabActive : ''} ${hasData ? styles.levelTabHasData : ''}`}
                onClick={() => { setActiveLevel(lv); setEditingItemsFor(null) }}
              >
                Lv.{lv}{hasData ? ' ✓' : ''}
              </button>
            )
          })}
        </div>

        <div className={styles.boardEditorBody}>
          {/* 챔피언 팔레트 — 코스트별 */}
          <div className={styles.champPalette}>
            <p className={styles.paletteLabel}>챔피언 클릭 → 셀 클릭으로 배치</p>
            {[1, 2, 3, 4, 5].map((cost) => {
              const list = champsByCost.get(cost) ?? []
              if (list.length === 0) return null
              return (
                <div key={cost} className={styles.costGroup}>
                  <span className={styles.costLabel} style={{ color: COST_COLORS[cost] }}>
                    {cost}코스트
                  </span>
                  <div className={styles.champGrid}>
                    {list.map((champ) => {
                      const placed = posMap.has(champ.apiName)
                      const isSelected = selected?.apiName === champ.apiName
                      return (
                        <button
                          key={champ.apiName}
                          className={`${styles.champChip} ${isSelected ? styles.champChipSelected : ''} ${placed ? styles.champChipPlaced : ''}`}
                          onClick={() => handleChampClick(champ)}
                          title={champ.name}
                        >
                          <img
                            src={champ.imageUrl}
                            alt={champ.name}
                            className={styles.champChipImg}
                            onError={(e) => { e.currentTarget.parentElement!.style.display = 'none' }}
                          />
                          <span className={styles.champChipName}>{champ.name}</span>
                        </button>
                      )
                    })}
                  </div>
                </div>
              )
            })}
          </div>

          {/* 헥스 그리드 + 아이템 피커 */}
          <div className={styles.editorBoardSection}>
          <div className={styles.editorBoard}>
            {Array.from({ length: BOARD_ROWS }, (_, vi) => {
              const row = BOARD_ROWS - 1 - vi
              const isOffset = row % 2 !== 0
              return (
                <div key={row} className={`${styles.editorRow} ${isOffset ? styles.editorRowOffset : ''}`}>
                  {Array.from({ length: BOARD_COLS }, (_, col) => {
                    const champ = champAt(row, col)
                    const isEditingThis = champ && editingItemsFor === champ.apiName
                    const champItems = champ ? (posMap.get(champ.apiName)?.items ?? []) : []
                    return (
                      <div key={col} className={styles.editorCellWrap}>
                        <button
                          className={`${styles.editorCell} ${champ ? styles.editorCellFilled : styles.editorCellEmpty} ${isEditingThis ? styles.editorCellEditing : ''}`}
                          onClick={() => handleCellClick(row, col)}
                          title={champ ? (selected ? `${champ.name} 제거` : `${champ.name} 아이템 편집`) : selected ? `${selected.name} 배치` : ''}
                        >
                          {champ && (
                            <img
                              src={champ.imageUrl}
                              alt={champ.name}
                              className={styles.editorCellImg}
                              onError={(e) => { e.currentTarget.style.opacity = '0.2' }}
                            />
                          )}
                        </button>
                        {champ && champItems.length > 0 && (
                          <div className={styles.editorCellItems}>
                            {champItems.slice(0, 3).map((itemId) => (
                              <img key={itemId} src={tftItemIconUrl(itemId)} alt={itemId} className={styles.editorCellItemIcon} onError={(e) => { e.currentTarget.style.display = 'none' }} />
                            ))}
                          </div>
                        )}
                      </div>
                    )
                  })}
                </div>
              )
            })}
          </div>

          {/* 아이템 피커 */}
          {editingItemsFor && (() => {
            const editingChamp = apiNameToChamp(editingItemsFor)
            const currentItems = posMap.get(editingItemsFor)?.items ?? []
            return (
              <div className={styles.itemPicker}>
                <div className={styles.itemPickerHeader}>
                  <span>{editingChamp?.name} 아이템 ({currentItems.length}/3)</span>
                  <button className={styles.modalClose} onClick={() => setEditingItemsFor(null)}>✕</button>
                </div>
                {currentItems.length > 0 && (
                  <div className={styles.itemPickerSelected}>
                    {currentItems.map((id) => (
                      <button key={id} className={styles.itemPickerChip} onClick={() => toggleItem(editingItemsFor, id)} title="클릭해서 제거">
                        <img src={tftItemIconUrl(id)} alt={id} className={styles.itemPickerIcon} onError={(e) => { e.currentTarget.style.opacity='0.3' }} />
                      </button>
                    ))}
                  </div>
                )}
                <input
                  className={styles.itemPickerSearch}
                  placeholder="아이템 검색..."
                  value={itemSearch}
                  onChange={(e) => setItemSearch(e.target.value)}
                />
                <div className={styles.itemPickerGrid}>
                  {filteredItems.slice(0, 40).map((item) => {
                    const isSelected = currentItems.includes(item.id)
                    const disabled = !isSelected && currentItems.length >= 3
                    return (
                      <button
                        key={item.id}
                        className={`${styles.itemPickerChip} ${isSelected ? styles.itemPickerChipSelected : ''}`}
                        onClick={() => !disabled && toggleItem(editingItemsFor, item.id)}
                        disabled={disabled}
                        title={item.name}
                      >
                        <img src={tftItemIconUrl(item.id)} alt={item.name} className={styles.itemPickerIcon} onError={(e) => { e.currentTarget.style.opacity='0.3' }} />
                        <span className={styles.itemPickerName}>{item.name}</span>
                      </button>
                    )
                  })}
                </div>
              </div>
            )
          })()}
          </div>
        </div>

        <div className={styles.modalFooter}>
          <button className={styles.resetBtn} onClick={() => setPosMap(() => new Map())}>
            Lv.{activeLevel} 초기화
          </button>
          <button className={styles.resetBtn} onClick={() => setLevelBoards(new Map())}>
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

const RANK_OPTIONS: { label: string; value: RankFilter }[] = [
  { label: '마스터+', value: 'MASTER_PLUS' },
  { label: '다이아+', value: 'DIAMOND_PLUS' },
  { label: '에메랄드+', value: 'EMERALD_PLUS' },
]

const TIER_COLOR: Record<string, string> = {
  S: '#04f3e5', A: '#a78bfa', B: '#60a5fa', C: '#818cf8', D: '#6b7280',
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

/* ── 영웅 증강 편집 모달 ── */
interface HeroAugmentModalProps {
  deck: AdminDeck
  locale: TFTLocale | undefined
  onClose: () => void
  onSave: (json: string | null) => Promise<void>
}

function HeroAugmentModal({ deck, locale, onClose, onSave }: HeroAugmentModalProps) {
  const parseEntries = (): HeroAugmentEntry[] => {
    if (!deck.heroAugments) return []
    try { return JSON.parse(deck.heroAugments) as HeroAugmentEntry[] } catch { return [] }
  }
  const [entries, setEntries] = useState<HeroAugmentEntry[]>(parseEntries)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState('')

  // 덱에 속한 챔피언 목록
  const deckChamps = useMemo(() =>
    deck.units.map((u) => ({
      championId: u.characterId.toLowerCase(),
      championName: getChampionName(u.imageUrl, locale, u.name),
      imageUrl: u.imageUrl,
    })),
    [deck.units, locale],
  )

  function addEntry() {
    const first = deckChamps[0]
    setEntries((prev) => [
      ...prev,
      { championId: first?.championId ?? '', championName: first?.championName ?? '', augmentName: '' },
    ])
  }

  function removeEntry(idx: number) {
    setEntries((prev) => prev.filter((_, i) => i !== idx))
  }

  function patchEntry(idx: number, patch: Partial<HeroAugmentEntry>) {
    setEntries((prev) => prev.map((e, i) => i === idx ? { ...e, ...patch } : e))
  }

  function handleChampChange(idx: number, championId: string) {
    const champ = deckChamps.find((c) => c.championId === championId)
    patchEntry(idx, { championId, championName: champ?.championName ?? championId })
  }

  async function handleSave() {
    setSaving(true)
    setSaveError('')
    const valid = entries.filter((e) => e.augmentName.trim())
    const json = valid.length > 0 ? JSON.stringify(valid) : null
    try {
      await onSave(json)
      onClose()
    } catch {
      setSaveError('저장에 실패했습니다. 다시 시도해 주세요.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={styles.modalOverlay} onClick={onClose}>
      <div className={styles.modalBox} onClick={(e) => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          <span className={styles.modalTitle}>영웅 증강 편집 — {deck.displayName}</span>
          <button className={styles.modalClose} onClick={onClose}>✕</button>
        </div>

        <div className={styles.heroAugEditorBody}>
          {entries.length === 0 && (
            <p className={styles.heroAugEmpty}>등록된 영웅 증강이 없습니다. 추가 버튼을 눌러 입력하세요.</p>
          )}
          {entries.map((entry, idx) => (
            <div key={idx} className={styles.heroAugEditorRow}>
              <select
                className={styles.heroAugSelect}
                value={entry.championId}
                onChange={(e) => handleChampChange(idx, e.target.value)}
              >
                {deckChamps.map((c) => (
                  <option key={c.championId} value={c.championId}>{c.championName}</option>
                ))}
              </select>
              <input
                className={styles.heroAugInput}
                placeholder="증강 이름 (예: 화약 소녀)"
                value={entry.augmentName}
                onChange={(e) => patchEntry(idx, { augmentName: e.target.value })}
              />
              <button className={styles.heroAugRemoveBtn} onClick={() => removeEntry(idx)}>✕</button>
            </div>
          ))}
        </div>

        <div className={styles.modalFooter}>
          {saveError && (
            <span
              className={styles.saveErrorMsg}
              role="alert"
              aria-live="polite"
              aria-atomic="true"
            >
              {saveError}
            </span>
          )}
          <button className={styles.boardBtn} onClick={addEntry}>+ 증강 추가</button>
          <button className={styles.resetBtn} onClick={() => setEntries([])}>전체 초기화</button>
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
  heroAugEditorOpen: boolean
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
    heroAugEditorOpen: false,
  })

  function markDirty(patch: Partial<DeckRowState>) {
    setState((s) => ({ ...s, ...patch, dirty: true }))
  }

  function buildRequest(
    boardPositions?: string | null,
    playGuide?: string | null,
    heroAugments?: string | null,
  ): DeckCurationRequest {
    return {
      customName: state.customName.trim() || null,
      hidden: state.hidden,
      sortPriority: state.sortPriority !== '' ? Number(state.sortPriority) : null,
      curatorNote: null,
      boardPositions: boardPositions !== undefined ? boardPositions : deck.boardPositions,
      playGuide: playGuide !== undefined ? playGuide : deck.playGuide,
      heroAugments: heroAugments !== undefined ? heroAugments : deck.heroAugments,
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

  async function handleHeroAugSave(heroAugmentsJson: string | null) {
    const updated = await updateDeckCuration(deck.id, buildRequest(undefined, undefined, heroAugmentsJson))
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
      heroAugEditorOpen: false,
    })
    onSaved({ ...deck, customName: null, displayName: deck.autoName, hidden: false, sortPriority: null, boardPositions: null, playGuide: null, heroAugments: null })
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
          <button
            className={`${styles.boardBtn} ${deck.heroAugments ? styles.boardBtnActive : ''}`}
            onClick={() => setState((s) => ({ ...s, heroAugEditorOpen: true }))}
          >
            영웅증강{deck.heroAugments ? ' ✓' : ''}
          </button>
          {(deck.customName != null || deck.hidden || deck.sortPriority != null) && (
            <button className={styles.resetBtn} onClick={handleReset}>초기화</button>
          )}
        </td>
      </tr>

      {state.boardEditorOpen && (
        <BoardEditorModal
          deck={deck}
          locale={locale}
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
      {state.heroAugEditorOpen && (
        <HeroAugmentModal
          deck={deck}
          locale={locale}
          onClose={() => setState((s) => ({ ...s, heroAugEditorOpen: false }))}
          onSave={handleHeroAugSave}
        />
      )}
    </>
  )
}

/* ── 영웅증강 덱 폼 모달 ── */
const EMPTY_HA_PAYLOAD: HeroAugmentDeckPayload = {
  name: '',
  description: null,
  champions: null,
  traits: null,
  boardPositions: null,
  heroAugments: null,
  recommended: true,
  sortOrder: 0,
  grade: null,
}

interface HaFormModalProps {
  initial: HeroAugmentDeckItem | null
  onClose: () => void
  onSaved: (item: HeroAugmentDeckItem) => void
}

function HaFormModal({ initial, onClose, onSaved }: HaFormModalProps) {
  const [payload, setPayload] = useState<HeroAugmentDeckPayload>(
    initial
      ? {
          name: initial.name,
          description: initial.description,
          champions: initial.champions,
          traits: initial.traits,
          boardPositions: initial.boardPositions,
          heroAugments: initial.heroAugments,
          recommended: initial.recommended,
          sortOrder: initial.sortOrder,
          grade: initial.grade,
        }
      : EMPTY_HA_PAYLOAD,
  )
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  function patch<K extends keyof HeroAugmentDeckPayload>(key: K, value: HeroAugmentDeckPayload[K]) {
    setPayload((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSave() {
    if (!payload.name.trim()) { setError('덱 이름은 필수입니다.'); return }
    setSaving(true); setError('')
    try {
      const result = initial
        ? await updateHeroAugmentDeck(initial.id, payload)
        : await createHeroAugmentDeck(payload)
      onSaved(result)
    } catch {
      setError('저장 실패. 토큰 또는 입력값을 확인하세요.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={styles.modalOverlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <h3 className={styles.modalTitle}>{initial ? '영웅증강 덱 수정' : '영웅증강 덱 추가'}</h3>

        <label className={styles.fieldLabel}>덱 이름 *</label>
        <input
          className={styles.textInput}
          value={payload.name}
          onChange={(e) => patch('name', e.target.value)}
          placeholder="예: (꽁) 나서스 덱"
        />

        <label className={styles.fieldLabel}>설명</label>
        <textarea
          className={styles.textarea}
          value={payload.description ?? ''}
          onChange={(e) => patch('description', e.target.value || null)}
          placeholder="운영 방법, 핵심 조합 등 간략 설명"
          rows={2}
        />

        <label className={styles.fieldLabel}>티어 (grade)</label>
        <select
          className={styles.rankSelect}
          value={payload.grade ?? ''}
          onChange={(e) => patch('grade', e.target.value || null)}
        >
          <option value="">선택 안 함</option>
          {['S', 'A', 'B', 'C', 'D'].map((g) => <option key={g} value={g}>{g}</option>)}
        </select>

        <label className={styles.fieldLabel}>정렬 순서</label>
        <input
          type="number"
          className={styles.textInput}
          value={payload.sortOrder}
          onChange={(e) => patch('sortOrder', Number(e.target.value))}
        />

        <label className={styles.fieldLabel}>영웅증강 JSON</label>
        <textarea
          className={styles.textarea}
          value={payload.heroAugments ?? ''}
          onChange={(e) => patch('heroAugments', e.target.value || null)}
          placeholder={'[{"championId":"tft17_nasus","championName":"나서스","augmentName":"꽁 나서스"}]'}
          rows={3}
        />

        <label className={styles.fieldLabel}>챔피언 JSON</label>
        <textarea
          className={styles.textarea}
          value={payload.champions ?? ''}
          onChange={(e) => patch('champions', e.target.value || null)}
          placeholder={'[{"characterId":"tft17_nasus","imageUrl":"...","stars":2}]'}
          rows={3}
        />

        <div className={styles.haCheckRow}>
          <input
            type="checkbox"
            id="ha-recommended"
            checked={payload.recommended}
            onChange={(e) => patch('recommended', e.target.checked)}
          />
          <label htmlFor="ha-recommended">추천 덱</label>
        </div>

        {error && <p className={styles.saveError}>{error}</p>}

        <div className={styles.modalBtns}>
          <button className={styles.cancelBtn} onClick={onClose}>취소</button>
          <button className={styles.saveBtn} onClick={handleSave} disabled={saving}>
            {saving ? '저장 중...' : '저장'}
          </button>
        </div>
      </div>
    </div>
  )
}

/* ── 영웅증강 덱 관리 ── */
export function HeroAugmentDeckManager() {
  const [decks, setDecks] = useState<HeroAugmentDeckItem[]>([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<HeroAugmentDeckItem | null | 'new'>(null)

  useEffect(() => {
    fetchAdminHeroAugmentDecks()
      .then(setDecks)
      .finally(() => setLoading(false))
  }, [])

  async function handleDelete(id: number) {
    if (!confirm('삭제하시겠습니까?')) return
    await deleteHeroAugmentDeck(id)
    setDecks((prev) => prev.filter((d) => d.id !== id))
  }

  function handleSaved(item: HeroAugmentDeckItem) {
    setDecks((prev) => {
      const exists = prev.find((d) => d.id === item.id)
      return exists ? prev.map((d) => (d.id === item.id ? item : d)) : [...prev, item]
    })
    setEditing(null)
  }

  return (
    <div>
      <div className={styles.toolbar}>
        <h2 className={styles.title}>영웅증강 덱 관리</h2>
        <button className={styles.saveBtn} onClick={() => setEditing('new')}>+ 덱 추가</button>
      </div>

      {loading ? (
        <p style={{ color: 'var(--text-muted)' }}>불러오는 중...</p>
      ) : decks.length === 0 ? (
        <p style={{ color: 'var(--text-muted)', marginTop: 24 }}>등록된 영웅증강 덱이 없습니다.</p>
      ) : (
        <table className={styles.table}>
          <thead>
            <tr>
              <th>ID</th>
              <th>덱 이름</th>
              <th>티어</th>
              <th>순서</th>
              <th>추천</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            {decks.map((deck) => (
              <tr key={deck.id}>
                <td style={{ color: 'var(--text-muted)', fontSize: 12 }}>{deck.id}</td>
                <td style={{ fontWeight: 600 }}>{deck.name}</td>
                <td>{deck.grade ?? '-'}</td>
                <td>{deck.sortOrder}</td>
                <td>{deck.recommended ? '✓' : '-'}</td>
                <td>
                  <div style={{ display: 'flex', gap: 6 }}>
                    <button className={styles.boardBtn} onClick={() => setEditing(deck)}>수정</button>
                    <button className={`${styles.boardBtn} ${styles.resetBtn}`} onClick={() => handleDelete(deck.id)}>삭제</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {editing !== null && (
        <HaFormModal
          initial={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={handleSaved}
        />
      )}
    </div>
  )
}


/* ── 메인 관리 페이지 ── */
export function AdminPage() {
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

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h1 className={styles.title}>메타덱 관리</h1>
      </div>

      {loading ? (
        <p style={{ color: 'var(--text-muted)' }}>불러오는 중...</p>
      ) : (
        <>
          <select
            className={styles.rankSelect}
            value={rankFilter}
            onChange={(e) => setRankFilter(e.target.value as RankFilter)}
          >
            {RANK_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
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
        </>
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
