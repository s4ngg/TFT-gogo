import { Bot } from 'lucide-react'
import { useEffect, useRef } from 'react'
import type { GuideCatalog, ItemStatGuide } from '../../../api/guide'
import { useGuideTabItems } from '../../../hooks/useGuide'
import {
  useGuidePageBounds,
  useGuideTabPagination,
} from '../hooks/useGuideTabPagination'
import { useGuideHighlightScroll } from '../hooks/useGuideHighlightScroll'
import {
  EmptyState,
  GuideAssetImage,
  GuidePagination,
  GuideStatusBanner,
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
import styles from '../Guide.module.css'

interface ItemStatsViewProps {
  fallbackData: GuideCatalog
  highlightedGuide: HighlightedGuide | null
  isGuideFallbackData: boolean
  isGuideFetching: boolean
  onGameGuideAiAsk: GameGuideAiAskHandler
  onGuideRetry: () => void
  onVisibleItemsChange: (items: ItemStatGuide[]) => void
  patchVersion: string
  query: string
}

const ITEM_GUIDE_PAGE_SIZE = 6

function ItemStatsView({
  fallbackData,
  highlightedGuide,
  isGuideFallbackData,
  isGuideFetching,
  onGameGuideAiAsk,
  onGuideRetry,
  onVisibleItemsChange,
  patchVersion,
  query,
}: ItemStatsViewProps) {
  const itemListRef = useRef<HTMLElement>(null)
  const {
    currentPage,
    setCurrentPage,
  } = useGuideTabPagination({ resetKey: query })
  const itemsQuery = useGuideTabItems({
    fallbackData,
    params: {
      page: currentPage,
      pageSize: ITEM_GUIDE_PAGE_SIZE,
      patchVersion,
      query,
      tab: 'items',
    },
  })
  const pageData = itemsQuery.data.data
  const safePage = useGuidePageBounds({
    currentPage,
    setCurrentPage,
    totalPages: pageData.totalPages,
  })
  const visibleItems = pageData.items
  const highlightWatchKey = getGuideHighlightWatchKey(visibleItems)

  useGuideHighlightScroll(itemListRef, 'items', highlightedGuide, highlightWatchKey)

  useEffect(() => {
    onVisibleItemsChange(visibleItems)
  }, [onVisibleItemsChange, visibleItems])

  return (
    <>
      <GuideStatusBanner
        isFallbackData={isGuideFallbackData || (itemsQuery.data.source === 'fallback' && !itemsQuery.isFetching)}
        isFetching={isGuideFetching || itemsQuery.isFetching}
        onRetry={() => {
          onGuideRetry()
          void itemsQuery.refetch()
        }}
      />
      <section className={styles.itemGuideList} aria-label="아이템 목록" ref={itemListRef}>
        {visibleItems.map((itemStat) => {
          const isHighlighted = isGuideHighlighted('items', itemStat, highlightedGuide)

          return (
            <article
              {...getGuideHighlightAttrs(isHighlighted, styles.itemGuideCard, styles.guideHighlighted)}
              key={itemStat.name}
            >
              <div className={styles.itemGuideTop}>
                <div className={styles.itemGuideIdentity}>
                  <GuideAssetImage
                    alt={itemStat.name}
                    fallbackLabel={itemStat.name}
                    imageUrl={itemStat.imageUrl}
                  />
                  <div>
                    <h3>{itemStat.name}</h3>
                  </div>
                </div>
                <button
                  aria-label={`${itemStat.name} AI 질문`}
                  className={styles.gameGuideAiCardButton}
                  onClick={() => onGameGuideAiAsk(createGameGuideAiRef('ITEM', itemStat.name, itemStat.targetKey))}
                  title="AI에게 물어보기"
                  type="button"
                >
                  <Bot size={14} />
                </button>
              </div>

              <p className={styles.itemGuideDescription}>
                {itemStat.description || '아이템 효과 정보가 아직 준비되지 않았습니다.'}
              </p>

              {itemStat.bestUsers.length > 0 && (
                <div className={styles.itemBestUsers}>
                  <strong>추천 챔피언</strong>
                  <div>
                    {itemStat.bestUsers.map((championRef) => (
                      <span key={championRef.name}>
                        <GuideAssetImage
                          decorative
                          fallbackLabel={championRef.name}
                          imageUrl={championRef.imageUrl}
                          title={championRef.name}
                        />
                        {championRef.name}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </article>
          )
        })}
        {visibleItems.length === 0 && <EmptyState />}
      </section>
      <GuidePagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />
    </>
  )
}

export default ItemStatsView
