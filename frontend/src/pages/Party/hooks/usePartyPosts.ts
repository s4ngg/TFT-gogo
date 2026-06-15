import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useRef, useState } from 'react'
import {
  buildLocalPartyPost,
  cancelPartyJoin,
  createPartyPost,
  fallbackPartyPosts,
  getPartyPosts,
  joinPartyPost,
  type CreatePartyPostRequest,
  type PartyPostsResult,
} from '../../../api/partyApi'
import useAuthStore from '../../../store/useAuthStore'
import type { PartyFilter } from '../partyFilters'
import type { PartyMode, PartyPost } from '../types'
import {
  filterPartyPosts,
  formatDeadlineForRequest,
  getDefaultDeadlineInput,
  normalizeCapacity,
  paginatePartyPosts,
  parseCapacity,
  removePostOverride,
  replacePost,
  restorePostOverride,
  updatePostJoinState,
} from '../utils/partyUtils'

const PARTY_PAGE_SIZE = 3
const PARTY_QUERY_KEY = ['community', 'parties'] as const

interface UsePartyPostsOptions {
  onPartyMessage: (post: PartyPost, message: string) => void
  onPartyPostCreated: (post: PartyPost, message?: string) => void
}

interface JoinMutationVariables {
  isJoining: boolean
  postId: string
}

function readAuthUserId(value: number | string | undefined) {
  if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  if (typeof value === 'string' && value.trim().length > 0) return value
  return null
}

function withOwnerState(post: PartyPost, authUserId: string | null): PartyPost {
  const isOwner = authUserId !== null && post.userId === authUserId

  return {
    ...post,
    isOwner,
  }
}

export function usePartyPosts({ onPartyMessage, onPartyPostCreated }: UsePartyPostsOptions) {
  const queryClient = useQueryClient()
  const authUserId = useAuthStore((state) => readAuthUserId(state.user?.id))
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

  const partyQuery = useQuery({
    queryKey: PARTY_QUERY_KEY,
    queryFn: getPartyPosts,
    staleTime: 1000 * 60,
  })
  const createMutation = useMutation({
    mutationFn: createPartyPost,
  })
  const joinMutation = useMutation({
    mutationFn: ({ postId, isJoining }: JoinMutationVariables) =>
      isJoining ? joinPartyPost(postId) : cancelPartyJoin(postId),
  })

  const posts = useMemo(() => {
    const serverPosts = partyQuery.data?.data ?? fallbackPartyPosts
    const mergedPosts = [...localPosts]
    const localPostIds = new Set(localPosts.map((post) => post.id))

    serverPosts.forEach((post) => {
      if (!localPostIds.has(post.id)) {
        mergedPosts.push(postOverrides[post.id] ?? post)
      }
    })

    return mergedPosts.map((post) => withOwnerState(postOverrides[post.id] ?? post, authUserId))
  }, [authUserId, localPosts, partyQuery.data?.data, postOverrides])
  const activeJoinedPostId = useMemo(() => {
    if (joinedPostId !== undefined) {
      return joinedPostId
    }

    return posts.find((post) => post.isJoined === true)?.id ?? null
  }, [joinedPostId, posts])
  const filteredPartyPosts = useMemo(
    () => filterPartyPosts(posts, selectedFilter, query),
    [posts, query, selectedFilter],
  )
  const { pageItems, safePage, totalPages } = useMemo(
    () => paginatePartyPosts(filteredPartyPosts, currentPage, PARTY_PAGE_SIZE),
    [currentPage, filteredPartyPosts],
  )
  const statusMessage = partyStatusMessage
    || (partyQuery.data?.source === 'fallback'
      ? '파티 API 응답을 불러오지 못해 목업 모집글을 표시합니다.'
      : '')

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
    const localId = `party-${crypto.randomUUID()}`
    const localPost = withOwnerState(
      {
        ...buildLocalPartyPost(request, localId),
        isJoined: authUserId !== null,
        userId: authUserId ?? undefined,
      },
      authUserId,
    )

    setLocalPosts((currentPosts) => [localPost, ...currentPosts])
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

    createMutation.mutate(request, {
      onSuccess: (createdPost) => {
        if (createdPost) {
          const serverPost = withOwnerState(createdPost, authUserId)

          setLocalPosts((currentPosts) =>
            currentPosts.map((post) => (post.id === localId ? serverPost : post)),
          )
          onPartyPostCreated(serverPost)
        }

        void queryClient.invalidateQueries({ queryKey: PARTY_QUERY_KEY })
      },
      onError: () => {
        setPartyStatusMessage('API 연결 실패로 방금 등록한 글은 임시로만 표시됩니다.')
      },
    })
  }

  const toggleJoin = (postId: string) => {
    if (joinRequestPostIdRef.current !== null || joinMutation.isPending) {
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
    const nextPost = withOwnerState(updatePostJoinState(targetPost, !alreadyJoined), authUserId)

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
          const confirmedPost = withOwnerState(serverPost ?? nextPost, authUserId)

          setLocalPosts((currentPosts) => replacePost(currentPosts, confirmedPost))
          setPostOverrides((currentOverrides) => ({
            ...currentOverrides,
            [postId]: confirmedPost,
          }))
          onPartyMessage(confirmedPost, nextMessage)
          await queryClient.invalidateQueries({ queryKey: PARTY_QUERY_KEY })

          const refreshedPosts = queryClient.getQueryData<PartyPostsResult>(PARTY_QUERY_KEY)

          if (refreshedPosts?.source === 'api') {
            setPostOverrides((currentOverrides) => removePostOverride(currentOverrides, postId))
          }
        },
        onError: () => {
          setLocalPosts((currentPosts) => replacePost(currentPosts, targetPost))
          setPostOverrides((currentOverrides) =>
            restorePostOverride(currentOverrides, postId, previousOverride),
          )
          setJoinedPostId(previousJoinedPostId)
          setPartyStatusMessage('API 연결 실패로 참여 상태를 되돌렸습니다.')
        },
        onSettled: (_data, error) => {
          joinRequestPostIdRef.current = null

          if (error) {
            void queryClient.invalidateQueries({ queryKey: PARTY_QUERY_KEY })
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
    joinedPostId: activeJoinedPostId,
    joiningPostId: joinMutation.isPending ? joinMutation.variables?.postId ?? null : null,
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
