import { useEffect } from 'react'
import type { PatchChangesQuery, PatchNoteDetail } from '../../../api/patchNotes'
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
  const patchChangesQuery = usePatchChanges({
    fallbackData: patchHistory.length > 0 ? patchHistory : fallbackData,
    params,
  })
  const changesPage = patchChangesQuery.data.data
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
