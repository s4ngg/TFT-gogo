import { useEffect, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  CHANGE_TYPE_FILTERS,
  PATCH_CATEGORIES,
  PATCH_PAGE_SIZE,
  buildPatchChangesResponse,
  getPatchChanges,
  getPatchHistory,
  getPatchSummary,
  type ChangeTypeFilter,
  type PatchCategory,
  type PatchChange,
  type PatchChangesQuery,
  type PatchHistoryItem,
  type PatchNotesFallbackData,
} from '../api/patchNotes'

interface UsePatchNotesOptions {
  historyFallback: PatchHistoryItem[]
  changesFallback: PatchChange[]
  pageSize?: number
}

const EMPTY_PATCH_HISTORY_ITEM: PatchHistoryItem = {
  version: '0.0',
  date: '',
  title: '패치 노트 준비 중',
  status: '현재',
  focus: '패치 정보 준비 중',
  description: '패치 정보를 준비하고 있습니다.',
  highlights: [],
  imageUrl: '',
}

export function usePatchNotes({
  historyFallback,
  changesFallback,
  pageSize = PATCH_PAGE_SIZE,
}: UsePatchNotesOptions) {
  const fallbackData = useMemo<PatchNotesFallbackData>(() => ({
    baseChanges: changesFallback,
    history: historyFallback,
  }), [changesFallback, historyFallback])
  const defaultPatch = historyFallback[0] ?? EMPTY_PATCH_HISTORY_ITEM
  const [selectedPatchVersion, setSelectedPatchVersion] = useState(defaultPatch.version)
  const [activeCategory, setActiveCategory] = useState<PatchCategory>(PATCH_CATEGORIES[0])
  const [activeChangeType, setActiveChangeType] = useState<ChangeTypeFilter>(CHANGE_TYPE_FILTERS[0])
  const [highImpactOnly, setHighImpactOnly] = useState(false)
  const [query, setQuery] = useState('')
  const [currentPage, setCurrentPage] = useState(1)

  const historyQuery = useQuery({
    queryKey: ['patchNotes', 'history'],
    queryFn: () => getPatchHistory(historyFallback),
    initialData: historyFallback,
    staleTime: 1000 * 60 * 5,
  })
  const patchHistory = historyQuery.data.length > 0 ? historyQuery.data : [defaultPatch]
  const selectedPatchFallback = patchHistory.find((patch) => patch.version === selectedPatchVersion) ?? patchHistory[0]

  const changesParams = useMemo<PatchChangesQuery>(() => ({
    patchVersion: selectedPatchVersion,
    category: activeCategory,
    changeType: activeChangeType,
    highImpactOnly,
    query,
    page: currentPage,
    pageSize,
  }), [activeCategory, activeChangeType, currentPage, highImpactOnly, pageSize, query, selectedPatchVersion])

  const initialChanges = useMemo(
    () => buildPatchChangesResponse(changesParams, fallbackData),
    [changesParams, fallbackData],
  )

  const summaryQuery = useQuery({
    queryKey: ['patchNotes', 'summary', selectedPatchVersion],
    queryFn: () => getPatchSummary(selectedPatchVersion, historyFallback),
    enabled: selectedPatchVersion.length > 0,
    initialData: selectedPatchFallback,
    staleTime: 1000 * 60 * 5,
  })

  const changesQuery = useQuery({
    queryKey: ['patchNotes', 'changes', changesParams],
    queryFn: () => getPatchChanges(changesParams, fallbackData),
    enabled: selectedPatchVersion.length > 0,
    initialData: initialChanges,
    staleTime: 1000 * 60 * 5,
  })

  useEffect(() => {
    if (!selectedPatchVersion && defaultPatch.version) {
      setSelectedPatchVersion(defaultPatch.version)
    }
  }, [defaultPatch.version, selectedPatchVersion])

  useEffect(() => {
    setCurrentPage(1)
  }, [activeCategory, activeChangeType, highImpactOnly, query, selectedPatchVersion])

  const refetch = () => {
    void historyQuery.refetch()
    void summaryQuery.refetch()
    void changesQuery.refetch()
  }

  return {
    selectedPatchVersion,
    setSelectedPatchVersion,
    activeCategory,
    setActiveCategory,
    activeChangeType,
    setActiveChangeType,
    highImpactOnly,
    setHighImpactOnly,
    query,
    setQuery,
    currentPage,
    setCurrentPage,
    patchHistory,
    selectedPatch: summaryQuery.data ?? selectedPatchFallback,
    patchChanges: changesQuery.data.allChanges,
    pagedChanges: changesQuery.data.items,
    totalItems: changesQuery.data.totalItems,
    totalPages: changesQuery.data.totalPages,
    safePage: Math.min(currentPage, changesQuery.data.totalPages),
    categoryCounts: changesQuery.data.categoryCounts,
    highImpactCount: changesQuery.data.highImpactCount,
    buffCount: changesQuery.data.buffCount,
    nerfCount: changesQuery.data.nerfCount,
    isLoading: historyQuery.isLoading || summaryQuery.isLoading || changesQuery.isLoading,
    isFetching: historyQuery.isFetching || summaryQuery.isFetching || changesQuery.isFetching,
    isError: historyQuery.isError || summaryQuery.isError || changesQuery.isError,
    refetch,
  }
}
