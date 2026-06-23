import { useState } from 'react'
import {
  type AugmentPlan,
  type AugmentPlanKey,
  type GuideCatalog,
} from '../../../api/guide'
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
import AugmentPlannerPanel from './AugmentPlannerPanel'

interface AugmentGuideViewProps {
  augmentPlans: AugmentPlan[]
  fallbackData: GuideCatalog
  query: string
}

const AUGMENT_PAGE_SIZE = 6

function AugmentGuideView({
  augmentPlans,
  fallbackData,
  query,
}: AugmentGuideViewProps) {
  const [planKey, setPlanKey] = useState<AugmentPlanKey>('fast8')
  const {
    currentPage,
    setCurrentPage,
  } = useGuideTabPagination({ resetKey: query })
  const augmentsQuery = useGuideTabItems({
    fallbackData,
    params: {
      page: currentPage,
      pageSize: AUGMENT_PAGE_SIZE,
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
        isFallbackData={augmentsQuery.data.source === 'fallback' && !augmentsQuery.isFetching}
        isFetching={augmentsQuery.isFetching}
        onRetry={() => {
          void augmentsQuery.refetch()
        }}
      />
      <AugmentGuideList
        augments={visibleAugments}
      />
      <GuidePagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />

      <AugmentPlannerPanel
        augmentPlans={augmentPlans}
        onPlanKeyChange={setPlanKey}
        planKey={planKey}
      />
    </>
  )
}

export default AugmentGuideView
