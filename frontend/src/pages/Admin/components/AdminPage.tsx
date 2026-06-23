import { useState } from 'react'
import { useCDragonLocale } from '../../../hooks/useCDragonLocale'
import type { RankFilter } from '../../Dashboard/dashboardData'
import { useAdminDecks } from '../hooks/useAdminDecks'
import { triggerDeckAggregate, isAdminAuthFailure, isNetworkOrTimeoutError, getServerErrorStatus, getHttpStatus } from '../../../api/adminApi'
import styles from '../Admin.module.css'
import DeckRow from './DeckRow'

const RANK_OPTIONS: { label: string; value: RankFilter }[] = [
  { label: '마스터+', value: 'MASTER_PLUS' },
  { label: '다이아+', value: 'DIAMOND_PLUS' },
  { label: '에메랄드+', value: 'EMERALD_PLUS' },
]

function getAggregateErrorMessage(error: unknown): string {
  if (isAdminAuthFailure(error)) return '인증 실패: 관리자 토큰을 확인해 주세요.'
  if (isNetworkOrTimeoutError(error)) return '네트워크 오류: 연결 상태를 확인 후 다시 시도해 주세요.'
  const httpStatus = getHttpStatus(error)
  if (httpStatus === 409) return '집계가 이미 실행 중입니다. 완료 후 다시 시도해 주세요.'
  const status = getServerErrorStatus(error)
  if (status != null) return `서버 오류가 발생했습니다. (${status})`
  return '집계 요청에 실패했습니다.'
}

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
      setAggregateMsg({ ok: true, text: '집계 요청이 전송됐습니다. 완료까지 수 분이 소요될 수 있습니다.' })
    } catch (error) {
      setAggregateMsg({ ok: false, text: getAggregateErrorMessage(error) })
    } finally {
      setAggregating(false)
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h1 className={styles.title}>메타덱 관리</h1>
      </div>

      <div className={styles.aggregatePanel}>
        <span className={styles.aggregatePanelLabel}>덱 데이터 수집</span>
        <input
          type="date"
          className={styles.aggregateDateInput}
          value={aggregateDate}
          onChange={(e) => setAggregateDate(e.target.value)}
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
          <span className={aggregateMsg.ok ? styles.aggregateMsgOk : styles.aggregateMsgError}>
            {aggregateMsg.text}
          </span>
        )}
        {!aggregateDate && (
          <span className={styles.aggregateHint}>날짜 미입력 시 어제 기준으로 집계</span>
        )}
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
