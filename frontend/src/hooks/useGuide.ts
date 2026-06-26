import { keepPreviousData, useQuery } from '@tanstack/react-query'
import {
  DEFAULT_GUIDE_PAGE_SIZE,
  getGuideCatalog,
  getGuideTabItems,
  type GuideCatalog,
  type GuideCatalogResult,
  type GuideListQuery,
  type GuideTab,
  type GuideTabItems,
  type GuideTabPageResult,
} from '../api/guide'
import { LIVE_CONTENT_QUERY_OPTIONS } from './liveContentQueryOptions'

interface UseGuideCatalogOptions {
  fallbackData: GuideCatalog
}

interface UseGuideTabItemsOptions<T extends GuideTab> {
  fallbackData: GuideCatalog
  params: GuideListQuery & { tab: T }
}

function getPositiveInteger(value: number | undefined, fallback: number) {
  return Number.isFinite(value) && value !== undefined && value > 0
    ? Math.floor(value)
    : fallback
}

function createGuidePlaceholderPage<T extends GuideTab>(
  params: GuideListQuery & { tab: T },
): GuideTabPageResult<T> {
  const page = getPositiveInteger(params.page, 1)
  const pageSize = getPositiveInteger(params.pageSize, DEFAULT_GUIDE_PAGE_SIZE)

  return {
    data: {
      items: [] as GuideTabItems[T][number][],
      page,
      pageSize,
      totalItems: 0,
      totalPages: 1,
    },
    source: 'placeholder',
  }
}

export function useGuideCatalog({ fallbackData }: UseGuideCatalogOptions) {
  const placeholderData: GuideCatalogResult = { data: fallbackData, source: 'placeholder' }
  const guideQuery = useQuery<GuideCatalogResult>({
    placeholderData,
    queryFn: () => getGuideCatalog(fallbackData),
    queryKey: ['guide', 'catalog'],
    ...LIVE_CONTENT_QUERY_OPTIONS,
  })
  const guideResult = guideQuery.data ?? placeholderData

  return {
    guideData: guideResult.data,
    isFallbackData: guideResult.source === 'fallback' && !guideQuery.isFetching,
    isFetching: guideQuery.isFetching,
    refetchGuideData: guideQuery.refetch,
  }
}

export function useGuideTabItems<T extends GuideTab>({
  fallbackData,
  params,
}: UseGuideTabItemsOptions<T>) {
  const placeholderData = createGuidePlaceholderPage(params)
  const resolvedParams = {
    ...params,
    patchVersion: params.patchVersion ?? fallbackData.patchVersion,
  }
  const guideQuery = useQuery<GuideTabPageResult<T>>({
    placeholderData: keepPreviousData,
    queryFn: () => getGuideTabItems(resolvedParams, fallbackData),
    queryKey: [
      'guide',
      'tab-items',
      resolvedParams.tab,
      resolvedParams.patchVersion,
      resolvedParams.page ?? 1,
      resolvedParams.pageSize ?? DEFAULT_GUIDE_PAGE_SIZE,
      resolvedParams.query ?? '',
      resolvedParams.cost ?? 'all',
    ],
    ...LIVE_CONTENT_QUERY_OPTIONS,
  })

  return {
    ...guideQuery,
    data: guideQuery.data ?? placeholderData,
  }
}