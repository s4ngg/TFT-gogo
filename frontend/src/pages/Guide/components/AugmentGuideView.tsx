import type { GuideCatalog } from '../../../api/guide'
import { useGuideTabItems } from '../../../hooks/useGuide'
import {
  useGuidePageBounds,
  useGuideTabPagination,
} from '../hooks/useGuideTabPagination'
import {
  GuidePagination,
  GuideStatusBanner,
} from './GuideShared'
import AugmentGuideList from './AugmentGuideList'

const AUGMENT_GUIDE_PAGE_SIZE = 6

interface AugmentGuideViewProps {
  fallbackData: GuideCatalog
  isGuideFallbackData: boolean
  isGuideFetching: boolean
  onGuideRetry: () => void
  query: string
}

function AugmentGuideView({
  fallbackData,
  isGuideFallbackData,
  isGuideFetching,
  onGuideRetry,
  query,
}: AugmentGuideViewProps) {
  const {
    currentPage,
    setCurrentPage,
  } = useGuideTabPagination({ resetKey: query })
  const augmentsQuery = useGuideTabItems({
    fallbackData,
    params: {
      page: currentPage,
      pageSize: AUGMENT_GUIDE_PAGE_SIZE,
      query,
      tab: 'augments',
    },
  })
  const pageData = augmentsQuery.data.data
  const safePage = useGuidePageBounds({
    currentPage,
    setCurrentPage,
    totalPages: pageData.totalPages,
  })
  const visibleAugments = pageData.items

  return (
    <>
      <GuideStatusBanner
        isFallbackData={isGuideFallbackData || (augmentsQuery.data.source === 'fallback' && !augmentsQuery.isFetching)}
        isFetching={isGuideFetching || augmentsQuery.isFetching}
        onRetry={() => {
          onGuideRetry()
          void augmentsQuery.refetch()
        }}
      />
      <AugmentGuideList
        augments={visibleAugments}
      />
      <GuidePagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />
    </>
  )
}

export default AugmentGuideView
