import { useState } from 'react'
import {
  DEFAULT_GUIDE_PAGE_SIZE,
  type AugmentPlan,
  type AugmentPlanKey,
  type GuideCatalog,
  type RewardRow,
} from '../../../api/guide'
import { useGuideTabItems } from '../../../hooks/useGuide'
import { useGuideMetricSort } from '../hooks/useGuideMetricSort'
import {
  useGuidePageBounds,
  useGuideTabPagination,
} from '../hooks/useGuideTabPagination'
import {
  GuidePagination,
  GuideStatusBanner,
} from './GuideShared'
import AugmentPlannerPanel from './AugmentPlannerPanel'
import AugmentRewardPanel from './AugmentRewardPanel'
import AugmentStatsTable from './AugmentStatsTable'
import styles from '../Guide.module.css'

interface AugmentGuideViewProps {
  augmentPlans: AugmentPlan[]
  fallbackData: GuideCatalog
  query: string
  rewardRows: RewardRow[]
}

function AugmentGuideView({
  augmentPlans,
  fallbackData,
  query,
  rewardRows,
}: AugmentGuideViewProps) {
  const [planKey, setPlanKey] = useState<AugmentPlanKey>('fast8')
  const {
    currentPage,
    setCurrentPage,
  } = useGuideTabPagination({ resetKey: query })
  const {
    handleSort,
    sortDir,
    sortKey,
  } = useGuideMetricSort({ onSortChange: () => setCurrentPage(1) })
  const augmentsQuery = useGuideTabItems({
    fallbackData,
    params: {
      page: currentPage,
      pageSize: DEFAULT_GUIDE_PAGE_SIZE,
      query,
      sortDir,
      sortKey,
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
      <div className={styles.augmentLayout}>
        <AugmentStatsTable
          augments={visibleAugments}
          onSort={handleSort}
          sortDir={sortDir}
          sortKey={sortKey}
        />
        <AugmentRewardPanel rewardRows={rewardRows} />
      </div>
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
