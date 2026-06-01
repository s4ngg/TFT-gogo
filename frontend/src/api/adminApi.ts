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
  grade: string
  winRate: string
  top4: string
  pickRate: string
  sampleSize: number
  units: UnitInfo[]
}

export interface BoardPosition {
  row: number
  col: number
}

export interface DeckCurationRequest {
  customName: string | null
  hidden: boolean
  sortPriority: number | null
  curatorNote: string | null
  boardPositions: string | null
}

export async function fetchAdminDecks(rankFilter: RankFilter = 'MASTER_PLUS'): Promise<AdminDeck[]> {
  const { data } = await axiosInstance.get(`/api/admin/decks?rankFilter=${rankFilter}`, {
    headers: adminHeaders(),
  })
  return data.data
}

export async function updateDeckCuration(deckId: number, req: DeckCurationRequest): Promise<AdminDeck> {
  const { data } = await axiosInstance.patch(`/api/admin/decks/${deckId}`, req, {
    headers: adminHeaders(),
  })
  return data.data
}

export async function resetDeckCuration(deckId: number): Promise<void> {
  await axiosInstance.delete(`/api/admin/decks/${deckId}/curation`, {
    headers: adminHeaders(),
  })
}
