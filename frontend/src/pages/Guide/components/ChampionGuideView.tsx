import { useEffect, useRef, useState } from 'react'
import {
  CHAMPION_PAGE_SIZE,
  type ChampionGuide,
  type ChampionCostFilter,
  type GuideCatalog,
} from '../../../api/guide'
import { useGuideTabItems } from '../../../hooks/useGuide'
import { useChampionDetailDialog } from '../hooks/useChampionDetailDialog'
import {
  useGuidePageBounds,
  useGuideTabPagination,
} from '../hooks/useGuideTabPagination'
import { useGuideHighlightScroll } from '../hooks/useGuideHighlightScroll'
import ChampionGuideCard from './ChampionGuideCard'
import ChampionDetailDialog from './ChampionDetailDialog'
import {
  EmptyState,
  GuidePagination,
  GuideStatusBanner,
} from './GuideShared'
import type { GameGuideAiAskHandler } from '../utils/gameGuideAiRefs'
import {
  getGuideHighlightWatchKey,
  type HighlightedGuide,
  isGuideHighlighted,
} from '../utils/guideHighlight'
import styles from '../Guide.module.css'

interface ChampionGuideViewProps {
  fallbackData: GuideCatalog
  favoriteChampions: string[]
  highlightedGuide: HighlightedGuide | null
  isGuideFallbackData: boolean
  isGuideFetching: boolean
  onChampionOpen: (championName: string) => void
  onFavoriteToggle: (championName: string) => void
  onGameGuideAiAsk: GameGuideAiAskHandler
  onGuideRetry: () => void
  onVisibleItemsChange: (items: ChampionGuide[]) => void
  patchVersion: string
  query: string
}

function ChampionGuideView({
  fallbackData,
  favoriteChampions,
  highlightedGuide,
  isGuideFallbackData,
  isGuideFetching,
  onChampionOpen,
  onFavoriteToggle,
  onGameGuideAiAsk,
  onGuideRetry,
  onVisibleItemsChange,
  patchVersion,
  query,
}: ChampionGuideViewProps) {
  const championGridRef = useRef<HTMLElement>(null)
  const [costFilter, setCostFilter] = useState<ChampionCostFilter>('all')
  const {
    currentPage,
    setCurrentPage,
  } = useGuideTabPagination({ resetKey: `${costFilter}:${query}` })
  const {
    closeChampionDetail,
    openChampionDetail,
    selectedChampion,
  } = useChampionDetailDialog(onChampionOpen)
  const championsQuery = useGuideTabItems({
    fallbackData,
    params: {
      cost: costFilter,
      page: currentPage,
      pageSize: CHAMPION_PAGE_SIZE,
      patchVersion,
      query,
      tab: 'champions',
    },
  })
  const pageData = championsQuery.data.data
  const safePage = useGuidePageBounds({
    currentPage,
    setCurrentPage,
    totalPages: pageData.totalPages,
  })
  const visibleChampions = pageData.items
  const isUnavailableData = championsQuery.data.source === 'unavailable' && !championsQuery.isFetching
  const highlightWatchKey = getGuideHighlightWatchKey(visibleChampions)

  useGuideHighlightScroll(championGridRef, 'champions', highlightedGuide, highlightWatchKey)

  useEffect(() => {
    onVisibleItemsChange(visibleChampions)
  }, [onVisibleItemsChange, visibleChampions])

  return (
    <>
      <GuideStatusBanner
        isFallbackData={!isUnavailableData && (isGuideFallbackData || (championsQuery.data.source === 'fallback' && !championsQuery.isFetching))}
        isFetching={isGuideFetching || championsQuery.isFetching}
        isUnavailableData={isUnavailableData}
        onRetry={() => {
          onGuideRetry()
          void championsQuery.refetch()
        }}
        patchVersion={championsQuery.data.patchVersion || patchVersion}
      />
      <div className={styles.costFilter} aria-label="챔피언 비용 필터">
        {(['all', 1, 2, 3, 4, 5] as const).map((cost) => (
          <button
            className={costFilter === cost ? styles.costActive : ''}
            key={cost}
            onClick={() => setCostFilter(cost)}
            aria-pressed={costFilter === cost}
            type="button"
          >
            {cost === 'all' ? '전체' : `${cost}코스트`}
          </button>
        ))}
      </div>
      <section className={styles.championGrid} ref={championGridRef}>
        {visibleChampions.length === 0 && <EmptyState />}
        {visibleChampions.map((championGuide) => (
          <ChampionGuideCard
            championGuide={championGuide}
            isFavorite={favoriteChampions.includes(championGuide.name)}
            isHighlighted={isGuideHighlighted('champions', championGuide, highlightedGuide)}
            key={championGuide.name}
            onFavoriteToggle={onFavoriteToggle}
            onGameGuideAiAsk={onGameGuideAiAsk}
            onOpen={openChampionDetail}
          />
        ))}
      </section>
      <GuidePagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />
      {selectedChampion && (
        <ChampionDetailDialog
          champion={selectedChampion}
          isFavorite={favoriteChampions.includes(selectedChampion.name)}
          onClose={closeChampionDetail}
          onFavoriteToggle={onFavoriteToggle}
        />
      )}
    </>
  )
}

export default ChampionGuideView
