import axiosInstance from './axiosInstance'
import { isRecord } from './apiResponse'

const CDRAGON_TFT_KO_URL = '/cdragon/tft/ko-kr'
const CDRAGON_TFT_KO_TIMEOUT_MS = 60_000

export type BreakpointTier = 'bronze' | 'silver' | 'gold' | 'prismatic'

export interface TraitBreakpoint {
  minUnits: number
  tier: BreakpointTier
}

export interface TraitDetail {
  name: string
  breakpoints: TraitBreakpoint[]
}

export interface TFTLocale {
  champByApiName: Map<string, string>
  champDetailByApiName: Map<string, ChampionDetail>
  traitBySuffix: Map<string, string>
  traitDetailBySuffix: Map<string, TraitDetail>
  itemByApiName: Map<string, string>
  augmentBySuffix: Map<string, string>
}

export interface ChampionDetail {
  apiName: string
  name: string
  cost: number
  role?: string
  range?: number
  abilityName?: string
  abilityDesc?: string
  traits: string[]   // lowercase suffix, e.g. ['brawler', 'slayer']
}

interface CDragonTraitEffect {
  minUnits?: number
  style?: number   // 1=bronze 2=silver 3=gold 4=prismatic
}

interface CDragonEntry {
  ability?: {
    desc?: string
    name?: string
  }
  apiName?: string
  cost?: number
  name?: string
  role?: string
  stats?: {
    range?: number
  }
  traits?: string[]
  effects?: CDragonTraitEffect[]
}

const STYLE_TO_TIER: Record<number, BreakpointTier> = {
  1: 'bronze',
  2: 'silver',
  3: 'gold',
  4: 'prismatic',
}

function readCDragonEntries(value: unknown): CDragonEntry[] {
  if (!Array.isArray(value)) return []

  return value
    .filter(isRecord)
    .map((entry) => {
      const ability = isRecord(entry.ability) ? entry.ability : undefined
      const stats = isRecord(entry.stats) ? entry.stats : undefined

      return {
        ability: ability
          ? {
              desc: typeof ability.desc === 'string' ? ability.desc : undefined,
              name: typeof ability.name === 'string' ? ability.name : undefined,
            }
          : undefined,
        apiName: typeof entry.apiName === 'string' ? entry.apiName : undefined,
        cost: typeof entry.cost === 'number' ? entry.cost : undefined,
        name: typeof entry.name === 'string' ? entry.name : undefined,
        role: typeof entry.role === 'string' ? entry.role : undefined,
        stats: stats
          ? {
              range: typeof stats.range === 'number' ? stats.range : undefined,
            }
          : undefined,
        traits: Array.isArray(entry.traits)
          ? (entry.traits as unknown[]).filter((t): t is string => typeof t === 'string')
          : undefined,
        effects: Array.isArray(entry.effects)
          ? (entry.effects as unknown[]).filter(isRecord).map((e) => ({
              minUnits: typeof e.minUnits === 'number' ? e.minUnits : undefined,
              style: typeof e.style === 'number' ? e.style : undefined,
            }))
          : undefined,
      }
    })
}

export async function fetchTFTLocale(): Promise<TFTLocale> {
  try {
    const { data: response } = await axiosInstance.get<{ data: Record<string, unknown> }>(CDRAGON_TFT_KO_URL, {
      timeout: CDRAGON_TFT_KO_TIMEOUT_MS,
    })
    const { data } = response

    const champByApiName = new Map<string, string>()
    const champDetailByApiName = new Map<string, ChampionDetail>()
    const traitBySuffix = new Map<string, string>()
    const traitDetailBySuffix = new Map<string, TraitDetail>()
    const itemByApiName = new Map<string, string>()
    const augmentBySuffix = new Map<string, string>()

    // 최신 세트 자동 감지
    const sets = isRecord(data.sets) ? data.sets : {}
    const setNumbers = Object.keys(sets).map(Number).filter(Number.isFinite)
    const latestSetKey = setNumbers.length > 0 ? String(Math.max(...setNumbers)) : undefined
    const currentSet = latestSetKey && isRecord(sets[latestSetKey]) ? sets[latestSetKey] : undefined

    readCDragonEntries(currentSet?.champions).forEach((c) => {
      if (c.apiName && c.name) {
        const key = c.apiName.toLowerCase()
        // traits: full ID -> lowercase suffix (e.g. "TFT17_Brawler" -> "brawler")
        const traits = (c.traits ?? [])
          .map((t) => t.split('_').pop()?.toLowerCase() ?? '')
          .filter(Boolean)
        champByApiName.set(key, c.name)
        champDetailByApiName.set(key, {
          apiName: c.apiName,
          name: c.name,
          cost: c.cost ?? 0,
          role: c.role,
          range: c.stats?.range,
          abilityName: c.ability?.name,
          abilityDesc: c.ability?.desc,
          traits,
        })
      }
    })

    readCDragonEntries(currentSet?.traits).forEach((t) => {
      if (t.apiName && t.name) {
        const suffix = t.apiName.split('_').pop()?.toLowerCase()
        if (suffix) {
          traitBySuffix.set(suffix, t.name)
          const breakpoints: TraitBreakpoint[] = (t.effects ?? [])
            .filter((e): e is CDragonTraitEffect & { minUnits: number; style: number } =>
              e.minUnits != null && e.style != null && STYLE_TO_TIER[e.style] != null,
            )
            .map((e) => ({ minUnits: e.minUnits, tier: STYLE_TO_TIER[e.style] }))
          if (breakpoints.length > 0) {
            traitDetailBySuffix.set(suffix, { name: t.name, breakpoints })
          }
        }
      }
    })

    readCDragonEntries(currentSet?.augments).forEach((a) => {
      if (a.apiName && a.name) {
        const suffix = a.apiName.split('_').pop()?.toLowerCase()
        if (suffix) augmentBySuffix.set(suffix, a.name)
      }
    })

    readCDragonEntries(data.items).forEach((item) => {
      if (item.apiName && item.name) {
        itemByApiName.set(item.apiName.toLowerCase(), item.name)
      }
    })

    return { champByApiName, champDetailByApiName, traitBySuffix, traitDetailBySuffix, itemByApiName, augmentBySuffix }
  } catch (error) {
    const message = error instanceof Error ? error.message : 'CDragon 데이터 로드 실패'
    throw new Error(`CDragon locale 조회 실패: ${message}`)
  }
}

// apiName(소문자) -> 한국 커뮤니티 축약명
// 한글 음절 5글자 이상인 긴 이름만 등록. 4글자 이하는 축약해도 어색함.
// 예: 탐 켄치, 마스터 이는 제외 / 블리츠크랭크는 등록
const CHAMP_SHORT: Record<string, string> = {
  tft17_aurelionsol: '아우솔',
  tft17_twistedfate: '트페',
  tft17_blitzcrank: '블리츠',
}

export function getChampionShortName(
  imageUrl: string,
  locale: TFTLocale | undefined,
  fallback: string,
): string {
  const apiName = getChampionApiName(imageUrl)
  if (apiName) {
    const abbrev = CHAMP_SHORT[apiName]
    if (abbrev) return abbrev
  }
  return getChampionName(imageUrl, locale, fallback)
}

export function getChampionName(imageUrl: string, locale: TFTLocale | undefined, fallback: string): string {
  if (!locale) return fallback
  const apiName = getChampionApiName(imageUrl)
  if (!apiName) return fallback
  return locale.champByApiName.get(apiName) ?? fallback
}

export function getChampionApiName(imageUrl: string): string | undefined {
  const match = imageUrl.match(/characters\/([^/]+)\//)
  return match?.[1]?.toLowerCase()
}

export function getChampionDetail(
  imageUrl: string,
  locale: TFTLocale | undefined,
): ChampionDetail | undefined {
  if (!locale) return undefined
  const apiName = getChampionApiName(imageUrl)
  return apiName ? locale.champDetailByApiName.get(apiName) : undefined
}

export function getTraitName(name: string, locale: TFTLocale | undefined): string {
  if (!locale) return name
  return locale.traitBySuffix.get(name.toLowerCase()) ?? name
}

// item apiName -> localized name. apiFallback is the original item name from the API.
export function getItemName(itemId: string, locale: TFTLocale | undefined, apiFallback?: string): string {
  const fallback = apiFallback ?? itemId.split('_').pop() ?? itemId
  if (!locale) return fallback
  return locale.itemByApiName.get(itemId.toLowerCase()) ?? fallback
}

export function getAugmentName(augmentId: string, locale: TFTLocale | undefined): string {
  const fallback = augmentId.split('_').pop() ?? augmentId
  if (!locale) return fallback
  const suffix = augmentId.split('_').pop()?.toLowerCase() ?? ''
  return locale.augmentBySuffix.get(suffix) ?? fallback
}
