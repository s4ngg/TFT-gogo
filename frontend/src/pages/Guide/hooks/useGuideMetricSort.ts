import { useState } from 'react'
import type { MetricSortKey, SortDir } from '../../../api/guide'

interface UseGuideMetricSortOptions {
  initialSortDir?: SortDir
  initialSortKey?: MetricSortKey
  onSortChange?: () => void
}

export function useGuideMetricSort({
  initialSortDir = 'desc',
  initialSortKey = 'winRate',
  onSortChange,
}: UseGuideMetricSortOptions = {}) {
  const [sortDir, setSortDir] = useState<SortDir>(initialSortDir)
  const [sortKey, setSortKey] = useState<MetricSortKey>(initialSortKey)

  function handleSort(nextSortKey: MetricSortKey) {
    if (sortKey === nextSortKey) {
      setSortDir((current) => (current === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortKey(nextSortKey)
      setSortDir(nextSortKey === 'avgPlace' ? 'asc' : 'desc')
    }

    onSortChange?.()
  }

  return {
    handleSort,
    sortDir,
    sortKey,
  }
}
