import { useState } from 'react'
import { useCDragonLocale } from '../../../hooks/useCDragonLocale'
import type { RankFilter } from '../../Dashboard/dashboardData'
import { useAdminDecks } from '../hooks/useAdminDecks'
import styles from '../Admin.module.css'
import DeckRow from './DeckRow'

const RANK_OPTIONS: { label: string; value: RankFilter }[] = [
  { label: '마스터+', value: 'MASTER_PLUS' },
  { label: '다이아+', value: 'DIAMOND_PLUS' },
  { label: '에메랄드+', value: 'EMERALD_PLUS' },
]

export default function AdminPage() {
  const [rankFilter, setRankFilter] = useState<RankFilter>('MASTER_PLUS')
  const { data: locale } = useCDragonLocale()
  const { decks, isLoading, updateDeck } = useAdminDecks(rankFilter)

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h1 className={styles.title}>메타덱 관리</h1>
      </div>

      {isLoading ? (
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
                <DeckRow key={deck.id} deck={deck} onSaved={updateDeck} locale={locale} />
              ))}
            </tbody>
          </table>
        </>
      )}
    </div>
  )
}
