import { useState } from 'react'
import { Star } from 'lucide-react'
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
import ChampionDetailDialog from './ChampionDetailDialog'
import {
  EmptyState,
  GuideChampionImage,
  GuidePagination,
  GuideStatusBanner,
  ItemIconStrip,
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
          <article
            className={styles.championCard}
            key={championGuide.name}
            onClick={() => {
              openChampionDetail(championGuide)
            }}
            onKeyDown={(event) => {
              if (event.target !== event.currentTarget) return
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                openChampionDetail(championGuide)
              }
            }}
            role="button"
            tabIndex={0}
          >
            <button
              aria-pressed={favoriteChampions.includes(championGuide.name)}
              className={`${styles.favoriteButton} ${favoriteChampions.includes(championGuide.name) ? styles.favoriteActive : ''}`}
              onClick={(event) => {
                event.stopPropagation()
                onFavoriteToggle(championGuide.name)
              }}
              onKeyDown={(event) => {
                event.stopPropagation()
              }}
              title={favoriteChampions.includes(championGuide.name) ? '즐겨찾기 해제' : '즐겨찾기 추가'}
              type="button"
            >
              <Star size={14} />
            </button>
            <div className={styles.championPortrait}>
              <GuideChampionImage imageUrl={championGuide.imageUrl} name={championGuide.name} />
              <span className={styles.championCostBadge}>{championGuide.cost}</span>
            </div>
            <div className={styles.championInfo}>
              <strong>{championGuide.name}</strong>
              <span>{championGuide.role}</span>
            </div>
            <ItemIconStrip items={championGuide.bestItems} onItemSelect={onItemSelect} />
            <div className={styles.championTooltip} role="tooltip">
              <div className={styles.tooltipTop}>
                <GuideChampionImage decorative imageUrl={championGuide.imageUrl} name={championGuide.name} />
                <div>
                  <strong>{championGuide.name}</strong>
                  <span>{championGuide.traits.join(' / ')}</span>
                </div>
              </div>
              <div className={styles.tooltipItems}>
                <b>3신기</b>
                <ItemIconStrip items={championGuide.bestItems} onItemSelect={onItemSelect} />
              </div>
              <dl className={styles.statGrid}>
                <div><dt>체력</dt><dd>{championGuide.stats.hp}</dd></div>
                <div><dt>공격력</dt><dd>{championGuide.stats.ad}</dd></div>
                <div><dt>공속</dt><dd>{championGuide.stats.attackSpeed}</dd></div>
                <div><dt>마나</dt><dd>{championGuide.stats.mana}</dd></div>
                <div><dt>방어</dt><dd>{championGuide.stats.armor}</dd></div>
                <div><dt>마저</dt><dd>{championGuide.stats.mr}</dd></div>
              </dl>
              <p>권장 배치: {championGuide.position}</p>
            </div>
          </article>
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
