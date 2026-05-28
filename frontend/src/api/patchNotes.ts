import axiosInstance from './axiosInstance'
import { isRecord, unwrapApiResponse, type ApiResponse } from './apiResponse'

export const CHANGE_CATEGORIES = ['챔피언', '시너지', '아이템', '증강체', '시스템'] as const
export const PATCH_CATEGORIES = ['전체', ...CHANGE_CATEGORIES] as const
export const CHANGE_TYPE_FILTERS = ['전체 변경', '상향', '하향', '조정', '신규'] as const

export type ChangeCategory = (typeof CHANGE_CATEGORIES)[number]
export type PatchCategory = (typeof PATCH_CATEGORIES)[number]
export type ChangeType = '상향' | '하향' | '조정' | '신규'
export type ChangeTypeFilter = (typeof CHANGE_TYPE_FILTERS)[number]
export type ImpactLevel = '높음' | '중간' | '낮음'

export interface PatchChange {
  after: string
  before: string
  category: ChangeCategory
  id: number
  imageUrl?: string
  impact: ImpactLevel
  summary: string
  tags: string[]
  target: string
  type: ChangeType
}

export interface PatchNoteSummary {
  date: string
  description: string
  focus: string
  highlights: string[]
  imageUrl: string
  status: '현재' | '이전'
  title: string
  version: string
}

export interface PatchNoteDetail extends PatchNoteSummary {
  changes: PatchChange[]
}

export type PatchNotesSource = 'api' | 'fallback'

export interface PatchNotesResult {
  data: PatchNoteDetail[]
  source: PatchNotesSource
}

type BackendChangeCategory = 'CHAMPION' | 'TRAIT' | 'ITEM' | 'AUGMENT' | 'SYSTEM'
type BackendChangeType = 'BUFF' | 'NERF' | 'ADJUST' | 'NEW'
type BackendImpactLevel = 'HIGH' | 'MEDIUM' | 'LOW'

interface PatchChangeResponse {
  after?: string | null
  afterValue?: string | null
  before?: string | null
  beforeValue?: string | null
  category?: BackendChangeCategory | ChangeCategory | null
  changeType?: BackendChangeType | ChangeType | null
  id?: number | null
  imageUrl?: string | null
  impact?: BackendImpactLevel | ImpactLevel | null
  summary?: string | null
  tags?: string[] | null
  tagsJson?: string[] | string | null
  target?: string | null
  targetName?: string | null
  type?: BackendChangeType | ChangeType | null
}

interface PatchHighlightResponse {
  content?: string | null
}

interface PatchNoteResponse {
  changes?: PatchChangeResponse[] | null
  content?: string | null
  date?: string | null
  description?: string | null
  focus?: string | null
  highlights?: Array<string | PatchHighlightResponse> | null
  id?: number | null
  imageUrl?: string | null
  isCurrent?: boolean | null
  patchNoteChanges?: PatchChangeResponse[] | null
  patchNoteHighlights?: PatchHighlightResponse[] | null
  publishedAt?: string | null
  representativeImageUrl?: string | null
  status?: 'CURRENT' | 'PREVIOUS' | '현재' | '이전' | null
  summary?: string | null
  title?: string | null
  version?: string | null
}

type PatchNotesPayload = PatchNoteResponse[] | {
  content?: PatchNoteResponse[]
  items?: PatchNoteResponse[]
  patchNotes?: PatchNoteResponse[]
}

function readString(value: unknown, fallback = '') {
  return typeof value === 'string' ? value : fallback
}

function readTags(value: unknown) {
  if (Array.isArray(value)) {
    return value.filter((item): item is string => typeof item === 'string')
  }

  if (typeof value !== 'string') return []

  try {
    const parsed: unknown = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : []
  } catch {
    return []
  }
}

function formatDateLabel(value: string, fallback = '') {
  if (!value) return fallback

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}.${month}.${day}`
}

function normalizeCategory(value: unknown): ChangeCategory {
  if (value === '챔피언' || value === '시너지' || value === '아이템' || value === '증강체' || value === '시스템') {
    return value
  }

  const categoryMap: Record<BackendChangeCategory, ChangeCategory> = {
    AUGMENT: '증강체',
    CHAMPION: '챔피언',
    ITEM: '아이템',
    SYSTEM: '시스템',
    TRAIT: '시너지',
  }

  return typeof value === 'string' && value in categoryMap
    ? categoryMap[value as BackendChangeCategory]
    : '시스템'
}

function normalizeChangeType(value: unknown): ChangeType {
  if (value === '상향' || value === '하향' || value === '조정' || value === '신규') return value

  const changeTypeMap: Record<BackendChangeType, ChangeType> = {
    ADJUST: '조정',
    BUFF: '상향',
    NERF: '하향',
    NEW: '신규',
  }

  return typeof value === 'string' && value in changeTypeMap
    ? changeTypeMap[value as BackendChangeType]
    : '조정'
}

function normalizeImpact(value: unknown): ImpactLevel {
  if (value === '높음' || value === '중간' || value === '낮음') return value

  const impactMap: Record<BackendImpactLevel, ImpactLevel> = {
    HIGH: '높음',
    LOW: '낮음',
    MEDIUM: '중간',
  }

  return typeof value === 'string' && value in impactMap
    ? impactMap[value as BackendImpactLevel]
    : '중간'
}

function normalizeHighlights(note: PatchNoteResponse) {
  if (Array.isArray(note.highlights)) {
    return note.highlights
      .map((highlight) => (typeof highlight === 'string' ? highlight : readString(highlight.content)))
      .filter(Boolean)
  }

  if (Array.isArray(note.patchNoteHighlights)) {
    return note.patchNoteHighlights.map((highlight) => readString(highlight.content)).filter(Boolean)
  }

  return []
}

function normalizeChange(change: PatchChangeResponse, index: number): PatchChange {
  return {
    after: readString(change.after ?? change.afterValue),
    before: readString(change.before ?? change.beforeValue),
    category: normalizeCategory(change.category),
    id: typeof change.id === 'number' ? change.id : index + 1,
    imageUrl: change.imageUrl ?? undefined,
    impact: normalizeImpact(change.impact),
    summary: readString(change.summary),
    tags: readTags(change.tags ?? change.tagsJson),
    target: readString(change.target ?? change.targetName),
    type: normalizeChangeType(change.type ?? change.changeType),
  }
}

function normalizePatchNote(note: PatchNoteResponse): PatchNoteDetail {
  const changes = note.changes ?? note.patchNoteChanges ?? []
  const publishedDate = readString(note.date ?? note.publishedAt)
  const summary = readString(note.summary)

  return {
    changes: changes.map(normalizeChange),
    date: formatDateLabel(publishedDate),
    description: readString(note.description ?? note.content ?? summary),
    focus: readString(note.focus ?? summary),
    highlights: normalizeHighlights(note),
    imageUrl: readString(note.imageUrl ?? note.representativeImageUrl),
    status: note.status === '현재' || note.status === 'CURRENT' || note.isCurrent ? '현재' : '이전',
    title: readString(note.title, `${readString(note.version)} 패치`),
    version: readString(note.version),
  }
}

function extractPatchNotes(payload: PatchNotesPayload): PatchNoteResponse[] {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload.patchNotes)) return payload.patchNotes
  if (Array.isArray(payload.items)) return payload.items
  if (Array.isArray(payload.content)) return payload.content
  return []
}

function normalizePatchNotes(payload: PatchNotesPayload, fallbackData: PatchNoteDetail[]): PatchNotesResult {
  const patchNotes = extractPatchNotes(payload)
  if (patchNotes.length === 0) {
    return { data: fallbackData, source: 'fallback' }
  }

  return { data: patchNotes.map(normalizePatchNote), source: 'api' }
}

export async function getPatchNotes(fallbackData: PatchNoteDetail[]): Promise<PatchNotesResult> {
  try {
    const { data } = await axiosInstance.get<ApiResponse<PatchNotesPayload> | PatchNotesPayload>('/patch-notes')
    const payload = unwrapApiResponse(data)

    if (!Array.isArray(payload) && !isRecord(payload)) {
      return { data: fallbackData, source: 'fallback' }
    }

    return normalizePatchNotes(payload, fallbackData)
  } catch {
    return { data: fallbackData, source: 'fallback' }
  }
}
