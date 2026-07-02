import { Bot } from 'lucide-react'
import type { AugmentGuide } from '../../../api/guide'
import {
  EmptyState,
  GuideAssetImage,
} from './GuideShared'
import {
  createGameGuideAiRef,
  type GameGuideAiAskHandler,
} from '../utils/gameGuideAiRefs'
import styles from '../Guide.module.css'

interface AugmentGuideListProps {
  augments: AugmentGuide[]
  onGameGuideAiAsk: GameGuideAiAskHandler
}

function AugmentGuideList({
  augments,
  onGameGuideAiAsk,
}: AugmentGuideListProps) {
  return (
    <section className={styles.augmentListWrap} aria-label="증강체 목록">
      {augments.length > 0 && (
        <div className={styles.augmentCardGrid}>
          {augments.map((augment) => (
            <article className={styles.augmentCard} key={augment.name}>
              <div className={styles.augmentCardTop}>
                <GuideAssetImage
                  alt={`${augment.name} 아이콘`}
                  fallbackLabel={augment.name}
                  imageUrl={augment.imageUrl}
                />
                <h3>{augment.name}</h3>
                <button
                  aria-label={`${augment.name} AI 질문`}
                  className={styles.gameGuideAiCardButton}
                  onClick={() => onGameGuideAiAsk(createGameGuideAiRef('AUGMENT', augment.name, augment.targetKey))}
                  title="AI에게 물어보기"
                  type="button"
                >
                  <Bot size={14} />
                </button>
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
