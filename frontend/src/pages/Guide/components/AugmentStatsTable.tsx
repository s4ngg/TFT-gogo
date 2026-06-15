import type {
  AugmentGuide,
  MetricSortKey,
  SortDir,
} from '../../../api/guide'
import TierBadge from '../../../components/common/TierBadge'
import {
  EmptyState,
  SortHeaderButton,
} from './GuideShared'
import styles from '../Guide.module.css'

interface AugmentStatsTableProps {
  augments: AugmentGuide[]
  onSort: (sortKey: MetricSortKey) => void
  sortDir: SortDir
  sortKey: MetricSortKey
}

function AugmentStatsTable({
  augments,
  onSort,
  sortDir,
  sortKey,
}: AugmentStatsTableProps) {
  return (
    <section className={styles.tableWrap}>
      <table className={styles.augmentTable}>
        <thead>
          <tr>
            <th>티어</th>
            <th className={styles.nameCol}>증강체</th>
            <th>종류</th>
            <th>
              <SortHeaderButton
                active={sortKey === 'winRate'}
                direction={sortDir}
                label="승률"
                onClick={() => onSort('winRate')}
              />
            </th>
            <th>
              <SortHeaderButton
                active={sortKey === 'avgPlace'}
                direction={sortDir}
                label="평균 등수"
                onClick={() => onSort('avgPlace')}
              />
            </th>
            <th>
              <SortHeaderButton
                active={sortKey === 'pickRate'}
                direction={sortDir}
                label="픽률"
                onClick={() => onSort('pickRate')}
              />
            </th>
            <th className={styles.rewardCol}>보상</th>
          </tr>
        </thead>
        <tbody>
          {augments.map((augment) => (
            <tr key={augment.name}>
              <td><TierBadge value={augment.tier} /></td>
              <td className={styles.augmentNameCell}>
                <strong>{augment.name}</strong>
                <span>{augment.description}</span>
                <div>
                  {augment.tags.map((tag) => <b key={tag}>{tag}</b>)}
                </div>
              </td>
              <td>{augment.type}</td>
              <td className={styles.winRate}>{augment.winRate}</td>
              <td className={styles.avgPlace}>#{augment.avgPlace}</td>
              <td className={styles.pickRate}>{augment.pickRate}</td>
              <td className={styles.rewardCell}>{augment.reward}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {augments.length === 0 && <EmptyState />}
    </section>
  )
}

export default AugmentStatsTable
