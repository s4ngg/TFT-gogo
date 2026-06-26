import axiosInstance from './axiosInstance'
import { isRecord, unwrapApiResponse, type ApiResponse } from './apiResponse'
import { readPatchChangeStatsPayload } from './patchNoteStatsPayload'

export const CHANGE_CATEGORIES = ['챔피언', '시너지', '아이템', '증강체', '시스템'] as const
export const PATCH_CATEGORIES = ['전체', ...CHANGE_CATEGORIES] as const
export const CHANGE_TYPE_FILTERS = ['전체', '상향', '하향', '조정', '신규'] as const

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
  importedAt?: string
  importSource?: string
  summary?: string
  status: '현재' | '이전'
  sourceUrl?: string
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

export interface PatchChangeStats {
  buffCount: number
  categoryCounts: Record<PatchCategory, number>
  highImpactCount: number
  nerfCount: number
  totalChanges: number
  typeCounts: Record<ChangeType, number>
}

export interface PatchChangePage {
  items: PatchChange[]
  page: number
  pageSize: number
  stats: PatchChangeStats
  totalItems: number
  totalPages: number
}

export interface PatchChangesResult {
  data: PatchChangePage
  source: PatchNotesSource
}

export interface PatchChangesQuery {
  category: PatchCategory
  changeType: ChangeTypeFilter
  highImpactOnly: boolean
  page: number
  pageSize: number
  query: string
  version: string
}

const PATCH_NOTE_DEFAULT_IMAGE = '/assets/emblems/patch-meta-emblem-pink.png'
const GENERATED_CATEGORY_TAGS = new Set(['champion', 'trait', 'item', 'augment', 'system'])

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
  importedAt?: string | null
  importSource?: string | null
  isCurrent?: boolean | null
  patchNoteChanges?: PatchChangeResponse[] | null
  patchNoteHighlights?: PatchHighlightResponse[] | null
  publishedAt?: string | null
  representativeImageUrl?: string | null
  sourceUrl?: string | null
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

function readNumber(value: unknown): number | undefined {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : undefined
  }
  return undefined
}

function readRecordNumber(record: Record<string, unknown>, key: string) {
  return readNumber(record[key])
}

function readNonEmptyString(value: unknown): string | undefined {
  if (typeof value !== 'string') return undefined

  const trimmed = value.trim()
  return trimmed.length > 0 ? trimmed : undefined
}

export function sanitizePatchHighlight(value: string) {
  return value.replace(/^\s*\(\s*\d+\s*\)\s*/u, '').trim()
}

function readTags(value: unknown) {
  const normalizeTags = (tags: string[]) => tags
    .map((tag) => tag.trim())
    .filter((tag) => tag.length > 0 && !GENERATED_CATEGORY_TAGS.has(tag.toLowerCase()))

  if (Array.isArray(value)) {
    return normalizeTags(value.filter((item): item is string => typeof item === 'string'))
  }

  if (typeof value !== 'string') return []

  try {
    const parsed: unknown = JSON.parse(value)
    return Array.isArray(parsed)
      ? normalizeTags(parsed.filter((item): item is string => typeof item === 'string'))
      : []
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

function getBackendCategory(category: PatchCategory) {
  const categoryMap: Record<ChangeCategory, BackendChangeCategory> = {
    아이템: 'ITEM',
    시너지: 'TRAIT',
    시스템: 'SYSTEM',
    증강체: 'AUGMENT',
    챔피언: 'CHAMPION',
  }

  return category === '전체' ? undefined : categoryMap[category]
}

function getBackendChangeType(changeType: ChangeTypeFilter) {
  const changeTypeMap: Record<ChangeType, BackendChangeType> = {
    신규: 'NEW',
    상향: 'BUFF',
    조정: 'ADJUST',
    하향: 'NERF',
  }

  return changeType === '전체' ? undefined : changeTypeMap[changeType]
}

function normalizePatchHeadingLabel(value: string) {
  const normalizedValue = value.replace(/\s+/g, ' ').trim()
  if (!normalizedValue) return ''

  const segments = normalizedValue
    .split('>')
    .map((segment) => segment.trim())
    .filter(Boolean)
  const label = segments[segments.length - 1] ?? normalizedValue

  const normalizedLabel = label
    .replace(/^\d{1,2}월\s+\d{1,2}일\s*/u, '')
    .replace(/^\d{1,2}(?:[.-]\d{1,2}[a-zA-Z]?)?\s*(?:추가\s*)?패치(?:\s*노트)?\s*/u, '')
    .trim()

  return sanitizePatchHighlight(normalizedLabel || label)
}

function uniqueNonEmpty(values: string[]) {
  return Array.from(new Set(values.map((value) => value.trim()).filter(Boolean)))
}

function normalizeHighlights(note: PatchNoteResponse) {
  const rawHighlights = Array.isArray(note.highlights)
    ? note.highlights.map((highlight) => (typeof highlight === 'string' ? highlight : readString(highlight.content)))
    : Array.isArray(note.patchNoteHighlights)
      ? note.patchNoteHighlights.map((highlight) => readString(highlight.content))
      : []

  return uniqueNonEmpty(rawHighlights.map(normalizePatchHeadingLabel))
}

function normalizePatchFocus(focus: unknown, summary: string, description: string) {
  const focusText = readString(focus).trim()
  if (!focusText) return summary || description
  if (focusText.includes('>')) return summary || description || normalizePatchHeadingLabel(focusText)

  return focusText
}

function normalizeChange(change: PatchChangeResponse, index: number): PatchChange {
  return {
    after: readString(change.after ?? change.afterValue),
    before: readString(change.before ?? change.beforeValue),
    category: normalizeCategory(change.category),
    id: typeof change.id === 'number' ? change.id : index + 1,
    imageUrl: readNonEmptyString(change.imageUrl),
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
  const description = readString(note.description ?? note.content ?? summary)
  const imageUrl = readNonEmptyString(note.imageUrl)
    ?? readNonEmptyString(note.representativeImageUrl)
    ?? PATCH_NOTE_DEFAULT_IMAGE

  return {
    changes: changes.map(normalizeChange),
    date: formatDateLabel(publishedDate),
    description,
    focus: normalizePatchFocus(note.focus, summary, description),
    highlights: normalizeHighlights(note),
    imageUrl,
    importedAt: readNonEmptyString(note.importedAt),
    importSource: readNonEmptyString(note.importSource),
    summary,
    status: note.status === '현재' || note.status === 'CURRENT' || note.isCurrent ? '현재' : '이전',
    sourceUrl: readNonEmptyString(note.sourceUrl),
    title: readString(note.title, `${readString(note.version)} 패치`),
    version: readString(note.version),
  }
}

function getEmptyPatchChangeStats(): PatchChangeStats {
  return {
    buffCount: 0,
    categoryCounts: {
      전체: 0,
      아이템: 0,
      시너지: 0,
      시스템: 0,
      증강체: 0,
      챔피언: 0,
    },
    highImpactCount: 0,
    nerfCount: 0,
    totalChanges: 0,
    typeCounts: {
      신규: 0,
      상향: 0,
      조정: 0,
      하향: 0,
    },
  }
}

function countPatchChangeStats(changes: PatchChange[]): PatchChangeStats {
  const stats = getEmptyPatchChangeStats()
  stats.totalChanges = changes.length
  stats.categoryCounts.전체 = changes.length

  changes.forEach((change) => {
    stats.categoryCounts[change.category] += 1
    stats.typeCounts[change.type] += 1
    if (change.impact === '높음') stats.highImpactCount += 1
  })

  stats.buffCount = stats.typeCounts.상향
  stats.nerfCount = stats.typeCounts.하향

  return stats
}

function readPatchChangeItems(payload: unknown): unknown[] | undefined {
  if (Array.isArray(payload)) return payload
  if (!isRecord(payload)) return undefined

  const itemKeys = ['items', 'content', 'changes', 'patchNoteChanges', 'data', 'results']
  const itemValue = itemKeys.map((key) => payload[key]).find(Array.isArray)

  return itemValue
}

function readPageNumber(payload: unknown, fallbackPage: number) {
  if (!isRecord(payload)) return fallbackPage

  const page = readRecordNumber(payload, 'page') ?? readRecordNumber(payload, 'currentPage')
  if (page !== undefined) return Math.max(1, page)

  const zeroBasedPage = readRecordNumber(payload, 'number')
  return zeroBasedPage !== undefined ? Math.max(1, zeroBasedPage + 1) : fallbackPage
}

function readStatsCount(payload: unknown, keys: string[]) {
  if (!isRecord(payload)) return undefined
  return keys.map((key) => readRecordNumber(payload, key)).find((value) => value !== undefined)
}

function normalizeCategoryCounts(value: unknown, fallback: PatchChangeStats['categoryCounts']) {
  const counts = { ...fallback }
  if (!isRecord(value)) return counts

  const keyMap: Record<string, PatchCategory> = {
    ALL: '전체',
    AUGMENT: '증강체',
    CHAMPION: '챔피언',
    ITEM: '아이템',
    SYSTEM: '시스템',
    TRAIT: '시너지',
    전체: '전체',
    아이템: '아이템',
    시너지: '시너지',
    시스템: '시스템',
    증강체: '증강체',
    챔피언: '챔피언',
  }

  Object.entries(value).forEach(([key, count]) => {
    const category = keyMap[key]
    const normalizedCount = readNumber(count)
    if (category && normalizedCount !== undefined) counts[category] = normalizedCount
  })

  return counts
}

function normalizeTypeCounts(value: unknown, fallback: PatchChangeStats['typeCounts']) {
  const counts = { ...fallback }
  if (!isRecord(value)) return counts

  const keyMap: Record<string, ChangeType> = {
    ADJUST: '조정',
    BUFF: '상향',
    NERF: '하향',
    NEW: '신규',
    신규: '신규',
    상향: '상향',
    조정: '조정',
    하향: '하향',
  }

  Object.entries(value).forEach(([key, count]) => {
    const changeType = keyMap[key]
    const normalizedCount = readNumber(count)
    if (changeType && normalizedCount !== undefined) counts[changeType] = normalizedCount
  })

  return counts
}

function normalizePatchChangeStats(payload: unknown, changes: PatchChange[], totalItems: number): PatchChangeStats {
  const fallback = countPatchChangeStats(changes)
  if (!isRecord(payload)) return fallback

  const categoryCounts = normalizeCategoryCounts(payload.categoryCounts, fallback.categoryCounts)
  const typeCounts = normalizeTypeCounts(payload.typeCounts, fallback.typeCounts)
  const highImpactCount = readStatsCount(payload, ['highImpactCount', 'highImpactChanges']) ?? fallback.highImpactCount
  const totalChanges = readStatsCount(payload, ['totalChanges', 'totalCount'])
    ?? categoryCounts.전체
    ?? totalItems

  return {
    buffCount: readStatsCount(payload, ['buffCount']) ?? typeCounts.상향,
    categoryCounts: {
      ...categoryCounts,
      전체: totalChanges,
    },
    highImpactCount,
    nerfCount: readStatsCount(payload, ['nerfCount']) ?? typeCounts.하향,
    totalChanges,
    typeCounts,
  }
}

function normalizePatchChangePage(payload: unknown, params: PatchChangesQuery): PatchChangePage | undefined {
  const rawChanges = readPatchChangeItems(payload)
  if (!rawChanges) return undefined

  const changes = rawChanges.filter(isRecord).map((change, index) => normalizeChange(change, index))
  const page = readPageNumber(payload, params.page)
  const pageSize = isRecord(payload)
    ? readRecordNumber(payload, 'pageSize') ?? readRecordNumber(payload, 'size') ?? params.pageSize
    : params.pageSize
  const totalItems = isRecord(payload)
    ? readRecordNumber(payload, 'totalItems') ?? readRecordNumber(payload, 'totalElements') ?? readRecordNumber(payload, 'total') ?? changes.length
    : changes.length
  const totalPages = isRecord(payload)
    ? readRecordNumber(payload, 'totalPages') ?? Math.max(1, Math.ceil(totalItems / pageSize))
    : Math.max(1, Math.ceil(totalItems / pageSize))

  return {
    items: changes,
    page,
    pageSize,
    stats: normalizePatchChangeStats(readPatchChangeStatsPayload(payload), changes, totalItems),
    totalItems,
    totalPages,
  }
}

export function getFallbackPatchChangePage(
  params: PatchChangesQuery,
  fallbackData: PatchNoteDetail[],
): PatchChangePage {
  const fallbackPatch = fallbackData.find((patch) => patch.version === params.version) ?? fallbackData[0]
  const patchChanges = fallbackPatch?.changes ?? []
  const normalizedQuery = params.query.trim().toLowerCase()
  const stats = countPatchChangeStats(patchChanges)
  const filteredChanges = patchChanges.filter((change) => {
    const matchesCategory = params.category === '전체' || change.category === params.category
    const matchesType = params.changeType === '전체' || change.type === params.changeType
    const matchesImpact = !params.highImpactOnly || change.impact === '높음'
    const searchableText = [change.target, change.summary, change.category, change.type, ...change.tags].join(' ').toLowerCase()
    const matchesQuery = !normalizedQuery || searchableText.includes(normalizedQuery)

    return matchesCategory && matchesType && matchesImpact && matchesQuery
  })
  const startIndex = (params.page - 1) * params.pageSize

  return {
    items: filteredChanges.slice(startIndex, startIndex + params.pageSize),
    page: params.page,
    pageSize: params.pageSize,
    stats,
    totalItems: filteredChanges.length,
    totalPages: Math.max(1, Math.ceil(filteredChanges.length / params.pageSize)),
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

export async function getPatchChanges(
  params: PatchChangesQuery,
  fallbackData: PatchNoteDetail[],
): Promise<PatchChangesResult> {
  if (!params.version) {
    return { data: getFallbackPatchChangePage(params, fallbackData), source: 'fallback' }
  }

  try {
    const { data } = await axiosInstance.get<ApiResponse<unknown> | unknown>(
      `/patch-notes/${encodeURIComponent(params.version)}/changes`,
      {
        params: {
          category: getBackendCategory(params.category),
          impact: params.highImpactOnly ? 'HIGH' : undefined,
          page: params.page,
          pageSize: params.pageSize,
          query: params.query || undefined,
          type: getBackendChangeType(params.changeType),
        },
      },
    )
    const payload = unwrapApiResponse(data)
    const page = normalizePatchChangePage(payload, params)

    if (!page) {
      return { data: getFallbackPatchChangePage(params, fallbackData), source: 'fallback' }
    }

    return { data: page, source: 'api' }
  } catch {
    return { data: getFallbackPatchChangePage(params, fallbackData), source: 'fallback' }
  }
}
