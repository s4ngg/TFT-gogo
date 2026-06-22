import {
  DEFAULT_GUIDE_PAGE_SIZE,
  type GuideCatalog,
} from '../../../api/guide'
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
  query: string
}

function isHiddenRecipeNote(note: string) {
  const trimmedNote = note.trim()
  return trimmedNote === '' || trimmedNote.includes('CDragon') || trimmedNote === '재료 2개 조합'
}

function ItemStatsView({
  fallbackData,
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
      pageSize: DEFAULT_GUIDE_PAGE_SIZE,
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
        isFallbackData={itemsQuery.data.source === 'fallback' && !itemsQuery.isFetching}
        isFetching={itemsQuery.isFetching}
        onRetry={() => {
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
                  <span>{itemStat.category}</span>
                </div>
              </div>
            </div>

            <p className={styles.itemGuideDescription}>
              {itemStat.description || '아이템 효과 정보가 아직 준비되지 않았습니다.'}
            </p>

            <div className={styles.itemRecipePanel}>
              <strong>조합식</strong>
              {itemStat.combinations.length > 0 ? (
                itemStat.combinations.map((combination) => (
                  <div className={styles.itemRecipeRow} key={combination.label}>
                    <div className={styles.itemRecipeIcons}>
                      {combination.items.map((recipeItem, index) => (
                        <span className={styles.itemRecipePart} key={`${recipeItem.name}-${index}`}>
                          {index > 0 && <b aria-hidden="true">+</b>}
                          <img src={recipeItem.imageUrl} alt={recipeItem.name} title={recipeItem.name} />
                        </span>
                      ))}
                    </div>
                    <div className={styles.itemRecipeText}>
                      <span>{combination.items.map((recipeItem) => recipeItem.name).join(' + ')}</span>
                      {!isHiddenRecipeNote(combination.note) && <small>{combination.note}</small>}
                    </div>
                  </div>
                ))
              ) : (
                <span className={styles.itemRecipeEmpty}>조합 정보 없음</span>
              )}
            </div>

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
