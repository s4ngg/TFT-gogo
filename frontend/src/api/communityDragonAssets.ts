const COMMUNITY_DRAGON_BASE_URL = 'https://raw.communitydragon.org/latest/game'
const COMMUNITY_DRAGON_RAW_BASE_URL = 'https://raw.communitydragon.org/latest'

export function communityDragonAssetUrl(assetPath: string) {
  return `${COMMUNITY_DRAGON_BASE_URL}/${assetPath.toLowerCase().replace('.tex', '.png')}`
}

export function communityDragonProfileIconUrl(profileIconId: number) {
  return `${COMMUNITY_DRAGON_RAW_BASE_URL}/plugins/rcp-be-lol-game-data/global/default/v1/profile-icons/${profileIconId}.jpg`
}
