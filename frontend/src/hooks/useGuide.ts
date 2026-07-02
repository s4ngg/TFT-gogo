import { keepPreviousData, useQuery } from '@tanstack/react-query'
import {
  DEFAULT_GUIDE_PAGE_SIZE,
  getGuidePatchVersion,
  getGuideTabItems,
  type GuideCatalog,
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
  const placeholderData = { patchVersion: fallbackData.patchVersion, source: 'placeholder' as const }
  const patchVersionQuery = useQuery({
    placeholderData,
    queryFn: () => getGuidePatchVersion(fallbackData.patchVersion),
    queryKey: ['guide', 'patch-version'],
    ...LIVE_CONTENT_QUERY_OPTIONS,
  })
  const patchVersionResult = patchVersionQuery.data ?? placeholderData
  const guideData: GuideCatalog = { ...fallbackData, patchVersion: patchVersionResult.patchVersion }

  return {
    guideData,
    isFallbackData: patchVersionResult.source === 'fallback' && !patchVersionQuery.isFetching,
    isFetching: patchVersionQuery.isFetching,
    refetchGuideData: patchVersionQuery.refetch,
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