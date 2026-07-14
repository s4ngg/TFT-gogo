import axiosInstance from './axiosInstance'
import { isRecord, unwrapApiResponse, type ApiResponse } from './apiResponse'
import { getFallbackGuideTabPage } from './guideFallback'
import { hasGuidePayloadData, normalizeGuideCatalog, normalizeGuideTabPage } from './guideNormalizers'
import type {
  GuideCatalog,
  GuideCatalogResult,
  GuideDataSource,
  GuideEntryResponse,
  GuideListQuery,
  GuideTab,
  GuideTabPageResult,
} from './guideTypes'

function getGuideTabFallbackResult<T extends GuideTab>(
  params: GuideListQuery & { tab: T },
  fallbackData: GuideCatalog,
): GuideTabPageResult<T> {
  const patchVersion = params.patchVersion?.trim() ?? ''
  const hasMatchingFallback = patchVersion.length > 0 && patchVersion === fallbackData.patchVersion

  return {
    data: getFallbackGuideTabPage(params, fallbackData),
    patchVersion,
    source: hasMatchingFallback ? 'fallback' : 'unavailable',
  }
}

export async function getGuideCatalog(fallbackData: GuideCatalog): Promise<GuideCatalogResult> {
  try {
    const { data } = await axiosInstance.get<
      ApiResponse<GuideCatalog | GuideEntryResponse[]> | GuideCatalog | GuideEntryResponse[]
    >('/guide')
    const payload = unwrapApiResponse(data)

    if (!hasGuidePayloadData(payload)) {
      return { data: fallbackData, source: 'fallback' }
    }

    return { data: normalizeGuideCatalog(payload, fallbackData), source: 'api' }
  } catch {
    return { data: fallbackData, source: 'fallback' }
  }
}

export async function getGuidePatchVersion(
  fallbackPatchVersion: string,
): Promise<{ patchVersion: string; source: GuideDataSource }> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<{ patchVersion: string }> | { patchVersion: string }>(
      '/guide/patch-version',
    )
    const payload = unwrapApiResponse(data)
    const patchVersion = isRecord(payload) && typeof payload.patchVersion === 'string'
      ? payload.patchVersion.trim()
      : ''

    if (!patchVersion) {
      return { patchVersion: fallbackPatchVersion, source: 'fallback' }
    }

    return { patchVersion, source: 'api' }
  } catch {
    return { patchVersion: fallbackPatchVersion, source: 'fallback' }
  }
}

export async function getGuideTabItems<T extends GuideTab>(
  params: GuideListQuery & { tab: T },
  fallbackData: GuideCatalog,
): Promise<GuideTabPageResult<T>> {
  try {
    const { tab, ...queryParams } = params
    const { data } = await axiosInstance.get<ApiResponse<unknown> | unknown>(`/guide/${tab}`, {
      params: {
        ...queryParams,
        cost: queryParams.cost === 'all' ? undefined : queryParams.cost,
        patchVersion: queryParams.patchVersion || undefined,
      },
    })
    const payload = unwrapApiResponse(data)
    const page = normalizeGuideTabPage(payload, params, fallbackData)

    if (!page) {
      return getGuideTabFallbackResult(params, fallbackData)
    }

    return { data: page, patchVersion: params.patchVersion?.trim() ?? '', source: 'api' }
  } catch {
    return getGuideTabFallbackResult(params, fallbackData)
  }
}
