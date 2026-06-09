import { useState } from 'react'
import type { MetricSortKey, SortDir } from '../../../api/guide'

interface UseGuideMetricSortOptions {
  initialSortDir?: SortDir
  initialSortKey?: MetricSortKey
  onSortChange?: (sortKey: MetricSortKey, sortDir: SortDir) => void
}

export function useGuideMetricSort({
  initialSortDir = 'desc',
  initialSortKey = 'winRate',
  onSortChange,
}: UseGuideMetricSortOptions = {}) {
  const [sortDir, setSortDir] = useState<SortDir>(initialSortDir)
  const [sortKey, setSortKey] = useState<MetricSortKey>(initialSortKey)

  function handleSort(nextSortKey: MetricSortKey) {
    let newSortKey = sortKey
    let newSortDir = sortDir

    if (sortKey === nextSortKey) {
      newSortDir = sortDir === 'asc' ? 'desc' : 'asc'
      setSortDir(newSortDir)
    } else {
      newSortKey = nextSortKey
      newSortDir = nextSortKey === 'avgPlace' ? 'asc' : 'desc'
      setSortKey(newSortKey)
      setSortDir(newSortDir)
    }

    onSortChange?.(newSortKey, newSortDir)
  }

  return {
    handleSort,
    sortDir,
    sortKey,
  }
}
