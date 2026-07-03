import type React from 'react'

export const TFT_ASSET_CONFIG = {
  currentSetNumber: 17,
  currentSetTag: 'tft_set17',
  fallbackItemSetTag: 'tft_set13',
  communityDragonGameBaseUrl: 'https://raw.communitydragon.org/latest/game',
  communityDragonRawBaseUrl: 'https://raw.communitydragon.org/latest',
} as const

const TFT_SET_PATTERN = /(?:set|tft)(\d+)/i
const TFT_SET_TAG_PATTERN = /\.tft_set\d+\./i

function readTftSetNumber(value: string): number | undefined {
  const match = value.match(TFT_SET_PATTERN)
  if (!match) return undefined

  const setNumber = Number(match[1])
  return Number.isInteger(setNumber) && setNumber > 0 ? setNumber : undefined
}

export function tftSetTag(setNumber: number = TFT_ASSET_CONFIG.currentSetNumber): string {
  return `tft_set${setNumber}`
}

export function tftSetTagFromId(value: string): string {
  return tftSetTag(readTftSetNumber(value) ?? TFT_ASSET_CONFIG.currentSetNumber)
}

function tftSetFileSuffix(setNumber: number): string {
  return `TFT_Set${setNumber}`
}

function tftSetFileSuffixFromId(value: string): string {
  return tftSetFileSuffix(readTftSetNumber(value) ?? TFT_ASSET_CONFIG.currentSetNumber)
}

function traitIconPath(iconSetNumber: number, traitName: string): string {
  return `ASSETS/UX/TraitIcons/Trait_Icon_${iconSetNumber}_${traitName}.${tftSetFileSuffix(iconSetNumber)}.tex`
}

// Set 태그 없이 고정 경로를 사용하는 트레이트 아이콘 (CDragon에서 Set 태그 없는 경로에만 존재)
function traitIconPathWithoutSet(iconSetNumber: number, traitName: string): string {
  return `ASSETS/UX/TraitIcons/Trait_Icon_${iconSetNumber}_${traitName}.tex`
}

export function communityDragonAssetUrl(assetPath: string) {
  return `${TFT_ASSET_CONFIG.communityDragonGameBaseUrl}/${assetPath.toLowerCase().replace('.tex', '.png')}`
}

export function communityDragonProfileIconUrl(profileIconId: number) {
  return `${TFT_ASSET_CONFIG.communityDragonRawBaseUrl}/plugins/rcp-be-lol-game-data/global/default/v1/profile-icons/${profileIconId}.jpg`
}

export function itemsFromUrls(urls: string[]): { imageUrl: string; name: string }[] {
  return urls.map((url) => ({
    imageUrl: url,
    name: url.split('/').pop()?.split('.')[0] || url,
  }))
}

export function tftItemIconUrl(itemId: string, setTag: string = TFT_ASSET_CONFIG.currentSetTag): string {
  const normalized = itemId.toLowerCase()
  return `${TFT_ASSET_CONFIG.communityDragonGameBaseUrl}/assets/maps/tft/icons/items/hexcore/${normalized}.${setTag}.png`
}

export function tftItemIconOnError(e: React.SyntheticEvent<HTMLImageElement>): void {
  const img = e.currentTarget
  const currentSrc = img.src
  const currentSetTag = currentSrc.match(TFT_SET_TAG_PATTERN)?.[0]
  const fallbackSetTag = `.${TFT_ASSET_CONFIG.fallbackItemSetTag}.`

  // 1차 fallback: Set17 → Set13
  if (currentSetTag && currentSetTag.toLowerCase() !== fallbackSetTag) {
    img.src = currentSrc.replace(currentSetTag, fallbackSetTag)
    return
  }

  // 2차 fallback: hexcore 경로가 Set13에도 없으면 /standard/ 경로 시도
  // (Set17 신규 아이템은 hexcore에 없고 standard 경로에만 존재하는 경우가 있음)
  const hexcorePattern = /\/assets\/maps\/tft\/icons\/items\/hexcore\/([^/]+)\.tft_set\d+\.png/i
  const match = currentSrc.match(hexcorePattern)
  if (match) {
    const itemId = match[1]
    img.src = `${TFT_ASSET_CONFIG.communityDragonGameBaseUrl}/assets/maps/particles/tft/item_icons/standard/${itemId}.png`
    return
  }

  img.style.opacity = '0'
}

const TRAIT_ICON_PATHS: Readonly<Record<string, string>> = {
  tft17_vanguard:   traitIconPath(12, 'Vanguard'),           // Set17에 없고 Set12 경로가 CDragon 200
  tft17_darkstar:   traitIconPath(17, 'DarkStar'),
  tft17_astronaut:  traitIconPath(17, 'Astronaut'),
  tft17_rogue:      traitIconPath(17, 'Rogue'),
  tft17_stargazer:  traitIconPath(17, 'Stargazer'),
  tft17_shepherd:   traitIconPath(17, 'Shepherd'),
  tft17_sniper:     traitIconPathWithoutSet(6, 'Sniper'),    // Set 태그 없는 경로에만 존재
  tft17_replicator: traitIconPath(17, 'Replicator'),
  tft17_psyops:     traitIconPath(17, 'PsyOps'),
  tft17_bastion:    traitIconPathWithoutSet(9, 'Bastion'),   // Set 태그 없는 경로에만 존재
}

const championImagePaths: Readonly<Record<string, string>> = {
  tft17_darkstar_fakeunit: 'ASSETS/Characters/TFT17_DarkStar_FakeUnit/HUD/TFT17_DarkStar_FakeUnit_SmallSplash.TFT_Set17.tex',
  tft17_rhaast: 'ASSETS/Characters/TFT17_Rhaast/HUD/TFT17_Kayn_Slay_Square.TFT_Set17.tex',
  // 바드 소환수(Follower)는 챔피언 목록에 없어 전용 square 아이콘이 CDragon에 존재하지 않음 -> 바드 본체 아이콘 재사용
  tft17_bard_follower: 'ASSETS/Characters/TFT17_Bard/HUD/TFT17_Bard_Square.TFT_Set17.tex',
}

export function tftTraitIconUrl(traitId: string): string {
  const normalizedTraitId = traitId.trim()
  const path = TRAIT_ICON_PATHS[normalizedTraitId.toLowerCase()]
  if (path) return communityDragonAssetUrl(path)

  const setNumber = readTftSetNumber(normalizedTraitId) ?? TFT_ASSET_CONFIG.currentSetNumber
  const traitName = normalizedTraitId.replace(/^(?:TFT|Set)\d+_/i, '')
  return communityDragonAssetUrl(traitIconPath(setNumber, traitName))
}

export function tftChampSquareUrl(apiName: string): string {
  const overridePath = championImagePaths[apiName.trim().toLowerCase()]
  if (overridePath) return communityDragonAssetUrl(overridePath)

  return communityDragonAssetUrl(`ASSETS/Characters/${apiName}/HUD/${apiName}_Square.${tftSetFileSuffixFromId(apiName)}.tex`)
}

export function tftTierEmblemUrl(tier: string): string {
  const t = tier.toLowerCase()
  return `${TFT_ASSET_CONFIG.communityDragonRawBaseUrl}/plugins/rcp-fe-lol-static-assets/global/default/ranked-emblem/emblem-${t}.png`
}
