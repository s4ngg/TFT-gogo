export const GAME_ASSET_BASE = 'https://raw.communitydragon.org/latest/game'
export const RAW_ASSET_BASE = 'https://raw.communitydragon.org/latest'

export function asset(path) {
  return `${GAME_ASSET_BASE}/${path.toLowerCase().replace('.tex', '.png')}`
}

export function profileIcon(id) {
  return `${RAW_ASSET_BASE}/plugins/rcp-be-lol-game-data/global/default/v1/profile-icons/${id}.jpg`
}
