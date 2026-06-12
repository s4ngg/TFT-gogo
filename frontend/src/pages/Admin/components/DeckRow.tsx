import { useState } from 'react'
import { resetDeckCuration, updateDeckCuration, type AdminDeck, type DeckCurationRequest } from '../../../api/adminApi'
import type { TFTLocale } from '../../../api/cdragonLocale'
import styles from '../Admin.module.css'
import { buildKoreanName } from '../utils/adminUtils'
import BoardEditorModal from './BoardEditorModal'
import HeroAugmentModal from './HeroAugmentModal'
import PlayGuideModal from './PlayGuideModal'

const TIER_COLOR: Record<string, string> = {
  S: '#04f3e5', A: '#a78bfa', B: '#60a5fa', C: '#818cf8', D: '#6b7280',
}

interface DeckRowState {
  customName: string
  hidden: boolean
  sortPriority: string
  dirty: boolean
  saving: boolean
  boardEditorOpen: boolean
  guideEditorOpen: boolean
  heroAugEditorOpen: boolean
  saveError: string
}


export default function DeckRow({ deck, onSaved, locale }: { deck: AdminDeck; onSaved: (updated: AdminDeck) => void; locale: TFTLocale | undefined }) {
  const [state, setState] = useState<DeckRowState>({
    customName: deck.customName ?? '',
    hidden: deck.hidden,
    sortPriority: deck.sortPriority != null ? String(deck.sortPriority) : '',
    dirty: false,
    saving: false,
    boardEditorOpen: false,
    guideEditorOpen: false,
    heroAugEditorOpen: false,
    saveError: '',
  })

  function markDirty(patch: Partial<DeckRowState>) {
    setState((s) => ({ ...s, ...patch, dirty: true, saveError: '' }))
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
    setState((s) => ({ ...s, saving: true, saveError: '' }))
    try {
      const updated = await updateDeckCuration(deck.id, buildRequest())
      onSaved(updated)
      setState((s) => ({ ...s, dirty: false, saving: false }))
    } catch {
      setState((s) => ({
        ...s,
        saving: false,
        saveError: '덱 큐레이션 저장에 실패했습니다. 다시 시도해 주세요.',
      }))
    }
  }

  async function saveCuration(
    boardPositions?: string | null,
    playGuide?: string | null,
    heroAugments?: string | null,
  ) {
    setState((s) => ({ ...s, saveError: '' }))
    try {
      const updated = await updateDeckCuration(deck.id, buildRequest(boardPositions, playGuide, heroAugments))
      onSaved(updated)
    } catch {
      const message = '덱 큐레이션 저장에 실패했습니다. 다시 시도해 주세요.'
      setState((s) => ({ ...s, saveError: message }))
      throw new Error(message)
    }
  }

  async function handleBoardSave(boardPositionsJson: string | null) {
    await saveCuration(boardPositionsJson)
  }

  async function handleGuideSave(playGuideJson: string | null) {
    await saveCuration(undefined, playGuideJson)
  }

  async function handleHeroAugSave(heroAugmentsJson: string | null) {
    await saveCuration(undefined, undefined, heroAugmentsJson)
  }

  async function handleReset() {
    if (!confirm('큐레이션을 초기화하고 자동 이름으로 되돌릴까요?')) return
    setState((s) => ({ ...s, saving: true, saveError: '' }))
    try {
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
        saveError: '',
      })
      onSaved({ ...deck, customName: null, displayName: deck.autoName, hidden: false, sortPriority: null, boardPositions: null, playGuide: null, heroAugments: null })
    } catch {
      setState((s) => ({
        ...s,
        saving: false,
        saveError: '큐레이션 초기화에 실패했습니다. 다시 시도해 주세요.',
      }))
    }
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
          {state.saveError && (
            <span
              className={styles.saveErrorMsg}
              role="alert"
              aria-live="polite"
              aria-atomic="true"
            >
              {state.saveError}
            </span>
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
