import { useState } from 'react'
import {
  deleteHeroAugmentDeck,
  isAdminAuthFailure,
  isNetworkOrTimeoutError,
  getServerErrorStatus,
  type HeroAugmentDeckItem,
} from '../../../api/adminApi'

function getLoadErrorMessage(error: unknown): string {
  if (isAdminAuthFailure(error)) return '인증 실패: 관리자 토큰을 확인해 주세요.'
  if (isNetworkOrTimeoutError(error)) return '네트워크 오류: 연결 상태를 확인 후 다시 시도해 주세요.'
  const status = getServerErrorStatus(error)
  if (status != null) return `서버 오류가 발생했습니다. (${status})`
  return '영웅증강 덱 목록을 불러오지 못했습니다.'
}
import { useAdminHeroAugmentDecks } from '../hooks/useAdminHeroAugmentDecks'
import styles from '../Admin.module.css'
import HaFormModal from './HaFormModal'

export default function HeroAugmentDeckManager() {
  const [editing, setEditing] = useState<HeroAugmentDeckItem | null | 'new'>(null)
  const { decks, error, isError, isLoading, refetch, removeDeck, upsertDeck } = useAdminHeroAugmentDecks()
  const [deleteError, setDeleteError] = useState('')

  async function handleDelete(id: number) {
    if (!confirm('삭제하시겠습니까?')) return
    setDeleteError('')
    try {
      await deleteHeroAugmentDeck(id)
      removeDeck(id)
    } catch {
      setDeleteError('영웅증강 덱 삭제에 실패했습니다. 다시 시도해 주세요.')
    }
  }

  function handleSaved(item: HeroAugmentDeckItem) {
    upsertDeck(item)
    setEditing(null)
  }

  return (
    <div>
      <div className={styles.toolbar}>
        <h2 className={styles.title}>영웅증강 덱 관리</h2>
        <button className={styles.saveBtn} onClick={() => setEditing('new')}>+ 덱 추가</button>
      </div>

      {deleteError && (
        <p className={styles.saveError} role="alert" aria-live="polite">{deleteError}</p>
      )}

      {isLoading ? (
        <p className={styles.mutedText}>불러오는 중...</p>
      ) : isError ? (
        <div>
          <p className={styles.saveError} role="alert" aria-live="polite">
            {getLoadErrorMessage(error)}
          </p>
          <button className={styles.boardBtn} onClick={() => refetch()}>다시 불러오기</button>
        </div>
      ) : decks.length === 0 ? (
        <p className={styles.emptyText}>등록된 영웅증강 덱이 없습니다.</p>
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
                <td className={styles.tableId}>{deck.id}</td>
                <td className={styles.strongText}>{deck.name}</td>
                <td>{deck.grade ?? '-'}</td>
                <td>{deck.sortOrder}</td>
                <td>{deck.recommended ? '✓' : '-'}</td>
                <td>
                  <div className={styles.tableActions}>
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
