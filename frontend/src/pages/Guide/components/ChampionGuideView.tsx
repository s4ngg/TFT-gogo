import { useState } from 'react'
import {
  CHAMPION_PAGE_SIZE,
  type ChampionCostFilter,
  type GuideCatalog,
} from '../../../api/guide'
import { useGuideTabItems } from '../../../hooks/useGuide'
import { useChampionDetailDialog } from '../hooks/useChampionDetailDialog'
import {
  useGuidePageBounds,
  useGuideTabPagination,
} from '../hooks/useGuideTabPagination'
import ChampionGuideCard from './ChampionGuideCard'
import ChampionDetailDialog from './ChampionDetailDialog'
import {
  EmptyState,
  GuidePagination,
  GuideStatusBanner,
} from './GuideShared'
import styles from '../Guide.module.css'

interface ChampionGuideViewProps {
  fallbackData: GuideCatalog
  favoriteChampions: string[]
  onChampionOpen: (championName: string) => void
  onFavoriteToggle: (championName: string) => void
  onItemSelect: (itemName: string) => void
  query: string
}

function ChampionGuideView({
  fallbackData,
  favoriteChampions,
  onChampionOpen,
  onFavoriteToggle,
  onItemSelect,
  query,
}: ChampionGuideViewProps) {
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

  return (
    <>
      <GuideStatusBanner
        isFallbackData={championsQuery.data.source === 'fallback' && !championsQuery.isFetching}
        isFetching={championsQuery.isFetching}
        onRetry={() => {
          void championsQuery.refetch()
        }}
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
      <section className={styles.championGrid}>
        {visibleChampions.length === 0 && <EmptyState />}
        {visibleChampions.map((championGuide) => (
          <ChampionGuideCard
            championGuide={championGuide}
            isFavorite={favoriteChampions.includes(championGuide.name)}
            key={championGuide.name}
            onFavoriteToggle={onFavoriteToggle}
            onItemSelect={onItemSelect}
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
          onItemSelect={(itemName) => {
            closeChampionDetail()
            onItemSelect(itemName)
          }}
        />
      )}
    </>
  )
}

export default ChampionGuideView
