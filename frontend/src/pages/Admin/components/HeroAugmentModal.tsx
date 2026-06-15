import { useMemo, useState } from 'react'
import type { AdminDeck, HeroAugmentEntry } from '../../../api/adminApi'
import { getChampionName, type TFTLocale } from '../../../api/cdragonLocale'
import styles from '../Admin.module.css'

interface HeroAugmentModalProps {
  deck: AdminDeck
  locale: TFTLocale | undefined
  onClose: () => void
  onSave: (json: string | null) => Promise<void>
}

export default function HeroAugmentModal({ deck, locale, onClose, onSave }: HeroAugmentModalProps) {
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
