import { useState } from 'react'
import { useCDragonLocale } from '../../../hooks/useCDragonLocale'
import type { RankFilter } from '../../Dashboard/dashboardData'
import { useAdminDecks } from '../hooks/useAdminDecks'
import { triggerDeckAggregate } from '../../../api/adminApi'
import styles from '../Admin.module.css'
import DeckRow from './DeckRow'

const RANK_OPTIONS: { label: string; value: RankFilter }[] = [
  { label: '마스터+', value: 'MASTER_PLUS' },
  { label: '다이아+', value: 'DIAMOND_PLUS' },
  { label: '에메랄드+', value: 'EMERALD_PLUS' },
]

export default function AdminPage() {
  const [rankFilter, setRankFilter] = useState<RankFilter>('MASTER_PLUS')
  const [aggregateDate, setAggregateDate] = useState('')
  const [aggregating, setAggregating] = useState(false)
  const [aggregateMsg, setAggregateMsg] = useState<{ ok: boolean; text: string } | null>(null)
  const { data: locale } = useCDragonLocale()
  const { decks, isLoading, updateDeck } = useAdminDecks(rankFilter)

  async function handleAggregate() {
    setAggregating(true)
    setAggregateMsg(null)
    try {
      await triggerDeckAggregate(aggregateDate || undefined)
      setAggregateMsg({ ok: true, text: '집계가 시작되었습니다.' })
    } catch {
      setAggregateMsg({ ok: false, text: '집계 요청에 실패했습니다.' })
    } finally {
      setAggregating(false)
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h1 className={styles.title}>메타덱 관리</h1>
      </div>

      <div className={styles.guideImportForm} style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
          <span className={styles.guideImportLabel}>덱 데이터 수집</span>
          <input
            type="date"
            className={styles.guideImportInput}
            value={aggregateDate}
            onChange={(e) => setAggregateDate(e.target.value)}
            style={{ width: 160 }}
            title="비워두면 어제 날짜로 집계"
          />
          <button
            className={styles.saveBtn}
            onClick={handleAggregate}
            disabled={aggregating}
          >
            {aggregating ? '집계 중...' : '집계 실행'}
          </button>
          {aggregateMsg && (
            <span style={{ fontSize: 13, color: aggregateMsg.ok ? 'var(--accent)' : 'var(--danger)' }}>
              {aggregateMsg.text}
            </span>
          )}
          {!aggregateDate && (
            <span className={styles.mutedText} style={{ fontSize: 12 }}>날짜 미입력 시 어제 기준으로 집계</span>
          )}
        </div>
      </div>

      {isLoading ? (
        <p className={styles.mutedText}>불러오는 중...</p>
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
