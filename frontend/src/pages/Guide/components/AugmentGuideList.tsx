import { Bot } from 'lucide-react'
import { useRef } from 'react'
import type { AugmentGuide } from '../../../api/guide'
import {
  EmptyState,
  GuideAssetImage,
} from './GuideShared'
import {
  createGameGuideAiRef,
  type GameGuideAiAskHandler,
} from '../utils/gameGuideAiRefs'
import {
  getGuideHighlightAttrs,
  getGuideHighlightWatchKey,
  type HighlightedGuide,
  isGuideHighlighted,
} from '../utils/guideHighlight'
import { useGuideHighlightScroll } from '../hooks/useGuideHighlightScroll'
import styles from '../Guide.module.css'

interface AugmentGuideListProps {
  augments: AugmentGuide[]
  highlightedGuide: HighlightedGuide | null
  onGameGuideAiAsk: GameGuideAiAskHandler
}

function AugmentGuideList({
  augments,
  highlightedGuide,
  onGameGuideAiAsk,
}: AugmentGuideListProps) {
  const augmentListRef = useRef<HTMLElement>(null)
  const highlightWatchKey = getGuideHighlightWatchKey(augments)

  useGuideHighlightScroll(augmentListRef, 'augments', highlightedGuide, highlightWatchKey)

  return (
    <section className={styles.augmentListWrap} aria-label="증강체 목록" ref={augmentListRef}>
      {augments.length > 0 && (
        <div className={styles.augmentCardGrid}>
          {augments.map((augment) => {
            const isHighlighted = isGuideHighlighted('augments', augment, highlightedGuide)

            return (
              <article
                {...getGuideHighlightAttrs(isHighlighted, styles.augmentCard, styles.guideHighlighted)}
                key={augment.name}
              >
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
            )
          })}
        </div>
      )}
      {augments.length === 0 && <EmptyState />}
    </section>
  )
}

export default AugmentGuideList
