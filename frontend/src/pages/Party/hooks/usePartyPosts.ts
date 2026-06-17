import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useRef, useState } from 'react'
import {
  cancelPartyJoin,
  createPartyPost,
  getPartyPosts,
  joinPartyPost,
  type CreatePartyPostRequest,
  type PartyPostsQueryParams,
  type PartyPostsResult,
} from '../../../api/partyApi'
import {
  COMMUNITY_PARTY_POSTS_QUERY_KEY,
  communityPartyPostsQueryKey,
} from '../../../api/partyQueryKeys'
import type { PartyFilter } from '../partyFilters'
import type { PartyMode, PartyPost } from '../types'
import {
  filterPartyPosts,
  formatDeadlineForRequest,
  getDefaultDeadlineInput,
  getPartyActionNotice,
  getPartyListEmptyMessage,
  mergePartyPostSources,
  normalizeCapacity,
  paginatePartyPosts,
  parseCapacity,
  removePostOverride,
  replacePost,
  restorePostOverride,
  updatePostJoinState,
} from '../utils/partyUtils'
import { usePartyAuth } from './usePartyAuth'

const PARTY_PAGE_SIZE = 3

interface UsePartyPostsOptions {
  onPartyMessage: (post: PartyPost, message: string) => void
  onPartyPostCreated: (post: PartyPost, message?: string) => void
}

interface JoinMutationVariables {
  isJoining: boolean
  postId: string
}

function withAuthDisplayState(
  post: PartyPost,
  authUserId: string | null,
  isAuthenticated: boolean,
): PartyPost {
  const isOwner = isAuthenticated && authUserId !== null && post.userId === authUserId

  return {
    ...post,
    isJoined: isAuthenticated ? post.isJoined : false,
    isOwner,
  }
}

function readMutationErrorMessage(error: unknown, fallbackMessage: string) {
  return error instanceof Error && error.message.trim().length > 0
    ? error.message
    : fallbackMessage
}

function toPartyPostsQueryParams(filter: PartyFilter): PartyPostsQueryParams {
  return filter === '전체' ? {} : { mode: filter }
}

export function usePartyPosts({ onPartyMessage, onPartyPostCreated }: UsePartyPostsOptions) {
  const queryClient = useQueryClient()
  const { token: authToken, userId: authUserId } = usePartyAuth()
  const isAuthenticated = (authToken?.trim().length ?? 0) > 0
  const [localPosts, setLocalPosts] = useState<PartyPost[]>([])
  const [postOverrides, setPostOverrides] = useState<Record<string, PartyPost>>({})
  const [selectedFilter, setSelectedFilter] = useState<PartyFilter>('전체')
  const [searchDraft, setSearchDraft] = useState('')
  const [query, setQuery] = useState('')
  const [joinedPostId, setJoinedPostId] = useState<string | null | undefined>(undefined)
  const [titleDraft, setTitleDraft] = useState('')
  const [modeDraft, setModeDraft] = useState<PartyMode>('랭크')
  const [tierDraft, setTierDraft] = useState('마스터+')
  const [capacityDraft, setCapacityDraft] = useState('2')
  const [deadlineDraft, setDeadlineDraft] = useState(getDefaultDeadlineInput)
  const [tagsDraft, setTagsDraft] = useState('')
  const [descriptionDraft, setDescriptionDraft] = useState('')
  const [composeError, setComposeError] = useState('')
  const [partyStatusMessage, setPartyStatusMessage] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const minDeadline = useMemo(getDefaultDeadlineInput, [])
  const titleInputRef = useRef<HTMLInputElement>(null)
  const joinRequestPostIdRef = useRef<string | null>(null)
  const partyQueryParams = useMemo(
    () => toPartyPostsQueryParams(selectedFilter),
    [selectedFilter],
  )
  const partyQueryKey = useMemo(
    () => communityPartyPostsQueryKey(partyQueryParams),
    [partyQueryParams],
  )

  const partyQuery = useQuery({
    queryKey: partyQueryKey,
    queryFn: () => getPartyPosts(partyQueryParams),
    staleTime: 1000 * 60,
  })
  const createMutation = useMutation({
    mutationFn: createPartyPost,
  })
  const joinMutation = useMutation({
    mutationFn: ({ postId, isJoining }: JoinMutationVariables) =>
      isJoining ? joinPartyPost(postId) : cancelPartyJoin(postId),
  })
  const isPartyListUnavailable = partyQuery.isError || partyQuery.data?.source === 'unavailable'

  useEffect(() => {
    if (isAuthenticated) {
      return
    }

    setLocalPosts([])
    setPostOverrides({})
    setJoinedPostId(undefined)
    setPartyStatusMessage('')
  }, [isAuthenticated])

  const posts = useMemo(() => {
    const shouldIgnoreLocalState = isPartyListUnavailable || !isAuthenticated
    const mergedPosts = mergePartyPostSources({
      localPosts: shouldIgnoreLocalState ? [] : localPosts,
      postOverrides: shouldIgnoreLocalState ? {} : postOverrides,
      serverPosts: partyQuery.data?.data,
    })

    return mergedPosts.map((post) => withAuthDisplayState(post, authUserId, isAuthenticated))
  }, [authUserId, isAuthenticated, isPartyListUnavailable, localPosts, partyQuery.data?.data, postOverrides])
  const activeJoinedPostId = useMemo(() => {
    if (!isAuthenticated) {
      return null
    }

    if (joinedPostId !== undefined) {
      return joinedPostId
    }

    return posts.find((post) => post.isJoined === true)?.id ?? null
  }, [isAuthenticated, joinedPostId, posts])
  const filteredPartyPosts = useMemo(
    () => filterPartyPosts(posts, selectedFilter, query),
    [posts, query, selectedFilter],
  )
  const { pageItems, safePage, totalPages } = useMemo(
    () => paginatePartyPosts(filteredPartyPosts, currentPage, PARTY_PAGE_SIZE),
    [currentPage, filteredPartyPosts],
  )
  const emptyMessage = getPartyListEmptyMessage({
    isAuthenticated,
    isLoading: partyQuery.isPending,
    isUnavailable: isPartyListUnavailable,
    selectedFilter,
  })
  const statusMessage = partyStatusMessage || getPartyActionNotice(isAuthenticated)

  useEffect(() => {
    if (currentPage !== safePage) {
      setCurrentPage(safePage)
    }
  }, [currentPage, safePage])

  const updateSearchDraft = (value: string) => {
    setSearchDraft(value)
    setQuery(value)
    setCurrentPage(1)
  }

  const changeFilter = (filter: PartyFilter) => {
    setSelectedFilter(filter)
    setCurrentPage(1)
  }

  const submitSearch = () => {
    setQuery(searchDraft)
    setCurrentPage(1)
  }

  const focusCompose = () => {
    titleInputRef.current?.focus()
    setComposeError('')
    setPartyStatusMessage('')
  }

  const submitPartyPost = () => {
    createMutation.reset()

    if (!isAuthenticated) {
      setComposeError('로그인 후 파티 모집글을 등록할 수 있습니다.')
      setPartyStatusMessage('')
      return
    }

    const title = titleDraft.trim()
    const description = descriptionDraft.trim()
    const deadline = formatDeadlineForRequest(deadlineDraft)

    if (!title || !description) {
      setComposeError('모집글 제목과 플레이 스타일을 입력해주세요.')
      return
    }

    if (deadline === null) {
      setComposeError('마감 시간을 선택해주세요.')
      return
    }

    const parsedTags = tagsDraft
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean)
      .slice(0, 4)
    const request: CreatePartyPostRequest = {
      title,
      mode: modeDraft,
      tier: tierDraft,
      capacity: normalizeCapacity(capacityDraft),
      deadline,
      description,
      tags: parsedTags,
    }

    createMutation.mutate(request, {
      onSuccess: (createdPost) => {
        const serverPost = withAuthDisplayState(createdPost, authUserId, isAuthenticated)

        setLocalPosts((currentPosts) => [
          serverPost,
          ...currentPosts.filter((post) => post.id !== serverPost.id),
        ])
        setSelectedFilter('전체')
        setSearchDraft('')
        setQuery('')
        setTitleDraft('')
        setModeDraft('랭크')
        setTierDraft('마스터+')
        setCapacityDraft('2')
        setDeadlineDraft(getDefaultDeadlineInput())
        setTagsDraft('')
        setDescriptionDraft('')
        setComposeError('')
        setPartyStatusMessage('모집글을 등록했습니다.')
        setCurrentPage(1)
        onPartyPostCreated(serverPost)

        void queryClient.invalidateQueries({ queryKey: COMMUNITY_PARTY_POSTS_QUERY_KEY })
      },
      onError: (error) => {
        setPartyStatusMessage(readMutationErrorMessage(error, '파티 모집글 등록에 실패했습니다.'))
      },
    })
  }

  const toggleJoin = (postId: string) => {
    if (joinRequestPostIdRef.current !== null || joinMutation.isPending) {
      return
    }

    if (!isAuthenticated) {
      setPartyStatusMessage('로그인 후 파티에 참여할 수 있습니다.')
      return
    }

    const targetPost = posts.find((post) => post.id === postId)

    if (!targetPost) {
      return
    }

    if (targetPost.isOwner) {
      setPartyStatusMessage('작성자는 자신의 모집글에서 나갈 수 없습니다.')
      return
    }

    joinMutation.reset()

    const serverJoinedPostId = posts.find((post) => post.isJoined === true)?.id ?? null
    const joinedLockPostId = joinedPostId !== undefined ? joinedPostId : serverJoinedPostId
    const alreadyJoined = targetPost.isJoined === true || joinedLockPostId === postId
    const previousJoinedPostId = joinedPostId
    const previousOverride = postOverrides[postId]
    const { current, total } = parseCapacity(targetPost.capacity)

    if (!alreadyJoined && (joinedLockPostId !== null || current >= total)) {
      return
    }

    const nextCurrent = alreadyJoined ? Math.max(0, current - 1) : Math.min(total, current + 1)
    const nextMessage = alreadyJoined
      ? `${targetPost.title} 참여 신청을 취소했어요.`
      : `${targetPost.title} 참여 신청했습니다. (${nextCurrent}/${total})`
    const nextPost = withAuthDisplayState(
      updatePostJoinState(targetPost, !alreadyJoined),
      authUserId,
      isAuthenticated,
    )

    setLocalPosts((currentPosts) => replacePost(currentPosts, nextPost))
    setPostOverrides((currentOverrides) => ({
      ...currentOverrides,
      [postId]: nextPost,
    }))
    setJoinedPostId(alreadyJoined ? null : postId)
    setPartyStatusMessage(nextMessage)
    joinRequestPostIdRef.current = postId

    joinMutation.mutate(
      { postId, isJoining: !alreadyJoined },
      {
        onSuccess: async (serverPost) => {
          const confirmedPost = withAuthDisplayState(serverPost, authUserId, isAuthenticated)

          setLocalPosts((currentPosts) => replacePost(currentPosts, confirmedPost))
          setPostOverrides((currentOverrides) => ({
            ...currentOverrides,
            [postId]: confirmedPost,
          }))
          onPartyMessage(confirmedPost, nextMessage)
          await queryClient.invalidateQueries({ queryKey: COMMUNITY_PARTY_POSTS_QUERY_KEY })

          const refreshedPosts = queryClient.getQueryData<PartyPostsResult>(partyQueryKey)

          if (refreshedPosts?.source === 'api') {
            setPostOverrides((currentOverrides) => removePostOverride(currentOverrides, postId))
          }
        },
        onError: (error) => {
          setLocalPosts((currentPosts) => replacePost(currentPosts, targetPost))
          setPostOverrides((currentOverrides) =>
            restorePostOverride(currentOverrides, postId, previousOverride),
          )
          setJoinedPostId(previousJoinedPostId)
          setPartyStatusMessage(
            readMutationErrorMessage(error, '파티 참여 상태를 되돌렸습니다.'),
          )
        },
        onSettled: (_data, error) => {
          joinRequestPostIdRef.current = null

          if (error) {
            void queryClient.invalidateQueries({ queryKey: COMMUNITY_PARTY_POSTS_QUERY_KEY })
          }
        },
      },
    )
  }

  return {
    capacityDraft,
    changeFilter,
    composeError,
    currentPage: safePage,
    deadlineDraft,
    descriptionDraft,
    filteredCount: filteredPartyPosts.length,
    focusCompose,
    isCreating: createMutation.isPending,
    isAuthenticated,
    isLoading: partyQuery.isPending,
    isUnavailable: isPartyListUnavailable,
    joinedPostId: activeJoinedPostId,
    joiningPostId: joinMutation.isPending ? joinMutation.variables?.postId ?? null : null,
    emptyMessage,
    minDeadline,
    modeDraft,
    pageItems,
    searchDraft,
    selectedFilter,
    setCapacityDraft,
    setCurrentPage,
    setDeadlineDraft,
    setDescriptionDraft,
    setModeDraft,
    setTagsDraft,
    setTierDraft,
    setTitleDraft,
    statusMessage,
    submitPartyPost,
    submitSearch,
    tagsDraft,
    tierDraft,
    titleDraft,
    titleInputRef,
    toggleJoin,
    totalPages,
    updateSearchDraft,
  }
}
