import { useEffect, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  getFallbackPatchChangePage,
  getPatchChanges,
  getPatchNotes,
  type PatchChangesQuery,
  type PatchChangesResult,
  type PatchNoteDetail,
  type PatchNotesResult,
} from '../api/patchNotes'

interface UsePatchNotesOptions {
  fallbackData: PatchNoteDetail[]
}

interface UsePatchChangesOptions {
  fallbackData: PatchNoteDetail[]
  params: PatchChangesQuery
}

export function usePatchNotes({ fallbackData }: UsePatchNotesOptions) {
  const [selectedPatchVersion, setSelectedPatchVersion] = useState(fallbackData[0]?.version ?? '')

  const patchNotesQuery = useQuery<PatchNotesResult>({
    initialData: { data: fallbackData, source: 'fallback' },
    queryFn: () => getPatchNotes(fallbackData),
    queryKey: ['patch-notes', 'list'],
    staleTime: 1000 * 60 * 5,
  })

  const patchNotes = patchNotesQuery.data.data
  const hasSelectedPatch = patchNotes.some((patch) => patch.version === selectedPatchVersion)

  const selectedPatch = useMemo(
    () => patchNotes.find((patch) => patch.version === selectedPatchVersion) ?? patchNotes[0],
    [patchNotes, selectedPatchVersion],
  )

  useEffect(() => {
    if (hasSelectedPatch || patchNotes.length === 0) return
    setSelectedPatchVersion(patchNotes[0].version)
  }, [hasSelectedPatch, patchNotes])

  return {
    isFallbackData: patchNotesQuery.data.source === 'fallback' && !patchNotesQuery.isFetching,
    isFetching: patchNotesQuery.isFetching,
    patchNotes,
    refetchPatchNotes: patchNotesQuery.refetch,
    selectedPatch,
    selectedPatchVersion,
    setSelectedPatchVersion,
  }
}

export function usePatchChanges({ fallbackData, params }: UsePatchChangesOptions) {
  return useQuery<PatchChangesResult>({
    initialData: {
      data: getFallbackPatchChangePage(params, fallbackData),
      source: 'fallback',
    },
    initialDataUpdatedAt: 0,
    queryFn: () => getPatchChanges(params, fallbackData),
    queryKey: ['patch-notes', params.version, 'changes', params],
    staleTime: 1000 * 60 * 5,
  })
}
