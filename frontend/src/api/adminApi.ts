import axiosInstance from './axiosInstance'
import type { RankFilter } from '../pages/Dashboard/dashboardData'

const ADMIN_TOKEN_KEY = 'tftgogo_admin_token'

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
