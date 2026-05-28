const CDRAGON_TFT_KO_URL = 'https://raw.communitydragon.org/latest/cdragon/tft/ko_kr.json'

export interface TFTLocale {
  champByApiName: Map<string, string>  // "tft17_illaoi" → "일라오이"
  traitBySuffix: Map<string, string>   // "shieldtank" → "방패 전위"
  itemByApiName: Map<string, string>   // "tft_item_infinityedge" → "무한의 대검"
  augmentBySuffix: Map<string, string> // "aoerocketgrab" → 한글 증강명
}

type CDragonEntry = { apiName?: string; name?: string }

export async function fetchTFTLocale(): Promise<TFTLocale> {
  const res = await fetch(CDRAGON_TFT_KO_URL)
  if (!res.ok) throw new Error(`CDragon locale fetch 실패: ${res.status}`)
  const data = await res.json()

  const champByApiName = new Map<string, string>()
  const traitBySuffix = new Map<string, string>()
  const itemByApiName = new Map<string, string>()
  const augmentBySuffix = new Map<string, string>()

  // 최신 세트 자동 감지
  const latestSetNum = Math.max(...Object.keys(data.sets ?? {}).map(Number))
  const currentSet = data.sets?.[String(latestSetNum)]

  currentSet?.champions?.forEach((c: CDragonEntry) => {
    if (c.apiName && c.name) {
      champByApiName.set(c.apiName.toLowerCase(), c.name)
    }
  })

  currentSet?.traits?.forEach((t: CDragonEntry) => {
    if (t.apiName && t.name) {
      const suffix = t.apiName.split('_').pop()?.toLowerCase()
      if (suffix) traitBySuffix.set(suffix, t.name)
    }
  })

  currentSet?.augments?.forEach((a: CDragonEntry) => {
    if (a.apiName && a.name) {
      const suffix = a.apiName.split('_').pop()?.toLowerCase()
      if (suffix) augmentBySuffix.set(suffix, a.name)
    }
  })

  data.items?.forEach((item: CDragonEntry) => {
    if (item.apiName && item.name) {
      itemByApiName.set(item.apiName.toLowerCase(), item.name)
    }
  })

  return { champByApiName, traitBySuffix, itemByApiName, augmentBySuffix }
}

/** 챔피언 이미지 URL에서 apiName 추출 후 한글 이름 반환 */
export function getChampionName(imageUrl: string, locale: TFTLocale | undefined, fallback: string): string {
  if (!locale) return fallback
  const m = imageUrl.match(/characters\/([^/]+)\//)
  if (!m) return fallback
  return locale.champByApiName.get(m[1]) ?? fallback
}

/** 트레이트 이름(suffix) → 한글 */
export function getTraitName(name: string, locale: TFTLocale | undefined): string {
  if (!locale) return name
  return locale.traitBySuffix.get(name.toLowerCase()) ?? name
}

/** 아이템 전체 ID → 한글 */
export function getItemName(itemId: string, locale: TFTLocale | undefined): string {
  const fallback = itemId.split('_').pop() ?? itemId
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
