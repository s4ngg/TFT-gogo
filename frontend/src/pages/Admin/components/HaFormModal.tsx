import { useState } from 'react'
import {
  createHeroAugmentDeck,
  updateHeroAugmentDeck,
  type HeroAugmentDeckItem,
  type HeroAugmentDeckPayload,
} from '../../../api/adminApi'
import styles from '../Admin.module.css'

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

export default function HaFormModal({ initial, onClose, onSaved }: HaFormModalProps) {
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

  function validateJsonField(
    value: string | null,
    label: string,
    expectedType?: 'array' | 'object',
  ): boolean {
    if (!value) return true
    let parsed: unknown
    try { parsed = JSON.parse(value) } catch { setError(`${label} 필드가 유효한 JSON이 아닙니다.`); return false }
    if (expectedType === 'array' && !Array.isArray(parsed)) {
      setError(`${label} 필드는 배열([...]) 형식이어야 합니다.`); return false
    }
    if (expectedType === 'object' && (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed))) {
      setError(`${label} 필드는 객체({...}) 형식이어야 합니다.`); return false
    }
    return true
  }

  async function handleSave() {
    if (!payload.name.trim()) { setError('덱 이름은 필수입니다.'); return }
    if (!validateJsonField(payload.heroAugments, '영웅증강 JSON', 'array')) return
    if (!validateJsonField(payload.champions, '챔피언 JSON', 'array')) return
    if (!validateJsonField(payload.traits, '특성 JSON', 'array')) return
    if (!validateJsonField(payload.boardPositions, '배치 JSON', 'object')) return
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
          onChange={(e) => {
            const nextSortOrder = Number.parseInt(e.target.value, 10)
            patch('sortOrder', Number.isFinite(nextSortOrder) ? nextSortOrder : 0)
          }}
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

        <label className={styles.fieldLabel}>특성 JSON</label>
        <textarea
          className={styles.textarea}
          value={payload.traits ?? ''}
          onChange={(e) => patch('traits', e.target.value || null)}
          placeholder={'[{"name":"신성","count":3}]'}
          rows={3}
        />

        <label className={styles.fieldLabel}>배치 JSON</label>
        <textarea
          className={styles.textarea}
          value={payload.boardPositions ?? ''}
          onChange={(e) => patch('boardPositions', e.target.value || null)}
          placeholder={'{"row0col0":"tft17_nasus"}'}
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
