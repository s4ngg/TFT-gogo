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
      <div className={styles.tableWrap}>
        <table className={styles.itemTable}>
          <thead>
            <tr>
              <th className={styles.nameCol}>아이템</th>
              <th className={styles.descriptionCol}>설명</th>
            </tr>
          </thead>
          <tbody>
            {visibleItems.map((itemStat) => (
              <tr key={itemStat.name}>
                <td className={styles.itemNameCell}>
                  <img src={itemStat.imageUrl} alt={itemStat.name} />
                  <div>
                    <strong>{itemStat.name}</strong>
                  </div>
                </td>
                <td className={styles.itemDescriptionCell}>
                  {itemStat.description || '-'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {visibleItems.length === 0 && <EmptyState />}
      </div>
      <GuidePagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />
    </>
  )
}

export default ItemStatsView
