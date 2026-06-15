import axiosInstance from './axiosInstance'
import {
  normalizeCommunityChatRoomId,
  PARTY_RECRUITMENT_ROOM_ID,
  type CommunityChatRoomId,
} from '../constants/communityChatRooms'
import { isRecord, unwrapApiResponse, type ApiResponse } from './apiResponse'

export type PartyMode = '랭크' | '일반' | '커스텀'
export type PartyStatus = '모집중' | '대기중'
export type PartyIcon = 'crown' | 'leaf' | 'spark' | 'swords'
export type PartyTone = 'purple' | 'green' | 'cyan' | 'gold'
export type PartyPostsSource = 'api' | 'fallback'

export interface PartyPost {
  capacity: string
  chatRoomId: CommunityChatRoomId
  close: string
  description: string
  icon: PartyIcon
  id: string
  isJoined?: boolean
  isOwner?: boolean
  mode: PartyMode
  status: PartyStatus
  tags: string[]
  tier: string
  title: string
  tone: PartyTone
  userId?: string
}

export interface PartyPostsResult {
  data: PartyPost[]
  source: PartyPostsSource
}

export interface CreatePartyPostRequest {
  capacity: string
  deadline: string
  description: string
  mode: PartyMode
  tags: string[]
  tier: string
  title: string
}

interface PartyPostResponse {
  capacity?: string | null
  chatRoomId?: string | null
  close?: string | null
  closed?: boolean | null
  content?: string | null
  createdAt?: string | null
  currentMembers?: number | string | null
  deadline?: string | null
  description?: string | null
  gameMode?: string | null
  id?: number | string | null
  isClosed?: boolean | null
  isJoined?: boolean | null
  joined?: boolean | null
  maxMembers?: number | string | null
  memberCount?: number | string | null
  mode?: string | null
  partyPostId?: number | string | null
  status?: string | null
  tags?: string[] | string | null
  tier?: string | null
  title?: string | null
  userId?: number | string | null
}

type PartyPostsPayload = PartyPostResponse[] | {
  content?: PartyPostResponse[]
  items?: PartyPostResponse[]
  parties?: PartyPostResponse[]
  partyPosts?: PartyPostResponse[]
}

interface PartyListPayloadResult {
  isValid: boolean
  posts: PartyPostResponse[]
}

export const fallbackPartyPosts: PartyPost[] = [
  {
    id: 'party-master-duo',
    chatRoomId: PARTY_RECRUITMENT_ROOM_ID,
    title: '마스터 이상 듀오 구합니다',
    mode: '랭크',
    tier: '마스터+',
    capacity: '2/2',
    close: '마감 15분 전',
    status: '모집중',
    description: '17.3 추천 메타 기준으로 빠르게 점수 올리실 분 찾아요.',
    tags: ['음성 가능', '연승 목표', '빠른 매칭'],
    icon: 'crown',
    tone: 'purple',
  },
  {
    id: 'party-diamond-practice',
    chatRoomId: PARTY_RECRUITMENT_ROOM_ID,
    title: '다이아 구간 야부/연습 같이해요',
    mode: '랭크',
    tier: '다이아+',
    capacity: '1/2',
    close: '마감 42분 전',
    status: '모집중',
    description: '운영 피드백 주고받으면서 편하게 연습하실 분이면 좋아요.',
    tags: ['피드백 환영', '저녁 접속', '마이크 선택'],
    icon: 'leaf',
    tone: 'green',
  },
  {
    id: 'party-casual-evening',
    chatRoomId: PARTY_RECRUITMENT_ROOM_ID,
    title: '저녁 근접, 편하게 즐기실 분!',
    mode: '일반',
    tier: '제한 없음',
    capacity: '3/4',
    close: '마감 1시간 전',
    status: '대기중',
    description: '랭크 부담 없이 조합 테스트하면서 같이 하실 분 구해요.',
    tags: ['초보 환영', '일반전', '덱 실험'],
    icon: 'spark',
    tone: 'cyan',
  },
  {
    id: 'party-weekend-master',
    chatRoomId: PARTY_RECRUITMENT_ROOM_ID,
    title: '주말 마스터 달성 목표!',
    mode: '랭크',
    tier: '플래티넘+',
    capacity: '2/3',
    close: '마감 2시간 전',
    status: '모집중',
    description: '순방 위주로 안정적인 운영 맞춰가실 분이면 좋습니다.',
    tags: ['주말 집중', '순방 운영', '멘탈 좋음'],
    icon: 'swords',
    tone: 'gold',
  },
]

function readString(value: unknown, fallback = '') {
  return typeof value === 'string' ? value : fallback
}

function readId(value: unknown, fallback: string) {
  if (typeof value === 'string' && value.trim().length > 0) return value
  if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  return fallback
}

function readOptionalId(value: unknown) {
  if (typeof value === 'string' && value.trim().length > 0) return value
  if (typeof value === 'number' && Number.isFinite(value)) return String(value)
  return undefined
}

function readNumber(value: unknown): number | undefined {
  if (typeof value === 'number' && Number.isFinite(value)) return value

  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : undefined
  }

  return undefined
}

function readBoolean(value: unknown) {
  return typeof value === 'boolean' ? value : false
}

function readTags(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
  }

  if (typeof value !== 'string') return []

  try {
    const parsed: unknown = JSON.parse(value)
    if (Array.isArray(parsed)) {
      return parsed.filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    }
  } catch {
    return value
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean)
  }

  return []
}

function normalizeMode(value: unknown): PartyMode {
  if (value === '일반' || value === 'NORMAL' || value === 'NORMAL_TFT') return '일반'
  if (value === '커스텀' || value === 'CUSTOM') return '커스텀'
  return '랭크'
}

function normalizeStatus(value: unknown, capacity: string, isClosed = false): PartyStatus {
  if (isClosed) {
    return '대기중'
  }

  if (value === '대기중' || value === 'FULL' || value === 'WAITING' || value === 'CLOSED') {
    return '대기중'
  }

  const [currentRaw, totalRaw] = capacity.split('/').map(Number)
  if (Number.isFinite(currentRaw) && Number.isFinite(totalRaw) && currentRaw >= totalRaw) {
    return '대기중'
  }

  return '모집중'
}

function normalizeCapacity(response: PartyPostResponse) {
  const capacity = readString(response.capacity).trim()

  if (/^\d+\/\d+$/.test(capacity)) {
    return capacity
  }

  const currentMembers = readNumber(response.currentMembers ?? response.memberCount) ?? 1
  const maxMembers = readNumber(response.maxMembers) ?? 2
  const total = Math.max(1, maxMembers)
  const current = Math.min(Math.max(0, currentMembers), total)

  return `${current}/${total}`
}

function getPostStyle(mode: PartyMode, tier: string): Pick<PartyPost, 'icon' | 'tone'> {
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

function formatCloseLabel(value: string | null | undefined) {
  if (!value) return '방금 등록'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return date.getTime() > Date.now() ? '마감 예정' : '방금 등록'
}

function toGameMode(mode: PartyMode) {
  if (mode === '일반') return 'NORMAL_TFT'
  if (mode === '커스텀') return 'CUSTOM'
  return 'RANKED_TFT'
}

function readPartyPostArray(value: unknown[]): PartyListPayloadResult {
  const posts = value.filter((item): item is PartyPostResponse => isRecord(item))

  return {
    isValid: posts.length === value.length,
    posts,
  }
}

function readPartyListPayload(payload: unknown): PartyListPayloadResult {
  if (Array.isArray(payload)) {
    return readPartyPostArray(payload)
  }

  if (!isRecord(payload)) {
    return { isValid: false, posts: [] }
  }

  const candidates = [payload.content, payload.items, payload.parties, payload.partyPosts]
  const list = candidates.find(Array.isArray)

  return Array.isArray(list) ? readPartyPostArray(list) : { isValid: false, posts: [] }
}

function hasFailedApiResponse(payload: unknown) {
  return isRecord(payload) && payload.success === false
}

function getPartyApiErrorMessage(error: unknown, fallbackMessage: string) {
  if (isRecord(error)) {
    const response = error.response
    if (isRecord(response)) {
      const data = response.data
      if (isRecord(data) && typeof data.message === 'string' && data.message.trim()) {
        return data.message
      }
    }

    if (typeof error.message === 'string' && error.message.trim()) {
      return error.message
    }
  }

  return fallbackMessage
}

function normalizeRequiredPartyPost(payload: unknown, fallbackMessage: string): PartyPost {
  if (!isRecord(payload)) {
    throw new Error(fallbackMessage)
  }

  return normalizePartyPost(payload, 0)
}

function normalizePartyPost(response: PartyPostResponse, index: number): PartyPost {
  const mode = normalizeMode(response.gameMode ?? response.mode)
  const tier = readString(response.tier, '제한 없음')
  const capacity = normalizeCapacity(response)
  const style = getPostStyle(mode, tier)
  const id = readId(response.partyPostId ?? response.id, `party-api-${index}`)
  const chatRoomId = normalizeCommunityChatRoomId(readId(response.chatRoomId, PARTY_RECRUITMENT_ROOM_ID))
    ?? PARTY_RECRUITMENT_ROOM_ID
  const isClosed = readBoolean(response.closed ?? response.isClosed)

  return {
    id,
    chatRoomId,
    title: readString(response.title, '제목 없는 모집글'),
    mode,
    tier,
    capacity,
    close: formatCloseLabel(response.deadline ?? response.close ?? response.createdAt),
    status: normalizeStatus(response.status, capacity, isClosed),
    description: readString(response.description ?? response.content, '상세 설명이 없습니다.'),
    tags: readTags(response.tags).slice(0, 4),
    isJoined: readBoolean(response.isJoined ?? response.joined),
    userId: readOptionalId(response.userId),
    ...style,
  }
}

export async function getPartyPosts(): Promise<PartyPostsResult> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<PartyPostsPayload> | PartyPostsPayload>('/community/parties')

    if (hasFailedApiResponse(data)) {
      return { data: fallbackPartyPosts, source: 'fallback' }
    }

    const payload = unwrapApiResponse(data)
    const payloadResult = readPartyListPayload(payload)

    if (!payloadResult.isValid) {
      return { data: fallbackPartyPosts, source: 'fallback' }
    }

    const posts = payloadResult.posts.map(normalizePartyPost)

    return { data: posts, source: 'api' }
  } catch {
    return { data: fallbackPartyPosts, source: 'fallback' }
  }
}

export async function createPartyPost(request: CreatePartyPostRequest): Promise<PartyPost> {
  try {
    const { data } = await axiosInstance.post<ApiResponse<PartyPostResponse | null> | PartyPostResponse | null>(
      '/community/parties',
      {
        title: request.title,
        content: request.description,
        deadline: request.deadline,
        gameMode: toGameMode(request.mode),
        maxMembers: readNumber(request.capacity.split('/')[1]) ?? 2,
        tags: request.tags,
      },
    )

    if (hasFailedApiResponse(data)) {
      throw new Error('파티 모집글 등록 실패')
    }

    const payload = unwrapApiResponse(data)

    return normalizeRequiredPartyPost(payload, '파티 모집글 등록 응답이 올바르지 않습니다.')
  } catch (error) {
    throw new Error(getPartyApiErrorMessage(error, '파티 모집글 등록 실패'))
  }
}

export async function joinPartyPost(partyPostId: string): Promise<PartyPost> {
  try {
    const { data } = await axiosInstance.post<ApiResponse<PartyPostResponse | null> | PartyPostResponse | null>(
      `/community/parties/${partyPostId}/join`,
    )

    if (hasFailedApiResponse(data)) {
      throw new Error('파티 참여 요청 실패')
    }

    const payload = unwrapApiResponse(data)

    return normalizeRequiredPartyPost(payload, '파티 참여 응답이 올바르지 않습니다.')
  } catch (error) {
    throw new Error(getPartyApiErrorMessage(error, '파티 참여 요청 실패'))
  }
}

export async function cancelPartyJoin(partyPostId: string): Promise<PartyPost> {
  try {
    const { data } = await axiosInstance.delete<ApiResponse<PartyPostResponse | null> | PartyPostResponse | null>(
      `/community/parties/${partyPostId}/join`,
    )

    if (hasFailedApiResponse(data)) {
      throw new Error('파티 참여 취소 실패')
    }

    const payload = unwrapApiResponse(data)

    return normalizeRequiredPartyPost(payload, '파티 참여 취소 응답이 올바르지 않습니다.')
  } catch (error) {
    throw new Error(getPartyApiErrorMessage(error, '파티 참여 취소 실패'))
  }
}
