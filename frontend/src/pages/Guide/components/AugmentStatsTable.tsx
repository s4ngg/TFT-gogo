import type { AugmentGuide } from '../../../api/guide'
import TierBadge from '../../../components/common/TierBadge'
import { EmptyState } from './GuideShared'
import styles from '../Guide.module.css'

interface AugmentStatsTableProps {
  augments: AugmentGuide[]
}

function AugmentStatsTable({
  augments,
}: AugmentStatsTableProps) {
  return (
    <section className={styles.tableWrap}>
      <table className={styles.augmentTable}>
        <thead>
          <tr>
            <th>티어</th>
            <th className={styles.nameCol}>증강체</th>
            <th>유형</th>
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
                {augment.tags.length > 0 && (
                  <div>
                    {augment.tags.map((tag) => <b key={tag}>{tag}</b>)}
                  </div>
                )}
              </td>
              <td>{augment.type}</td>
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
