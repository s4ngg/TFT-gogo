import { useState } from 'react'
import type { AdminDeck, PlayGuide } from '../../../api/adminApi'
import styles from '../Admin.module.css'

interface PlayGuideModalProps {
  deck: AdminDeck
  onClose: () => void
  onSave: (guide: string | null) => Promise<void>
}

export default function PlayGuideModal({ deck, onClose, onSave }: PlayGuideModalProps) {
  const initial = (): PlayGuide => {
    if (!deck.playGuide) return { early: '', mid: '', late: '' }
    try { return JSON.parse(deck.playGuide) as PlayGuide } catch { return { early: '', mid: '', late: '' } }
  }
  const [guide, setGuide] = useState<PlayGuide>(initial)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState('')

  function patch(key: keyof PlayGuide, value: string) {
    setGuide((g) => ({ ...g, [key]: value }))
  }

  async function handleSave() {
    setSaving(true)
    setSaveError('')
    const isEmpty = !guide.early.trim() && !guide.mid.trim() && !guide.late.trim()
    const json = isEmpty ? null : JSON.stringify(guide)
    try {
      await onSave(json)
      onClose()
    } catch {
      setSaveError('운영방법 저장에 실패했습니다. 다시 시도해 주세요.')
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
