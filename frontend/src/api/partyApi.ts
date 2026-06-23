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
export type PartyPostsSource = 'api' | 'unavailable'

const defaultPartyTier = '제한 없음'
const partyTierTags = new Set(['마스터+', '다이아+', '플래티넘+'])

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

export interface PartyPostsQueryParams {
  mode?: PartyMode
}

export interface CreatePartyPostRequest {
  capacity: string
  deadline: string
  description: string
  mode: PartyMode
  tags: string[]
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

function unavailablePartyPosts(): PartyPostsResult {
  return { data: [], source: 'unavailable' }
}

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

function readPartyTier(tags: string[]) {
  return tags.find((tag) => partyTierTags.has(tag)) ?? defaultPartyTier
}

function formatCloseLabel(value: string | null | undefined) {
  if (!value) return '마감 시간 없음'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  if (date.getTime() <= Date.now()) {
    return '마감됨'
  }

  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const today = new Date()
  const isToday = date.getFullYear() === today.getFullYear()
    && date.getMonth() === today.getMonth()
    && date.getDate() === today.getDate()

  if (isToday) {
    return `오늘 ${hours}:${minutes} 마감`
  }

  return `${date.getMonth() + 1}/${date.getDate()} ${hours}:${minutes} 마감`
}

function toGameMode(mode: PartyMode) {
  if (mode === '일반') return 'NORMAL_TFT'
  if (mode === '커스텀') return 'CUSTOM'
  return 'RANKED_TFT'
}

function buildPartyPostRequestParams(params: PartyPostsQueryParams) {
  if (!params.mode) {
    return undefined
  }

  return {
    mode: toGameMode(params.mode),
  }
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

  const response = payload as PartyPostResponse
  if (readOptionalId(response.partyPostId ?? response.id) === undefined) {
    throw new Error(fallbackMessage)
  }

  return normalizePartyPost(response, 0)
}

function normalizePartyPost(response: PartyPostResponse, index: number): PartyPost {
  const mode = normalizeMode(response.gameMode ?? response.mode)
  const tags = readTags(response.tags).slice(0, 4)
  const tier = readPartyTier(tags)
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
    close: formatCloseLabel(response.deadline ?? response.close),
    status: normalizeStatus(response.status, capacity, isClosed),
    description: readString(response.description ?? response.content, '상세 설명이 없습니다.'),
    tags,
    isJoined: readBoolean(response.isJoined ?? response.joined),
    userId: readOptionalId(response.userId),
    ...style,
  }
}

export async function getPartyPosts(params: PartyPostsQueryParams = {}): Promise<PartyPostsResult> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<PartyPostsPayload> | PartyPostsPayload>(
      '/community/parties',
      { params: buildPartyPostRequestParams(params) },
    )

    if (hasFailedApiResponse(data)) {
      return unavailablePartyPosts()
    }

    const payload = unwrapApiResponse(data)
    const payloadResult = readPartyListPayload(payload)

    if (!payloadResult.isValid) {
      return unavailablePartyPosts()
    }

    const posts = payloadResult.posts.map(normalizePartyPost)

    return { data: posts, source: 'api' }
  } catch {
    return unavailablePartyPosts()
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
