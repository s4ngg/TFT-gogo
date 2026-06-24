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

export function usePatchChangesPage({
  fallbackData,
  onPageOutOfRange,
  params,
  patchHistory,
}: UsePatchChangesPageOptions) {
  const patchChangesFallbackData = patchHistory.length > 0 ? patchHistory : fallbackData
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
