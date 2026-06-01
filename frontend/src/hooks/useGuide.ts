import { useQuery } from '@tanstack/react-query'
import {
  getFallbackGuideTabPage,
  getGuideCatalog,
  getGuideTabItems,
  type GuideCatalog,
  type GuideCatalogResult,
  type GuideListQuery,
  type GuideTab,
  type GuideTabPageResult,
} from '../api/guide'

interface UseGuideCatalogOptions {
  fallbackData: GuideCatalog
}

interface UseGuideTabItemsOptions<T extends GuideTab> {
  fallbackData: GuideCatalog
  params: GuideListQuery & { tab: T }
}

export function useGuideCatalog({ fallbackData }: UseGuideCatalogOptions) {
  const guideQuery = useQuery<GuideCatalogResult>({
    initialData: { data: fallbackData, source: 'fallback' },
    queryFn: () => getGuideCatalog(fallbackData),
    queryKey: ['guide', 'catalog'],
    staleTime: 1000 * 60 * 5,
  })

  return {
    guideData: guideQuery.data.data,
    isFallbackData: guideQuery.data.source === 'fallback' && !guideQuery.isFetching,
    isFetching: guideQuery.isFetching,
    refetchGuideData: guideQuery.refetch,
  }
}

export function useGuideTabItems<T extends GuideTab>({
  fallbackData,
  params,
}: UseGuideTabItemsOptions<T>) {
  return useQuery<GuideTabPageResult<T>>({
    initialData: () => ({
      data: getFallbackGuideTabPage(params, fallbackData),
      source: 'fallback' as const,
    }),
    queryFn: () => getGuideTabItems(params, fallbackData),
    queryKey: ['guide', params.tab, params],
    staleTime: 1000 * 60 * 5,
  })
}
