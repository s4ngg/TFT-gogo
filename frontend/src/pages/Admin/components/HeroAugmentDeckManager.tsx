import { useState } from 'react'
import {
  deleteHeroAugmentDeck,
  type HeroAugmentDeckItem,
} from '../../../api/adminApi'
import { useAdminHeroAugmentDecks } from '../hooks/useAdminHeroAugmentDecks'
import styles from '../Admin.module.css'
import HaFormModal from './HaFormModal'

export function HeroAugmentDeckManager() {
  const [editing, setEditing] = useState<HeroAugmentDeckItem | null | 'new'>(null)
  const { decks, isLoading, removeDeck, upsertDeck } = useAdminHeroAugmentDecks()

  async function handleDelete(id: number) {
    if (!confirm('삭제하시겠습니까?')) return
    await deleteHeroAugmentDeck(id)
    removeDeck(id)
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

      {isLoading ? (
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
