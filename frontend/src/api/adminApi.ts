import axiosInstance from './axiosInstance'
import type { AxiosRequestConfig } from 'axios'
import type { RankFilter } from '../pages/Dashboard/dashboardData'
import type { AdminRole } from '../types/admin'
import { clearAdminSession } from '../hooks/useAdminSession'

const GUIDE_CDRAGON_IMPORT_TIMEOUT_MS = 120_000
const PATCH_NOTE_RIOT_IMPORT_TIMEOUT_MS = 120_000

// ── In-memory access token (XSS로부터 보호) ────────────────────────────────
// Refresh Token은 HttpOnly 쿠키로 서버가 관리한다.
let _accessToken: string | null = null
let _refreshPromise: Promise<string> | null = null

export function setAccessToken(token: string): void {
  _accessToken = token
}

export function clearAccessToken(): void {
  _accessToken = null
}

export function getAccessToken(): string | null {
  return _accessToken
}

function adminAuthHeaders() {
  return _accessToken ? { Authorization: `Bearer ${_accessToken}` } : {}
}

export function getHttpStatus(error: unknown): number | undefined {
  if (typeof error !== 'object' || error === null || !('response' in error)) {
    return undefined
  }

  const response = (error as { response?: { status?: unknown } }).response
  return typeof response?.status === 'number' ? response.status : undefined
}

export function isAdminAuthFailure(error: unknown): boolean {
  const status = getHttpStatus(error)
  return status === 401 || status === 403
}

export function isNetworkOrTimeoutError(error: unknown): boolean {
  if (typeof error !== 'object' || error === null) return false
  if ('response' in error) return false
  if ('code' in error) {
    const code = (error as { code?: string }).code
    return code === 'ECONNABORTED' || code === 'ERR_NETWORK' || code === 'ETIMEDOUT'
  }
  return true
}

export function getServerErrorStatus(error: unknown): number | undefined {
  const status = getHttpStatus(error)
  if (status != null && status >= 500) return status
  return undefined
}

interface AdminRequestError extends Error {
  cause?: unknown
  response?: { status?: unknown }
}

function createAdminRequestError(error: unknown, message: string): AdminRequestError {
  const wrappedError = new Error(message) as AdminRequestError
  wrappedError.cause = error

  if (typeof error === 'object' && error !== null && 'response' in error) {
    wrappedError.response = (error as { response?: { status?: unknown } }).response
  }

  return wrappedError
}

// ── 인증 API ────────────────────────────────────────────────────────────────

export interface AdminLoginPayload {
  username: string
  password: string
}

export interface AdminLoginResult {
  accessToken: string
  username: string
  role: AdminRole
}

export async function adminLogin(payload: AdminLoginPayload): Promise<AdminLoginResult> {
  const { data } = await axiosInstance.post<{ data: AdminLoginResult }>(
    '/admin/auth/login',
    payload,
    { withCredentials: true },
  )
  return data.data
}

export async function adminRefresh(): Promise<string> {
  const { data } = await axiosInstance.post<{ data: { accessToken: string } }>(
    '/admin/auth/refresh',
    null,
    { withCredentials: true },
  )
  return data.data.accessToken
}

// 관리자 API 401 → single-flight refresh → 재시도
axiosInstance.interceptors.response.use(
  (res) => res,
  async (error) => {
    const config = error.config as AxiosRequestConfig & { _adminRetried?: boolean }
    const isAdminPath =
      typeof config?.url === 'string' &&
      config.url.startsWith('/admin/') &&
      !config.url.startsWith('/admin/auth/')
    const is401 = error.response?.status === 401

    if (!isAdminPath || !is401 || config._adminRetried) {
      return Promise.reject(error)
    }

    config._adminRetried = true

    // single-flight: 동시에 여러 401이 와도 refresh는 한 번만
    if (!_refreshPromise) {
      _refreshPromise = adminRefresh().finally(() => {
        _refreshPromise = null
      })
    }

    try {
      const newToken = await _refreshPromise
      setAccessToken(newToken)
      if (config.headers) {
        (config.headers as Record<string, string>)['Authorization'] = `Bearer ${newToken}`
      }
      return axiosInstance(config)
    } catch {
      clearAccessToken()
      clearAdminSession()
      return Promise.reject(error)
    }
  },
)

export async function adminLogout(): Promise<void> {
  await axiosInstance.post('/admin/auth/logout', null, { withCredentials: true })
}

// ── 덱 ─────────────────────────────────────────────────────────────────────

export interface UnitInfo {
  characterId: string
  name: string
  imageUrl: string
}

export interface HeroAugmentEntry {
  championId: string
  championName: string
  augmentName: string
}

export interface AdminDeck {
  id: number
  signature: string
  rankFilter: string
  autoName: string
  customName: string | null
  displayName: string
  hidden: boolean
  sortPriority: number | null
  curatorNote: string | null
  boardPositions: string | null
  playGuide: string | null
  heroAugments: string | null
  grade: string
  winRate: string
  top4: string
  pickRate: string
  sampleSize: number
  units: UnitInfo[]
  traitSuffixes: string[]
}

export interface BoardPosition {
  row: number
  col: number
}

export interface PlayGuide {
  early: string
  mid: string
  late: string
}

export interface DeckCurationRequest {
  customName: string | null
  hidden: boolean
  sortPriority: number | null
  curatorNote: string | null
  boardPositions: string | null
  playGuide: string | null
  heroAugments: string | null
}

export async function fetchAdminDecks(rankFilter: RankFilter = 'MASTER_PLUS'): Promise<AdminDeck[]> {
  const { data } = await axiosInstance.get(`/admin/decks?rankFilter=${rankFilter}`, {
    headers: adminAuthHeaders(),
  })
  return data.data
}

export async function updateDeckCuration(deckId: number, req: DeckCurationRequest): Promise<AdminDeck> {
  const { data } = await axiosInstance.patch(`/admin/decks/${deckId}`, req, {
    headers: adminAuthHeaders(),
  })
  return data.data
}

export async function resetDeckCuration(deckId: number): Promise<void> {
  await axiosInstance.delete(`/admin/decks/${deckId}/curation`, {
    headers: adminAuthHeaders(),
  })
}

export async function triggerDeckAggregate(date?: string): Promise<void> {
  const params = date ? `?date=${date}` : ''
  await axiosInstance.post(`/admin/decks/meta/aggregate${params}`, null, {
    headers: adminAuthHeaders(),
  })
}

// ── 게임가이드 import ─────────────────────────────────────────────────────

export interface GuideCdragonImportRequest {
  includeAugments: boolean
  includeChampions: boolean
  includeItems: boolean
  includeTraits: boolean
  mutator: string
  patchVersion: string
  setNumber: number
}

export interface GuideImportResponse {
  augmentCount: number
  championCount: number
  createdCount: number
  importedCount: number
  itemCount: number
  mutator: string
  patchVersion: string
  setNumber: number
  skippedCount: number
  traitCount: number
  updatedCount: number
}

interface ApiResponse<T> {
  data: T
}

export type AdminPatchChangeCategory = 'CHAMPION' | 'TRAIT' | 'ITEM' | 'AUGMENT' | 'SYSTEM'
export type AdminPatchChangeType = 'BUFF' | 'NERF' | 'ADJUST' | 'NEW'
export type AdminPatchChangeImpact = 'HIGH' | 'MEDIUM' | 'LOW'

export interface AdminPatchNote {
  changeCount: number
  description: string | null
  focus: string | null
  highlights: string[]
  id: number
  imageUrl: string | null
  isCurrent: boolean
  publishedAt: string
  summary: string
  title: string
  version: string
}

export interface AdminPatchNotePayload {
  current: boolean
  description: string | null
  focus: string | null
  highlights: string[]
  imageUrl: string | null
  publishedAt: string
  summary: string
  title: string
  version: string
}

export interface AdminPatchNoteImportRequest {
  current: boolean
  locale: string | null
  sourceUrl: string | null
  version: string | null
}

export interface AdminPatchNoteImportResponse {
  createdChanges: number
  parserWarnings: string[]
  patchNoteCreated: boolean
  patchNoteId: number
  patchNoteSkipped: boolean
  patchNoteUpdated: boolean
  skippedChanges: number
  sourceUrl: string
  updatedChanges: number
  version: string
}

export interface AdminPatchChange {
  afterValue: string | null
  beforeValue: string | null
  category: AdminPatchChangeCategory
  id: number
  imageUrl: string | null
  impact: AdminPatchChangeImpact
  sortOrder: number
  summary: string
  tags: string[]
  targetKey: string | null
  targetName: string
  type: AdminPatchChangeType
}

export interface AdminPatchChangePayload {
  afterValue: string | null
  beforeValue: string | null
  category: AdminPatchChangeCategory
  imageUrl: string | null
  impact: AdminPatchChangeImpact
  patchNoteId: number
  sortOrder: number
  summary: string
  tags: string[]
  targetKey: string | null
  targetName: string
  type: AdminPatchChangeType
}

export async function fetchAdminPatchNotes(): Promise<AdminPatchNote[]> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<AdminPatchNote[]>>('/admin/patch-notes', {
      headers: adminAuthHeaders(),
    })
    return data.data
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to fetch admin patch notes.')
  }
}

export async function createAdminPatchNote(payload: AdminPatchNotePayload): Promise<AdminPatchNote> {
  try {
    const { data } = await axiosInstance.post<ApiResponse<AdminPatchNote>>('/admin/patch-notes', payload, {
      headers: adminAuthHeaders(),
    })
    return data.data
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to create admin patch note.')
  }
}

export async function updateAdminPatchNote(
  patchNoteId: number,
  payload: AdminPatchNotePayload,
): Promise<AdminPatchNote> {
  try {
    const { data } = await axiosInstance.patch<ApiResponse<AdminPatchNote>>(
      `/admin/patch-notes/${patchNoteId}`,
      payload,
      { headers: adminAuthHeaders() },
    )
    return data.data
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to update admin patch note.')
  }
}

export async function deleteAdminPatchNote(patchNoteId: number): Promise<void> {
  try {
    await axiosInstance.delete(`/admin/patch-notes/${patchNoteId}`, {
      headers: adminAuthHeaders(),
    })
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to delete admin patch note.')
  }
}

export async function importAdminPatchNoteFromRiot(
  payload: AdminPatchNoteImportRequest,
): Promise<AdminPatchNoteImportResponse> {
  try {
    const { data } = await axiosInstance.post<ApiResponse<AdminPatchNoteImportResponse>>(
      '/admin/patch-notes/import/riot',
      payload,
      {
        headers: adminAuthHeaders(),
        timeout: PATCH_NOTE_RIOT_IMPORT_TIMEOUT_MS,
      },
    )
    return data.data
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to import Riot patch note.')
  }
}

export async function fetchAdminPatchChanges(patchNoteId: number): Promise<AdminPatchChange[]> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<AdminPatchChange[]>>(
      `/admin/patch-notes/${patchNoteId}/changes`,
      { headers: adminAuthHeaders() },
    )
    return data.data
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to fetch admin patch changes.')
  }
}

export async function createAdminPatchChange(payload: AdminPatchChangePayload): Promise<AdminPatchChange> {
  try {
    const { data } = await axiosInstance.post<ApiResponse<AdminPatchChange>>('/admin/patch-note-changes', payload, {
      headers: adminAuthHeaders(),
    })
    return data.data
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to create admin patch change.')
  }
}

export async function updateAdminPatchChange(
  changeId: number,
  payload: AdminPatchChangePayload,
): Promise<AdminPatchChange> {
  try {
    const { data } = await axiosInstance.patch<ApiResponse<AdminPatchChange>>(
      `/admin/patch-note-changes/${changeId}`,
      payload,
      { headers: adminAuthHeaders() },
    )
    return data.data
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to update admin patch change.')
  }
}

export async function deleteAdminPatchChange(changeId: number): Promise<void> {
  try {
    await axiosInstance.delete(`/admin/patch-note-changes/${changeId}`, {
      headers: adminAuthHeaders(),
    })
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to delete admin patch change.')
  }
}

export async function importGuideCdragonData(
  payload: GuideCdragonImportRequest,
): Promise<GuideImportResponse> {
  const { data } = await axiosInstance.post<ApiResponse<GuideImportResponse>>(
    '/admin/guides/import/cdragon',
    payload,
    {
      headers: adminAuthHeaders(),
      timeout: GUIDE_CDRAGON_IMPORT_TIMEOUT_MS,
    },
  )
  return data.data
}

// ── 영웅증강 덱 ────────────────────────────────────────────────────────────

export interface HeroAugmentDeckItem {
  id: number
  name: string
  description: string | null
  champions: string | null
  traits: string | null
  boardPositions: string | null
  heroAugments: string | null
  recommended: boolean
  sortOrder: number
  grade: string | null
}

export interface HeroAugmentDeckPayload {
  name: string
  description: string | null
  champions: string | null
  traits: string | null
  boardPositions: string | null
  heroAugments: string | null
  recommended: boolean
  sortOrder: number
  grade: string | null
}

export async function fetchAdminHeroAugmentDecks(): Promise<HeroAugmentDeckItem[]> {
  const { data } = await axiosInstance.get('/admin/hero-augment-decks', {
    headers: adminAuthHeaders(),
  })
  return data.data
}

export async function createHeroAugmentDeck(payload: HeroAugmentDeckPayload): Promise<HeroAugmentDeckItem> {
  const { data } = await axiosInstance.post('/admin/hero-augment-decks', payload, {
    headers: adminAuthHeaders(),
  })
  return data.data
}

export async function updateHeroAugmentDeck(id: number, payload: HeroAugmentDeckPayload): Promise<HeroAugmentDeckItem> {
  const { data } = await axiosInstance.put(`/admin/hero-augment-decks/${id}`, payload, {
    headers: adminAuthHeaders(),
  })
  return data.data
}

export async function deleteHeroAugmentDeck(id: number): Promise<void> {
  await axiosInstance.delete(`/admin/hero-augment-decks/${id}`, {
    headers: adminAuthHeaders(),
  })
}

// ── 전적 캐시 & Rate Limit 모니터링 ──────────────────────────────────────────

export interface CacheStats {
  totalCount: number
  rankedCount: number
  normalCount: number
  newestMatchTimestamp: number | null
  oldestMatchTimestamp: number | null
  lastCachedAt: string | null
}

export interface RateLimitStats {
  shortRemaining: number
  shortMax: number
  shortWindowMs: number
  shortWindowRemainMs: number
  longRemaining: number
  longMax: number
  longWindowMs: number
  longWindowRemainMs: number
}

export async function fetchMatchCacheStats(): Promise<CacheStats> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<CacheStats>>('/admin/match/cache-stats', {
      headers: adminAuthHeaders(),
    })
    return data.data
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to fetch match cache stats.')
  }
}

export async function fetchRateLimitStats(): Promise<RateLimitStats> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<RateLimitStats>>('/admin/match/rate-limit', {
      headers: adminAuthHeaders(),
    })
    return data.data
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to fetch rate limit stats.')
  }
}
