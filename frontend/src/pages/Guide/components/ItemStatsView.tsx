import type { GuideCatalog } from '../../../api/guide'
import { useGuideTabItems } from '../../../hooks/useGuide'
import {
  useGuidePageBounds,
  useGuideTabPagination,
} from '../hooks/useGuideTabPagination'
import {
  EmptyState,
  GuidePagination,
  GuideStatusBanner,
} from './GuideShared'
import styles from '../Guide.module.css'

interface ItemStatsViewProps {
  fallbackData: GuideCatalog
  isGuideFallbackData: boolean
  isGuideFetching: boolean
  onGuideRetry: () => void
  query: string
}

const ITEM_GUIDE_PAGE_SIZE = 6

function ItemStatsView({
  fallbackData,
  isGuideFallbackData,
  isGuideFetching,
  onGuideRetry,
  query,
}: ItemStatsViewProps) {
  const {
    currentPage,
    setCurrentPage,
  } = useGuideTabPagination({ resetKey: query })
  const itemsQuery = useGuideTabItems({
    fallbackData,
    params: {
      page: currentPage,
      pageSize: ITEM_GUIDE_PAGE_SIZE,
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
      <section className={styles.itemGuideList} aria-label="아이템 목록">
        {visibleItems.map((itemStat) => (
          <article className={styles.itemGuideCard} key={itemStat.name}>
            <div className={styles.itemGuideTop}>
              <div className={styles.itemGuideIdentity}>
                <img src={itemStat.imageUrl} alt={itemStat.name} />
                <div>
                  <h3>{itemStat.name}</h3>
                </div>
              </div>
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
                      <img src={championRef.imageUrl} alt="" />
                      {championRef.name}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </article>
        ))}
        {visibleItems.length === 0 && <EmptyState />}
      </section>
      <GuidePagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />
    </>
  )
}

export default ItemStatsView
