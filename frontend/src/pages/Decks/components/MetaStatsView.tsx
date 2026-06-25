import { Fragment, useState } from 'react'
import TierBadge from '../../../components/common/TierBadge'
import type { TFTLocale } from '../../../api/cdragonLocale'
import type { MetaDeck, RankFilter } from '../../Dashboard/dashboardData'
import { TIER_META, TIER_ORDER } from '../../../constants/tiers'
import { sortDecks } from '../utils/deckListUtils'
import type { SortKey, SortDir } from '../utils/deckListUtils'
import DeckRow from './DeckRow'
import TableHead from './TableHead'
import styles from '../Decks.module.css'


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
        <TableHead sortKey={sortKey} sortDir={sortDir} onSort={handleSort} showTier={false} showRank={false} />
        <tbody>
          {TIER_ORDER.map((tier) => {
            const tierDecks = sortDecks(safeDecks.filter((d) => d.grade === tier), sortKey, sortDir)
            if (tierDecks.length === 0) return null
            const { color, label } = TIER_META[tier]
            return (
              <Fragment key={tier}>
                <tr className={styles.tierHeaderRow}>
                  <td colSpan={5}>
                    <span className={styles.tierHeaderInner} style={{ borderLeftColor: color }}>
                      <TierBadge value={tier} />
                      <span className={styles.tierName} style={{ color }}>{tier} 티어</span>
                      <span className={styles.tierDesc}>{label}</span>
                      <span className={styles.tierCount}>{tierDecks.length}개</span>
                    </span>
                  </td>
                </tr>
                {tierDecks.map((d) => (
                  <DeckRow key={d.rank} deck={d} showTier={false} showRank={false} locale={locale} rankFilter={rankFilter} />
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
