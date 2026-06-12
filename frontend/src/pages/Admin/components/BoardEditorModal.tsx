import { useCallback, useMemo, useState } from 'react'
import type { AdminDeck } from '../../../api/adminApi'
import type { TFTLocale } from '../../../api/cdragonLocale'
import { tftChampSquareUrl, tftItemIconUrl } from '../../../api/communityDragonAssets'
import styles from '../Admin.module.css'
import {
  BOARD_COLS,
  BOARD_LEVELS,
  BOARD_ROWS,
  COST_COLORS,
  isCompleteItem,
  parseLevelBoards,
  serializeLevelBoards,
  type CellPos,
  type ChampInfo,
  type LevelBoards,
} from '../utils/adminUtils'

interface BoardEditorProps {
  deck: AdminDeck
  locale: TFTLocale | undefined
  onClose: () => void
  onSave: (boardPositionsJson: string | null) => Promise<void>
}

export default function BoardEditorModal({ deck, locale, onClose, onSave }: BoardEditorProps) {
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
