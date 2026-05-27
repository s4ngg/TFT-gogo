import axiosInstance from './axiosInstance'
import { communityDragonAssetUrl } from './communityDragonAssets'

export const CHANGE_CATEGORIES = ['챔피언', '시너지', '아이템', '증강체', '시스템'] as const
export const PATCH_CATEGORIES = ['전체', ...CHANGE_CATEGORIES] as const
export const CHANGE_TYPE_FILTERS = ['전체 변경', '상향', '하향', '조정', '신규'] as const
export const PATCH_PAGE_SIZE = 5
export const PATCH_SAMPLE_PAGE_COUNT = 7
export const PATCH_FALLBACK_IMAGE = '/assets/emblems/patch-meta-emblem-pink.png'

export type ChangeCategory = (typeof CHANGE_CATEGORIES)[number]
export type PatchCategory = (typeof PATCH_CATEGORIES)[number]
export type ChangeType = '상향' | '하향' | '조정' | '신규'
export type ChangeTypeFilter = (typeof CHANGE_TYPE_FILTERS)[number]
export type ImpactLevel = '높음' | '중간' | '낮음'

export interface PatchChange {
  id: number
  category: ChangeCategory
  target: string
  type: ChangeType
  impact: ImpactLevel
  imageUrl?: string
  summary: string
  before: string
  after: string
  tags: string[]
}

export interface PatchHistoryItem {
  version: string
  date: string
  title: string
  status: '현재' | '이전'
  focus: string
  description: string
  highlights: string[]
  imageUrl: string
}

export interface PatchChangesQuery {
  patchVersion: string
  category: PatchCategory
  changeType: ChangeTypeFilter
  highImpactOnly: boolean
  query: string
  page: number
  pageSize?: number
}

export interface PatchNotesFallbackData {
  baseChanges: PatchChange[]
  history: PatchHistoryItem[]
}

export interface PatchChangesResponse {
  allChanges: PatchChange[]
  categoryCounts: Record<PatchCategory, number>
  highImpactCount: number
  buffCount: number
  nerfCount: number
  items: PatchChange[]
  totalItems: number
  totalPages: number
}

export const PATCH_CATEGORY_IMAGE_URLS: Record<ChangeCategory, string> = {
  챔피언: communityDragonAssetUrl('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  시너지: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Challenger.TFT_Set17.tex'),
  아이템: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GuinsoosRageblade.TFT_Set13.tex'),
  증강체: communityDragonAssetUrl('ASSETS/UX/TFT/Augments/Augment_Silver.tex'),
  시스템: PATCH_FALLBACK_IMAGE,
}

export const PATCH_TARGET_IMAGE_URLS: Record<string, string> = {
  아펠리오스: communityDragonAssetUrl('ASSETS/Characters/TFT17_Aphelios/Skins/Base/Images/TFT17_Aphelios_splash_tile_1.TFT_Set17.tex'),
  세주아니: communityDragonAssetUrl('ASSETS/Characters/TFT17_Sejuani/Skins/Base/Images/TFT17_Sejuani_splash_tile_1.TFT_Set17.tex'),
  럭스: communityDragonAssetUrl('ASSETS/Characters/TFT17_Lux/Skins/Base/Images/TFT17_Lux_splash_tile_1.TFT_Set17.tex'),
  카이사: communityDragonAssetUrl('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  오른: communityDragonAssetUrl('ASSETS/Characters/TFT17_Ornn/Skins/Base/Images/TFT17_Ornn_splash_tile_11.TFT_Set17.tex'),
  학살자: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Rogue.TFT_Set17.tex'),
  마법사: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Fateweaver.TFT_Set17.tex'),
  감시자: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_12_Vanguard.TFT_Set12.tex'),
  전략가: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Stargazer.TFT_Set17.tex'),
  결투가: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Challenger.TFT_Set17.tex'),
  '구인수의 격노검': communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GuinsoosRageblade.TFT_Set13.tex'),
  '워모그의 갑옷': communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_WarmogsArmor.TFT_Set13.tex'),
  '쇼진의 창': communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_SpearOfShojin.TFT_Set13.tex'),
  '이온 충격기': communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_IonicSpark.TFT_Set13.tex'),
  '거인 학살자': communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GiantSlayer.TFT_Set13.tex'),
}

export function getPatchBaseTarget(target: string) {
  return target.replace(/\s샘플\s\d+$/, '')
}

export function getPatchChangeImageUrl(change: PatchChange) {
  return change.imageUrl
    ?? PATCH_TARGET_IMAGE_URLS[getPatchBaseTarget(change.target)]
    ?? PATCH_CATEGORY_IMAGE_URLS[change.category]
    ?? PATCH_FALLBACK_IMAGE
}

export function getPatchChangeFallbackImageUrl(change: PatchChange) {
  return PATCH_CATEGORY_IMAGE_URLS[change.category] ?? PATCH_FALLBACK_IMAGE
}

export function getPatchImageAlt(target: string, label: string) {
  return `${target} ${label} 이미지`
}

function buildVersionedChange(change: PatchChange, version: string, patchIndex: number): PatchChange {
  if (patchIndex === 0) {
    return change
  }

  return {
    ...change,
    id: patchIndex * 1000 + change.id,
    summary: `${version} 패치 기준 ${change.summary}`,
    before: `${change.before} · ${version} 이전 기준`,
    after: `${change.after} · ${version} 적용 기준`,
    tags: [version, ...change.tags.slice(0, 2)],
  }
}

function expandPatchSamples(
  changes: PatchChange[],
  patch: PatchHistoryItem,
  history: PatchHistoryItem[],
  pageSize: number,
) {
  const targetCount = pageSize * PATCH_SAMPLE_PAGE_COUNT
  const patchIndex = Math.max(0, history.findIndex((historyItem) => historyItem.version === patch.version))

  return CHANGE_CATEGORIES.flatMap((category) => {
    const categoryChanges = changes.filter((change) => change.category === category)

    if (categoryChanges.length === 0) {
      return []
    }

    return Array.from({ length: targetCount }, (_, index) => {
      const source = buildVersionedChange(categoryChanges[index % categoryChanges.length], patch.version, patchIndex)
      const sampleRound = Math.floor(index / categoryChanges.length) + 1
      const isOriginal = sampleRound === 1
      const id = patchIndex * 1000 + CHANGE_CATEGORIES.indexOf(category) * targetCount + index + 1

      return {
        ...source,
        id,
        target: isOriginal ? source.target : `${source.target} 샘플 ${sampleRound}`,
        summary: isOriginal ? source.summary : `${source.summary} ${sampleRound}차 샘플 기준으로 표시했습니다.`,
        before: isOriginal ? source.before : `${source.before} · 샘플 ${sampleRound}`,
        after: isOriginal ? source.after : `${source.after} · 샘플 ${sampleRound}`,
        tags: isOriginal ? source.tags : [...source.tags.slice(0, 2), `샘플 ${sampleRound}`],
      }
    })
  })
}

function getCategoryCount(category: PatchCategory, changes: PatchChange[]) {
  if (category === '전체') return changes.length
  return changes.filter((change) => change.category === category).length
}

function getTotalPages(totalItems: number, pageSize: number) {
  return Math.max(1, Math.ceil(totalItems / pageSize))
}

function getPageItems<T>(items: T[], page: number, pageSize: number) {
  const startIndex = (page - 1) * pageSize
  return items.slice(startIndex, startIndex + pageSize)
}

export function buildPatchChangesResponse(
  params: PatchChangesQuery,
  fallbackData: PatchNotesFallbackData,
): PatchChangesResponse {
  const pageSize = params.pageSize ?? PATCH_PAGE_SIZE
  const selectedPatch = fallbackData.history.find((patch) => patch.version === params.patchVersion) ?? fallbackData.history[0]
  const allChanges = selectedPatch
    ? expandPatchSamples(fallbackData.baseChanges, selectedPatch, fallbackData.history, pageSize)
    : []
  const normalizedQuery = params.query.trim().toLowerCase()

  const filteredChanges = allChanges.filter((change) => {
    const matchesCategory = params.category === '전체' || change.category === params.category
    const matchesType = params.changeType === '전체 변경' || change.type === params.changeType
    const matchesImpact = !params.highImpactOnly || change.impact === '높음'
    const searchableText = [change.target, change.summary, change.category, change.type, ...change.tags].join(' ').toLowerCase()
    const matchesQuery = !normalizedQuery || searchableText.includes(normalizedQuery)

    return matchesCategory && matchesType && matchesImpact && matchesQuery
  })

  const totalPages = getTotalPages(filteredChanges.length, pageSize)
  const safePage = Math.min(params.page, totalPages)

  return {
    allChanges,
    categoryCounts: PATCH_CATEGORIES.reduce((counts, category) => ({
      ...counts,
      [category]: getCategoryCount(category, allChanges),
    }), {} as Record<PatchCategory, number>),
    highImpactCount: allChanges.filter((change) => change.impact === '높음').length,
    buffCount: allChanges.filter((change) => change.type === '상향').length,
    nerfCount: allChanges.filter((change) => change.type === '하향').length,
    items: getPageItems(filteredChanges, safePage, pageSize),
    totalItems: filteredChanges.length,
    totalPages,
  }
}

export const getPatchHistory = async (
  fallbackHistory: PatchHistoryItem[],
): Promise<PatchHistoryItem[]> => {
  try {
    const { data } = await axiosInstance.get<PatchHistoryItem[]>('/patch-notes/history')
    return data
  } catch {
    return fallbackHistory
  }
}

export const getPatchSummary = async (
  version: string,
  fallbackHistory: PatchHistoryItem[],
): Promise<PatchHistoryItem> => {
  try {
    const { data } = await axiosInstance.get<PatchHistoryItem>(`/patch-notes/${version}`)
    return data
  } catch {
    return fallbackHistory.find((patch) => patch.version === version) ?? fallbackHistory[0]
  }
}

export const getPatchChanges = async (
  params: PatchChangesQuery,
  fallbackData: PatchNotesFallbackData,
): Promise<PatchChangesResponse> => {
  try {
    const { data } = await axiosInstance.get<PatchChangesResponse>(`/patch-notes/${params.patchVersion}/changes`, {
      params: {
        category: params.category,
        changeType: params.changeType,
        highImpactOnly: params.highImpactOnly,
        page: params.page,
        pageSize: params.pageSize ?? PATCH_PAGE_SIZE,
        query: params.query,
      },
    })

    return data
  } catch {
    return buildPatchChangesResponse(params, fallbackData)
  }
}
