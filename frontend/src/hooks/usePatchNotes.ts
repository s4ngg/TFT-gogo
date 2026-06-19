import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
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
import { LIVE_CONTENT_QUERY_OPTIONS } from './liveContentQueryOptions'
import { resolvePatchSelection } from './patchNoteSelection'

interface UsePatchNotesOptions {
  fallbackData: PatchNoteDetail[]
}

interface UsePatchChangesOptions {
  fallbackData: PatchNoteDetail[]
  params: PatchChangesQuery
}

const EMPTY_PATCH_NOTES: PatchNoteDetail[] = []

export function usePatchNotes({ fallbackData }: UsePatchNotesOptions) {
  const [selectedPatchVersion, setSelectedPatchVersion] = useState('')
  const hasUserSelectedPatchRef = useRef(false)

  const patchNotesQuery = useQuery<PatchNotesResult>({
    queryFn: () => getPatchNotes(fallbackData),
    queryKey: ['patch-notes', 'list'],
    ...LIVE_CONTENT_QUERY_OPTIONS,
  })

  const patchNotes = patchNotesQuery.data?.data ?? EMPTY_PATCH_NOTES
  const patchVersions = useMemo(() => patchNotes.map((patch) => patch.version), [patchNotes])

  const selectedPatch = useMemo(
    () => patchNotes.find((patch) => patch.version === selectedPatchVersion) ?? patchNotes[0],
    [patchNotes, selectedPatchVersion],
  )

  const setSelectedPatchVersionByUser = useCallback((version: string) => {
    hasUserSelectedPatchRef.current = true
    setSelectedPatchVersion(version)
  }, [])

  useEffect(() => {
    if (!patchNotesQuery.data) return

    const nextPatchVersion = resolvePatchSelection({
      hasUserSelectedPatch: hasUserSelectedPatchRef.current,
      isApiData: patchNotesQuery.data.source === 'api',
      patchVersions,
      selectedPatchVersion,
    })

    if (nextPatchVersion !== selectedPatchVersion) {
      setSelectedPatchVersion(nextPatchVersion)
    }
  }, [patchNotesQuery.data, patchVersions, selectedPatchVersion])

  return {
    isFallbackData: patchNotesQuery.data?.source === 'fallback' && !patchNotesQuery.isFetching,
    isFetching: patchNotesQuery.isFetching,
    patchNotes,
    refetchPatchNotes: patchNotesQuery.refetch,
    selectedPatch,
    selectedPatchVersion,
    setSelectedPatchVersion: setSelectedPatchVersionByUser,
  }
}

export function usePatchChanges({ fallbackData, params }: UsePatchChangesOptions) {
  const fallbackResult = useMemo<PatchChangesResult>(
    () => ({
      data: getFallbackPatchChangePage(params, fallbackData),
      source: 'fallback',
    }),
    [fallbackData, params],
  )

  return useQuery<PatchChangesResult>({
    enabled: params.version.length > 0,
    placeholderData: (previousData) => (params.version ? previousData ?? fallbackResult : undefined),
    queryFn: () => getPatchChanges(params, fallbackData),
    queryKey: ['patch-notes', params.version, 'changes', params],
    ...LIVE_CONTENT_QUERY_OPTIONS,
  })
}
