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
  const currentSetTag = img.src.match(TFT_SET_TAG_PATTERN)?.[0]
  const fallbackSetTag = `.${TFT_ASSET_CONFIG.fallbackItemSetTag}.`

  if (currentSetTag && currentSetTag.toLowerCase() !== fallbackSetTag) {
    img.src = img.src.replace(currentSetTag, fallbackSetTag)
    return
  }

  img.style.opacity = '0'
}

const TRAIT_ICON_PATHS: Readonly<Record<string, string>> = {
  tft17_vanguard: traitIconPath(12, 'Vanguard'),
  tft17_darkstar: traitIconPath(17, 'DarkStar'),
  tft17_astronaut: traitIconPath(17, 'Astronaut'),
  tft17_rogue: traitIconPath(17, 'Rogue'),
  tft17_stargazer: traitIconPath(17, 'Stargazer'),
  tft17_shepherd: traitIconPath(17, 'Shepherd'),
  tft17_sniper: traitIconPathWithoutSet(6, 'Sniper'),
  tft17_replicator: traitIconPath(17, 'Replicator'),
  tft17_psyops: traitIconPath(17, 'PsyOps'),
  tft17_bastion: traitIconPathWithoutSet(9, 'Bastion'),
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
  return communityDragonAssetUrl(`ASSETS/Characters/${apiName}/HUD/${apiName}_Square.${tftSetFileSuffixFromId(apiName)}.tex`)
}

export function tftTierEmblemUrl(tier: string): string {
  const t = tier.toLowerCase()
  return `${TFT_ASSET_CONFIG.communityDragonRawBaseUrl}/plugins/rcp-fe-lol-static-assets/global/default/ranked-emblem/emblem-${t}.png`
}
