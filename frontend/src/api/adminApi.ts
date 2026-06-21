import axiosInstance from './axiosInstance'
import type { RankFilter } from '../pages/Dashboard/dashboardData'

const ADMIN_TOKEN_KEY = 'tftgogo_admin_token'
const GUIDE_CDRAGON_IMPORT_TIMEOUT_MS = 120_000

export function getAdminToken(): string {
  return localStorage.getItem(ADMIN_TOKEN_KEY) ?? ''
}

export function setAdminToken(token: string): void {
  localStorage.setItem(ADMIN_TOKEN_KEY, token)
}

export function clearAdminToken(): void {
  localStorage.removeItem(ADMIN_TOKEN_KEY)
}

function adminHeaders() {
  return { 'X-Admin-Token': getAdminToken() }
}

function getHttpStatus(error: unknown): number | undefined {
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

export async function validateAdminToken(): Promise<void> {
  await axiosInstance.get('/admin/guides', {
    headers: adminHeaders(),
  })
}

export interface UnitInfo {
  characterId: string
  name: string
  imageUrl: string
}

export interface HeroAugmentEntry {
  championId: string    // e.g. "tft17_jinx"
  championName: string  // e.g. "징크스"
  augmentName: string   // e.g. "화약 소녀"
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
  heroAugments: string | null   // JSON string
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
  heroAugments: string | null   // JSON string
}

export async function fetchAdminDecks(rankFilter: RankFilter = 'MASTER_PLUS'): Promise<AdminDeck[]> {
  const { data } = await axiosInstance.get(`/admin/decks?rankFilter=${rankFilter}`, {
    headers: adminHeaders(),
  })
  return data.data
}

export async function updateDeckCuration(deckId: number, req: DeckCurationRequest): Promise<AdminDeck> {
  const { data } = await axiosInstance.patch(`/admin/decks/${deckId}`, req, {
    headers: adminHeaders(),
  })
  return data.data
}

export async function resetDeckCuration(deckId: number): Promise<void> {
  await axiosInstance.delete(`/admin/decks/${deckId}/curation`, {
    headers: adminHeaders(),
  })
}

// ── 게임가이드 import ─────────────────────────────────────────────────────

export interface GuideCdragonImportRequest {
  includeAugments: boolean
  includeChampions: boolean
  includeItems: boolean
  includeTraits: boolean
  mutator: string | null
  patchVersion: string
  setNumber: number
}

export interface GuideImportResponse {
  augmentCount: number
  championCount: number
  createdCount: number
  importedCount: number
  itemCount: number
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
  targetKey: string
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
  targetKey: string
  targetName: string
  type: AdminPatchChangeType
}

export async function fetchAdminPatchNotes(): Promise<AdminPatchNote[]> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<AdminPatchNote[]>>('/admin/patch-notes', {
      headers: adminHeaders(),
    })
    return data.data
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to fetch admin patch notes.')
  }
}

export async function createAdminPatchNote(payload: AdminPatchNotePayload): Promise<AdminPatchNote> {
  try {
    const { data } = await axiosInstance.post<ApiResponse<AdminPatchNote>>('/admin/patch-notes', payload, {
      headers: adminHeaders(),
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
      {
        headers: adminHeaders(),
      },
    )
    return data.data
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to update admin patch note.')
  }
}

export async function deleteAdminPatchNote(patchNoteId: number): Promise<void> {
  try {
    await axiosInstance.delete(`/admin/patch-notes/${patchNoteId}`, {
      headers: adminHeaders(),
    })
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to delete admin patch note.')
  }
}

export async function fetchAdminPatchChanges(patchNoteId: number): Promise<AdminPatchChange[]> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<AdminPatchChange[]>>(
      `/admin/patch-notes/${patchNoteId}/changes`,
      {
        headers: adminHeaders(),
      },
    )
    return data.data
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to fetch admin patch changes.')
  }
}

export async function createAdminPatchChange(payload: AdminPatchChangePayload): Promise<AdminPatchChange> {
  try {
    const { data } = await axiosInstance.post<ApiResponse<AdminPatchChange>>('/admin/patch-note-changes', payload, {
      headers: adminHeaders(),
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
      {
        headers: adminHeaders(),
      },
    )
    return data.data
  } catch (error) {
    throw createAdminRequestError(error, 'Failed to update admin patch change.')
  }
}

export async function deleteAdminPatchChange(changeId: number): Promise<void> {
  try {
    await axiosInstance.delete(`/admin/patch-note-changes/${changeId}`, {
      headers: adminHeaders(),
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
      headers: adminHeaders(),
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
    headers: adminHeaders(),
  })
  return data.data
}

export async function createHeroAugmentDeck(payload: HeroAugmentDeckPayload): Promise<HeroAugmentDeckItem> {
  const { data } = await axiosInstance.post('/admin/hero-augment-decks', payload, {
    headers: adminHeaders(),
  })
  return data.data
}

export async function updateHeroAugmentDeck(id: number, payload: HeroAugmentDeckPayload): Promise<HeroAugmentDeckItem> {
  const { data } = await axiosInstance.put(`/admin/hero-augment-decks/${id}`, payload, {
    headers: adminHeaders(),
  })
  return data.data
}

export async function deleteHeroAugmentDeck(id: number): Promise<void> {
  await axiosInstance.delete(`/admin/hero-augment-decks/${id}`, {
    headers: adminHeaders(),
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
  const { data } = await axiosInstance.get<ApiResponse<CacheStats>>('/admin/match/cache-stats', {
    headers: adminHeaders(),
  })
  return data.data
}

export async function fetchRateLimitStats(): Promise<RateLimitStats> {
  const { data } = await axiosInstance.get<ApiResponse<RateLimitStats>>('/admin/match/rate-limit', {
    headers: adminHeaders(),
  })
  return data.data
}
