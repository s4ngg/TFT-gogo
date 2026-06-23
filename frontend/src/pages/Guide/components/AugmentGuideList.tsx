import type { AugmentGuide } from '../../../api/guide'
import { EmptyState } from './GuideShared'
import styles from '../Guide.module.css'

interface AugmentGuideListProps {
  augments: AugmentGuide[]
}

function AugmentGuideList({
  augments,
}: AugmentGuideListProps) {
  return (
    <section className={styles.augmentListWrap} aria-label="증강체 목록">
      {augments.length > 0 && (
        <div className={styles.augmentCardGrid}>
          {augments.map((augment) => (
            <article className={styles.augmentCard} key={augment.name}>
              <div className={styles.augmentCardTop}>
                {augment.imageUrl ? (
                  <img alt={`${augment.name} 아이콘`} src={augment.imageUrl} />
                ) : (
                  <span className={styles.augmentIconFallback} aria-hidden="true">증</span>
                )}
                <h3>{augment.name}</h3>
              </div>
              <p>{augment.description}</p>
              {augment.tags.length > 0 && (
                <div className={styles.augmentTagList}>
                  {augment.tags.map((tag) => <span key={tag}>{tag}</span>)}
                </div>
              )}
            </article>
          ))}
        </div>
      )}
      {augments.length === 0 && <EmptyState />}
    </section>
  )
}

export default AugmentGuideList
