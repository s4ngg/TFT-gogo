import { useEffect, useMemo } from 'react'
import {
  getFallbackPatchChangePage,
  type PatchChangesQuery,
  type PatchNoteDetail,
} from '../../../api/patchNotes'
import { usePatchChanges } from '../../../hooks/usePatchNotes'

interface UsePatchChangesPageOptions {
  fallbackData: PatchNoteDetail[]
  patchHistory: PatchNoteDetail[]
  params: PatchChangesQuery
  onPageOutOfRange: (page: number) => void
}

/**
 * Uses API history only when the requested patch version has change rows.
 * Otherwise keeps the static fallback data so the UI does not show an empty list
 * for a version that is missing from API data.
 */
export function resolvePatchChangesFallbackData(
  version: string,
  fallbackData: PatchNoteDetail[],
  patchHistory: PatchNoteDetail[],
) {
  if (patchHistory.length === 0) return fallbackData

  const selectedHistoryPatch = patchHistory.find((patch) => patch.version === version)
  if (selectedHistoryPatch && selectedHistoryPatch.changes.length > 0) return patchHistory

  const selectedFallbackPatch = fallbackData.find((patch) => patch.version === version)
  if (selectedFallbackPatch && selectedFallbackPatch.changes.length > 0) return fallbackData

  return fallbackData
}

export function usePatchChangesPage({
  fallbackData,
  onPageOutOfRange,
  params,
  patchHistory,
}: UsePatchChangesPageOptions) {
  const patchChangesFallbackData = useMemo(
    () => resolvePatchChangesFallbackData(params.version, fallbackData, patchHistory),
    [fallbackData, params.version, patchHistory],
  )
  const fallbackChangesPage = useMemo(
    () => getFallbackPatchChangePage(params, patchChangesFallbackData),
    [params, patchChangesFallbackData],
  )
  const patchChangesQuery = usePatchChanges({
    fallbackData: patchChangesFallbackData,
    params,
  })
  const changesPage = patchChangesQuery.data?.data ?? fallbackChangesPage
  const patchChanges = changesPage.items
  const changeStats = changesPage.stats
  const safePage = Math.max(1, Math.min(params.page, changesPage.totalPages))

  useEffect(() => {
    if (changesPage.totalPages < 1) {
      if (params.page !== 1) onPageOutOfRange(1)
      return
    }

    if (params.page > changesPage.totalPages) onPageOutOfRange(changesPage.totalPages)
  }, [changesPage.totalPages, onPageOutOfRange, params.page])

  return {
    changeStats,
    changesPage,
    patchChanges,
    patchChangesQuery,
    safePage,
  }
}
