import { useEffect, useRef, useState } from 'react'
import { Star } from 'lucide-react'
import {
  CHAMPION_PAGE_SIZE,
  type ChampionCostFilter,
  type ChampionGuide,
  type GuideCatalog,
} from '../../../api/guide'
import { useGuideTabItems } from '../../../hooks/useGuide'
import ChampionDetailDialog from './ChampionDetailDialog'
import {
  EmptyState,
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
  const [currentPage, setCurrentPage] = useState(1)
  const [selectedChampion, setSelectedChampion] = useState<ChampionGuide | null>(null)
  const lastFocusedElementRef = useRef<HTMLElement | null>(null)
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
  const safePage = Math.min(currentPage, pageData.totalPages)
  const visibleChampions = pageData.items

  useEffect(() => {
    setCurrentPage(1)
  }, [costFilter, query])

  useEffect(() => {
    if (currentPage > pageData.totalPages) setCurrentPage(pageData.totalPages)
  }, [currentPage, pageData.totalPages])

  function openChampionDetail(championGuide: ChampionGuide) {
    lastFocusedElementRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
    setSelectedChampion(championGuide)
    onChampionOpen(championGuide.name)
  }

  function closeChampionDetail() {
    setSelectedChampion(null)
    window.requestAnimationFrame(() => {
      if (lastFocusedElementRef.current?.isConnected) {
        lastFocusedElementRef.current.focus()
      }
    })
  }

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
          >
            <button
              aria-pressed={favoriteChampions.includes(championGuide.name)}
              className={`${styles.favoriteButton} ${favoriteChampions.includes(championGuide.name) ? styles.favoriteActive : ''}`}
              onClick={() => {
                onFavoriteToggle(championGuide.name)
              }}
              title={favoriteChampions.includes(championGuide.name) ? '즐겨찾기 해제' : '즐겨찾기 추가'}
              type="button"
            >
              <Star size={14} />
            </button>
            <button
              aria-label={`${championGuide.name} 상세 보기`}
              className={styles.championDetailButton}
              onClick={() => {
                openChampionDetail(championGuide)
              }}
              type="button"
            >
              <span className={styles.championPortrait}>
                <img src={championGuide.imageUrl} alt={championGuide.name} />
                <span>{championGuide.cost}</span>
              </span>
              <span className={styles.championInfo}>
                <strong>{championGuide.name}</strong>
                <span>{championGuide.role}</span>
              </span>
            </button>
            <ItemIconStrip items={championGuide.bestItems} onItemSelect={onItemSelect} />
            <div className={styles.championTooltip} role="tooltip">
              <div className={styles.tooltipTop}>
                <img src={championGuide.imageUrl} alt="" />
                <div>
                  <strong>{championGuide.name}</strong>
                  <span>{championGuide.traits.join(' / ')}</span>
                </div>
              </div>
              <div className={styles.tooltipItems}>
                <b>3신기</b>
                <ItemIconStrip items={championGuide.bestItems} />
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
