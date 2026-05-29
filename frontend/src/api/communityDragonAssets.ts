const COMMUNITY_DRAGON_BASE_URL = 'https://raw.communitydragon.org/latest/game'
const COMMUNITY_DRAGON_RAW_BASE_URL = 'https://raw.communitydragon.org/latest'

export function communityDragonAssetUrl(assetPath: string) {
  return `${COMMUNITY_DRAGON_BASE_URL}/${assetPath.toLowerCase().replace('.tex', '.png')}`
}

export function communityDragonProfileIconUrl(profileIconId: number) {
  return `${COMMUNITY_DRAGON_RAW_BASE_URL}/plugins/rcp-be-lol-game-data/global/default/v1/profile-icons/${profileIconId}.jpg`
}

export function itemsFromUrls(urls: string[]): { imageUrl: string; name: string }[] {
  return urls.map((url) => ({
    imageUrl: url,
    name: url.split('/').pop()?.split('.')[0] || url,
  }))
}

/**
 * Riot API itemId(예: "TFT_Item_InfinityEdge")로 CDragon 아이템 아이콘 URL 반환
 */
export function tftItemIconUrl(itemId: string): string {
  const normalized = itemId.toLowerCase()
  return `${COMMUNITY_DRAGON_BASE_URL}/assets/maps/tft/icons/items/hexcore/${normalized}.tft_set13.png`
}

/**
 * TFT 시너지 traitId(예: "TFT17_Vanguard")로 CDragon 아이콘 URL 반환.
 * 덱 탭에서 검증된 CDN 경로를 단일 소스로 관리한다.
 */
const TRAIT_ICON_PATHS: Record<string, string> = {
  TFT17_Vanguard:   'ASSETS/UX/TraitIcons/Trait_Icon_12_Vanguard.TFT_Set12.tex',
  TFT17_DarkStar:   'ASSETS/UX/TraitIcons/Trait_Icon_17_DarkStar.TFT_Set17.tex',
  TFT17_Astronaut:  'ASSETS/UX/TraitIcons/Trait_Icon_17_Astronaut.TFT_Set17.tex',
  TFT17_Rogue:      'ASSETS/UX/TraitIcons/Trait_Icon_17_Rogue.TFT_Set17.tex',
  TFT17_Stargazer:  'ASSETS/UX/TraitIcons/Trait_Icon_17_Stargazer.TFT_Set17.tex',
  TFT17_Shepherd:   'ASSETS/UX/TraitIcons/Trait_Icon_17_Shepherd.TFT_Set17.tex',
  TFT17_Sniper:     'ASSETS/UX/TraitIcons/Trait_Icon_6_Sniper.tex',
  TFT17_Replicator: 'ASSETS/UX/TraitIcons/Trait_Icon_17_Replicator.TFT_Set17.tex',
  TFT17_PsyOps:     'ASSETS/UX/TraitIcons/Trait_Icon_17_PsyOps.TFT_Set17.tex',
  TFT17_Bastion:    'ASSETS/UX/TraitIcons/Trait_Icon_9_Bastion.tex',
}

export function tftTraitIconUrl(traitId: string): string {
  const path = TRAIT_ICON_PATHS[traitId]
  return path ? communityDragonAssetUrl(path) : ''
}
