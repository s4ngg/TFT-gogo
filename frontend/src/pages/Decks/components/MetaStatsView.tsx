import { Fragment, useState } from 'react'
import TierBadge from '../../../components/common/TierBadge'
import type { TFTLocale } from '../../../api/cdragonLocale'
import type { MetaDeck, RankFilter } from '../../Dashboard/dashboardData'
import type { TierBadgeValue } from '../../../types/badges'
import { sortDecks } from '../utils/deckListUtils'
import type { SortKey, SortDir } from '../utils/deckListUtils'
import DeckRow from './DeckRow'
import TableHead from './TableHead'
import styles from '../Decks.module.css'

const TIER_ORDER: TierBadgeValue[] = ['S', 'A', 'B', 'C', 'D']
const TIER_COLOR: Record<TierBadgeValue, string> = {
  S: '#04f3e5', A: '#a78bfa', B: '#60a5fa', C: '#818cf8', D: '#6b7280',
}

const TIER_DESC: Record<TierBadgeValue, string> = {
  S: '최상위 픽 · 강력 추천',
  A: '중상위권 범용 덱',
  B: '중위권 상황 의존적',
  C: '하위권 전문 운영 필요',
  D: '비추천 · 낮은 안정성',
}

interface MetaStatsViewProps {
  decks: MetaDeck[]
  locale: TFTLocale | undefined
  rankFilter: RankFilter
}

function MetaStatsView({ decks, locale, rankFilter }: MetaStatsViewProps) {
  const [sortKey, setSortKey] = useState<SortKey>('pickRate')
  const [sortDir, setSortDir] = useState<SortDir>('desc')

  function handleSort(key: SortKey) {
    if (sortKey === key) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    else { setSortKey(key); setSortDir(key === 'avgPlace' ? 'asc' : 'desc') }
  }

  const safeDecks = Array.isArray(decks) ? decks : []

  return (
    <div className={styles.tableWrap}>
      <table className={styles.table}>
        <TableHead sortKey={sortKey} sortDir={sortDir} onSort={handleSort} showTier showRank={false} />
        <tbody>
          {TIER_ORDER.map((tier) => {
            const tierDecks = sortDecks(safeDecks.filter((d) => d.grade === tier), sortKey, sortDir)
            if (tierDecks.length === 0) return null
            const color = TIER_COLOR[tier]
            return (
              <Fragment key={tier}>
                <tr className={styles.tierHeaderRow}>
                  <td colSpan={7}>
                    <span className={styles.tierHeaderInner} style={{ borderLeftColor: color }}>
                      <TierBadge value={tier} />
                      <span className={styles.tierName} style={{ color }}>{tier} 티어</span>
                      <span className={styles.tierDesc}>{TIER_DESC[tier]}</span>
                      <span className={styles.tierCount}>{tierDecks.length}개</span>
                    </span>
                  </td>
                </tr>
                {tierDecks.map((d) => (
                  <DeckRow key={d.rank} deck={d} showTier showRank={false} locale={locale} rankFilter={rankFilter} />
                ))}
              </Fragment>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

export default MetaStatsView
