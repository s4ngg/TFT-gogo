import { useEffect } from 'react'
import type { AugmentGuide, GuideCatalog } from '../../../api/guide'
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
import type { HighlightedGuide } from '../utils/guideHighlight'

const AUGMENT_GUIDE_PAGE_SIZE = 6

interface AugmentGuideViewProps {
  fallbackData: GuideCatalog
  highlightedGuide: HighlightedGuide | null
  isGuideFallbackData: boolean
  isGuideFetching: boolean
  onGameGuideAiAsk: GameGuideAiAskHandler
  onGuideRetry: () => void
  onVisibleItemsChange: (items: AugmentGuide[]) => void
  patchVersion: string
  query: string
}

function AugmentGuideView({
  fallbackData,
  highlightedGuide,
  isGuideFallbackData,
  isGuideFetching,
  onGameGuideAiAsk,
  onGuideRetry,
  onVisibleItemsChange,
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
  const isUnavailableData = augmentsQuery.data.source === 'unavailable' && !augmentsQuery.isFetching

  useEffect(() => {
    onVisibleItemsChange(visibleAugments)
  }, [onVisibleItemsChange, visibleAugments])

  return (
    <>
      <GuideStatusBanner
        isFallbackData={!isUnavailableData && (isGuideFallbackData || (augmentsQuery.data.source === 'fallback' && !augmentsQuery.isFetching))}
        isFetching={isGuideFetching || augmentsQuery.isFetching}
        isUnavailableData={isUnavailableData}
        onRetry={() => {
          onGuideRetry()
          void augmentsQuery.refetch()
        }}
        patchVersion={augmentsQuery.data.patchVersion || patchVersion}
      />
      <AugmentGuideList
        augments={visibleAugments}
        highlightedGuide={highlightedGuide}
        onGameGuideAiAsk={onGameGuideAiAsk}
      />
      <GuidePagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />
    </>
  )
}

export default AugmentGuideView
