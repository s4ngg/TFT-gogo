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

export function usePatchNotes({ fallbackData }: UsePatchNotesOptions) {
  const [selectedPatchVersion, setSelectedPatchVersion] = useState(fallbackData[0]?.version ?? '')
  const hasUserSelectedPatchRef = useRef(false)

  const patchNotesQuery = useQuery<PatchNotesResult>({
    initialData: { data: fallbackData, source: 'fallback' },
    initialDataUpdatedAt: 0,
    queryFn: () => getPatchNotes(fallbackData),
    queryKey: ['patch-notes', 'list'],
    ...LIVE_CONTENT_QUERY_OPTIONS,
  })

  const patchNotes = patchNotesQuery.data.data
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
    const nextPatchVersion = resolvePatchSelection({
      hasUserSelectedPatch: hasUserSelectedPatchRef.current,
      isApiData: patchNotesQuery.data.source === 'api',
      patchVersions,
      selectedPatchVersion,
    })

    if (nextPatchVersion !== selectedPatchVersion) {
      setSelectedPatchVersion(nextPatchVersion)
    }
  }, [patchNotesQuery.data.source, patchVersions, selectedPatchVersion])

  return {
    isFallbackData: patchNotesQuery.data.source === 'fallback' && !patchNotesQuery.isFetching,
    isFetching: patchNotesQuery.isFetching,
    patchNotes,
    refetchPatchNotes: patchNotesQuery.refetch,
    selectedPatch,
    selectedPatchVersion,
    setSelectedPatchVersion: setSelectedPatchVersionByUser,
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
    ...LIVE_CONTENT_QUERY_OPTIONS,
  })
}
