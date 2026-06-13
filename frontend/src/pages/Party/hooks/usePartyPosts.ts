import { useEffect, useMemo, useRef, useState } from 'react'
import { initialPartyPosts } from '../data/partyMockData'
import type { PartyFilter } from '../partyFilters'
import type { PartyMode, PartyPost } from '../types'
import {
  filterPartyPosts,
  getPostStyle,
  normalizeCapacity,
  paginatePartyPosts,
  parseCapacity,
} from '../utils/partyUtils'

const PARTY_PAGE_SIZE = 3

interface UsePartyPostsOptions {
  onPartyMessage: (message: string) => void
}

export function usePartyPosts({ onPartyMessage }: UsePartyPostsOptions) {
  const [posts, setPosts] = useState<PartyPost[]>(initialPartyPosts)
  const [selectedFilter, setSelectedFilter] = useState<PartyFilter>('전체')
  const [searchDraft, setSearchDraft] = useState('')
  const [query, setQuery] = useState('')
  const [joinedPostId, setJoinedPostId] = useState<string | null>(null)
  const [titleDraft, setTitleDraft] = useState('')
  const [modeDraft, setModeDraft] = useState<PartyMode>('랭크')
  const [tierDraft, setTierDraft] = useState('마스터+')
  const [capacityDraft, setCapacityDraft] = useState('2')
  const [tagsDraft, setTagsDraft] = useState('')
  const [descriptionDraft, setDescriptionDraft] = useState('')
  const [composeError, setComposeError] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const titleInputRef = useRef<HTMLInputElement>(null)

  const filteredPartyPosts = useMemo(
    () => filterPartyPosts(posts, selectedFilter, query),
    [posts, query, selectedFilter],
  )
  const { pageItems, safePage, totalPages } = useMemo(
    () => paginatePartyPosts(filteredPartyPosts, currentPage, PARTY_PAGE_SIZE),
    [currentPage, filteredPartyPosts],
  )

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
  }

  const submitPartyPost = () => {
    const title = titleDraft.trim()
    const description = descriptionDraft.trim()

    if (!title || !description) {
      setComposeError('모집글 제목과 플레이 스타일을 입력해주세요.')
      return
    }

    const style = getPostStyle(modeDraft, tierDraft)
    const parsedTags = tagsDraft
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean)
      .slice(0, 4)
    const nextPost: PartyPost = {
      id: `party-${crypto.randomUUID()}`,
      title,
      mode: modeDraft,
      tier: tierDraft,
      capacity: normalizeCapacity(capacityDraft),
      close: '방금 등록',
      status: '모집중',
      description,
      tags: parsedTags.length > 0 ? parsedTags : [modeDraft, tierDraft, '방금 등록'],
      ...style,
    }

    setPosts((currentPosts) => [nextPost, ...currentPosts])
    setSelectedFilter('전체')
    setSearchDraft('')
    setQuery('')
    setTitleDraft('')
    setModeDraft('랭크')
    setTierDraft('마스터+')
    setCapacityDraft('2')
    setTagsDraft('')
    setDescriptionDraft('')
    setComposeError('')
    setCurrentPage(1)
  }

  const toggleJoin = (postId: string) => {
    const targetPost = posts.find((post) => post.id === postId)

    if (!targetPost) {
      return
    }

    const alreadyJoined = joinedPostId === postId
    const { current, total } = parseCapacity(targetPost.capacity)

    if (!alreadyJoined && (joinedPostId !== null || current >= total)) {
      return
    }

    const nextCurrent = alreadyJoined ? Math.max(0, current - 1) : Math.min(total, current + 1)
    const nextMessage = alreadyJoined
      ? `${targetPost.title} 참여 신청을 취소했어요.`
      : `${targetPost.title} 참여 신청했습니다. (${nextCurrent}/${total})`

    setPosts((currentPosts) =>
      currentPosts.map((post) => {
        if (post.id !== postId) {
          return post
        }

        return {
          ...post,
          capacity: `${nextCurrent}/${total}`,
          status: nextCurrent >= total ? '대기중' : '모집중',
        }
      }),
    )

    setJoinedPostId(alreadyJoined ? null : postId)
    onPartyMessage(nextMessage)
  }

  return {
    capacityDraft,
    changeFilter,
    composeError,
    currentPage: safePage,
    descriptionDraft,
    filteredCount: filteredPartyPosts.length,
    focusCompose,
    joinedPostId,
    modeDraft,
    pageItems,
    searchDraft,
    selectedFilter,
    setCapacityDraft,
    setCurrentPage,
    setDescriptionDraft,
    setModeDraft,
    setTagsDraft,
    setTierDraft,
    setTitleDraft,
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
