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
import type { GameGuideAiAskHandler } from '../utils/gameGuideAiRefs'

const AUGMENT_GUIDE_PAGE_SIZE = 6

interface AugmentGuideViewProps {
  fallbackData: GuideCatalog
  isGuideFallbackData: boolean
  isGuideFetching: boolean
  onGameGuideAiAsk: GameGuideAiAskHandler
  onGuideRetry: () => void
  patchVersion: string
  query: string
}

function AugmentGuideView({
  fallbackData,
  isGuideFallbackData,
  isGuideFetching,
  onGameGuideAiAsk,
  onGuideRetry,
  patchVersion,
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
      patchVersion,
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
        onGameGuideAiAsk={onGameGuideAiAsk}
      />
      <GuidePagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />
    </>
  )
}

export default AugmentGuideView
