import type { PartyFilter } from '../partyFilters'
import type { PartyMode, PartyPost, PartyPostStyle } from '../types'

interface PartyJoinActionStateOptions {
  hasJoinedOtherPost: boolean
  isAuthenticated: boolean
  isFull: boolean
  isJoined: boolean
  isJoinPending: boolean
  isOwner: boolean
}

export function getCurrentTime() {
  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date())
}

export function normalizeCapacity(value: string) {
  const trimmedValue = value.trim()
  const slashMatch = trimmedValue.match(/^(\d+)\/(\d+)$/)

  if (slashMatch) {
    const current = Number(slashMatch[1])
    const total = Number(slashMatch[2])

    if (Number.isFinite(current) && Number.isFinite(total) && total > 0) {
      return `${Math.min(current, total)}/${total}`
    }
  }

  const total = Number(trimmedValue)

  if (Number.isFinite(total) && total > 0) {
    return `1/${total}`
  }

  return '1/2'
}

export function parseCapacity(capacity: string) {
  const [currentRaw, totalRaw] = capacity.split('/').map(Number)
  const total = Number.isFinite(totalRaw) && totalRaw > 0 ? totalRaw : 2
  const current = Number.isFinite(currentRaw) ? Math.min(Math.max(currentRaw, 0), total) : 0

  return { current, total }
}

export function replacePost(posts: PartyPost[], nextPost: PartyPost) {
  return posts.map((post) => (post.id === nextPost.id ? nextPost : post))
}

export function restorePostOverride(
  overrides: Record<string, PartyPost>,
  postId: string,
  previousOverride: PartyPost | undefined,
) {
  if (previousOverride) {
    return {
      ...overrides,
      [postId]: previousOverride,
    }
  }

  const nextOverrides = { ...overrides }
  delete nextOverrides[postId]

  return nextOverrides
}

export function removePostOverride(overrides: Record<string, PartyPost>, postId: string) {
  const nextOverrides = { ...overrides }
  delete nextOverrides[postId]

  return nextOverrides
}

export function updatePostJoinState(post: PartyPost, isJoining: boolean): PartyPost {
  const { current, total } = parseCapacity(post.capacity)
  const nextCurrent = isJoining ? Math.min(total, current + 1) : Math.max(0, current - 1)

  return {
    ...post,
    capacity: `${nextCurrent}/${total}`,
    isJoined: isJoining,
    status: nextCurrent >= total ? '대기중' : '모집중',
  }
}

export function getPartyJoinActionState({
  hasJoinedOtherPost,
  isAuthenticated,
  isFull,
  isJoined,
  isJoinPending,
  isOwner,
}: PartyJoinActionStateOptions) {
  if (isOwner) {
    return { disabled: true, label: '작성자' }
  }

  if (isJoinPending) {
    return { disabled: true, label: '처리중' }
  }

  if (isJoined) {
    return { disabled: false, label: '참여중' }
  }

  if (isFull) {
    return { disabled: true, label: '마감' }
  }

  if (!isAuthenticated) {
    return { disabled: true, label: '로그인 후 참여' }
  }

  if (hasJoinedOtherPost) {
    return { disabled: true, label: '잠김' }
  }

  return { disabled: false, label: '참여' }
}

export function getDefaultDeadlineInput() {
  const deadline = new Date(Date.now() + 1000 * 60 * 60 * 2)
  const year = deadline.getFullYear()
  const month = String(deadline.getMonth() + 1).padStart(2, '0')
  const day = String(deadline.getDate()).padStart(2, '0')
  const hour = String(deadline.getHours()).padStart(2, '0')
  const minute = String(deadline.getMinutes()).padStart(2, '0')

  return `${year}-${month}-${day}T${hour}:${minute}`
}

export function formatDeadlineForRequest(value: string) {
  const trimmedValue = value.trim()

  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(trimmedValue)) {
    return `${trimmedValue}:00`
  }

  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(trimmedValue)) {
    return trimmedValue
  }

  return null
}

export function getPostStyle(mode: PartyMode, tier: string): PartyPostStyle {
  if (mode === '일반') {
    return { icon: 'spark', tone: 'cyan' }
  }

  if (mode === '커스텀') {
    return { icon: 'swords', tone: 'gold' }
  }

  if (tier.includes('다이아')) {
    return { icon: 'leaf', tone: 'green' }
  }

  return { icon: 'crown', tone: 'purple' }
}

export function filterPartyPosts(posts: PartyPost[], selectedFilter: PartyFilter, query: string) {
  const normalizedQuery = query.trim().toLowerCase()

  return posts.filter((post) => {
    const matchesFilter = selectedFilter === '전체' || post.mode === selectedFilter
    const searchableText = [
      post.title,
      post.mode,
      post.tier,
      post.description,
      ...post.tags,
    ].join(' ').toLowerCase()
    const matchesQuery = normalizedQuery.length === 0 || searchableText.includes(normalizedQuery)

    return matchesFilter && matchesQuery
  })
}

export function paginatePartyPosts(posts: PartyPost[], currentPage: number, pageSize: number) {
  const totalPages = Math.max(1, Math.ceil(posts.length / pageSize))
  const safePage = Math.min(Math.max(currentPage, 1), totalPages)
  const startIndex = (safePage - 1) * pageSize

  return {
    pageItems: posts.slice(startIndex, startIndex + pageSize),
    safePage,
    totalPages,
  }
}
