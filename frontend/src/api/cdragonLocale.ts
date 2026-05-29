import axiosInstance from './axiosInstance'
import { isRecord } from './apiResponse'

const CDRAGON_TFT_KO_URL = 'https://raw.communitydragon.org/latest/cdragon/tft/ko_kr.json'

export interface TFTLocale {
  champByApiName: Map<string, string>
  champDetailByApiName: Map<string, ChampionDetail>
  traitBySuffix: Map<string, string>
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
      }
    })
}

export async function fetchTFTLocale(): Promise<TFTLocale> {
  try {
    const { data } = await axiosInstance.get<Record<string, unknown>>(CDRAGON_TFT_KO_URL)

    const champByApiName = new Map<string, string>()
    const champDetailByApiName = new Map<string, ChampionDetail>()
    const traitBySuffix = new Map<string, string>()
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
        champByApiName.set(key, c.name)
        champDetailByApiName.set(key, {
          apiName: c.apiName,
          name: c.name,
          cost: c.cost ?? 0,
          role: c.role,
          range: c.stats?.range,
          abilityName: c.ability?.name,
          abilityDesc: c.ability?.desc,
        })
      }
    })

    readCDragonEntries(currentSet?.traits).forEach((t) => {
      if (t.apiName && t.name) {
        const suffix = t.apiName.split('_').pop()?.toLowerCase()
        if (suffix) traitBySuffix.set(suffix, t.name)
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

    return { champByApiName, champDetailByApiName, traitBySuffix, itemByApiName, augmentBySuffix }
  } catch (error) {
    const message = error instanceof Error ? error.message : 'CDragon 데이터 로드 실패'
    throw new Error(`CDragon locale 조회 실패: ${message}`)
  }
}

// apiName(소문자) → 한국 커뮤니티 축약명
// 풀네임이 짧은 챔피언은 제외, 보편적으로 통용되는 것만 등록
const CHAMP_SHORT: Record<string, string> = {
  tft17_masteryi:    '마이',
  tft17_aurelionsol: '아우솔',
  tft17_twistedfate: '트페',
  tft17_blitzcrank:  '블리츠',
  tft17_missfortune: '미포',
  tft17_tahmkench:   '탐켄',
}

/**
 * 덱 이름 표시용 — 축약명이 있으면 축약명, 없으면 일반 한글 이름 반환
 * 예: 마스터 이 → 마이 / 아우렐리온 솔 → 아우솔
 */
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

/** 챔피언 이미지 URL에서 apiName 추출 후 한글 이름 반환 */
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

/** 트레이트 이름(suffix) → 한글 */
export function getTraitName(name: string, locale: TFTLocale | undefined): string {
  if (!locale) return name
  return locale.traitBySuffix.get(name.toLowerCase()) ?? name
}

/** 아이템 전체 ID → 한글 (apiFallback: API가 제공한 원본 itemName) */
export function getItemName(itemId: string, locale: TFTLocale | undefined, apiFallback?: string): string {
  const fallback = apiFallback ?? itemId.split('_').pop() ?? itemId
  if (!locale) return fallback
  return locale.itemByApiName.get(itemId.toLowerCase()) ?? fallback
}

/** 증강 전체 ID → 한글 */
export function getAugmentName(augmentId: string, locale: TFTLocale | undefined): string {
  const fallback = augmentId.split('_').pop() ?? augmentId
  if (!locale) return fallback
  const suffix = augmentId.split('_').pop()?.toLowerCase() ?? ''
  return locale.augmentBySuffix.get(suffix) ?? fallback
}
