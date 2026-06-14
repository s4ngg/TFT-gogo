import type { PartyFilter } from '../partyFilters'
import type { PartyMode, PartyPost, PartyPostStyle } from '../types'

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
