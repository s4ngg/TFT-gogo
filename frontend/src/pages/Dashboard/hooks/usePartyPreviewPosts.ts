import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'

import { getPartyPosts, type PartyPostsQueryParams } from '../../../api/partyApi'
import { communityPartyPostsQueryKey } from '../../../api/partyQueryKeys'
import type { PartyFilter } from '../../Party/partyFilters'
import { selectPartyPreviewPosts } from '../utils/partyPreview'

const PARTY_PREVIEW_STALE_TIME_MS = 1000 * 60

function toPartyPostsQueryParams(filter: PartyFilter): PartyPostsQueryParams {
  return filter === '전체' ? {} : { mode: filter }
}

export function usePartyPreviewPosts(selectedFilter: PartyFilter) {
  const partyQueryParams = useMemo(
    () => toPartyPostsQueryParams(selectedFilter),
    [selectedFilter],
  )
  const partyQuery = useQuery({
    queryKey: communityPartyPostsQueryKey(partyQueryParams),
    queryFn: () => getPartyPosts(partyQueryParams),
    staleTime: PARTY_PREVIEW_STALE_TIME_MS,
  })
  const posts = useMemo(
    () => selectPartyPreviewPosts(partyQuery.data?.data ?? [], selectedFilter),
    [partyQuery.data?.data, selectedFilter],
  )
  const statusMessage = partyQuery.isPending
    ? '파티 모집글을 불러오는 중입니다.'
    : partyQuery.data?.source === 'fallback'
      ? '파티 API 응답을 불러오지 못해 예시 모집글을 표시합니다.'
      : ''
  const isFallback = partyQuery.data?.source === 'fallback'
  const emptyMessage = selectedFilter === '전체'
    ? '등록된 모집글이 없습니다.'
    : '선택한 조건에 맞는 모집글이 없습니다.'

  return {
    emptyMessage,
    isFallback,
    isLoading: partyQuery.isPending,
    posts,
    statusMessage,
  }
}
