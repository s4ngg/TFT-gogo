import {
  communityDragonAssetUrl,
  TFT_ASSET_CONFIG,
  tftChampSquareUrl,
  tftItemIconUrl,
  tftTraitIconUrl,
} from '../../api/communityDragonAssets'
import type { ChangeCategory, PatchChange } from '../../api/patchNotes'

export const PATCH_FALLBACK_IMAGE = '/assets/emblems/patch-meta-emblem-pink.png'

function fallbackItemIconUrl(itemId: string) {
  return tftItemIconUrl(itemId, TFT_ASSET_CONFIG.fallbackItemSetTag)
}

const categoryImageUrl: Record<ChangeCategory, string> = {
  챔피언: tftChampSquareUrl('TFT17_Kaisa'),
  시너지: tftTraitIconUrl('TFT17_Challenger'),
  아이템: fallbackItemIconUrl('TFT_Item_GuinsoosRageblade'),
  증강체: communityDragonAssetUrl('ASSETS/UX/TFT/Augments/Augment_Silver.tex'),
  시스템: PATCH_FALLBACK_IMAGE,
}

const targetImageUrl: Record<string, string> = {
  아펠리오스: tftChampSquareUrl('TFT17_Aphelios'),
  세주아니: tftChampSquareUrl('TFT17_Sejuani'),
  럭스: tftChampSquareUrl('TFT17_Lux'),
  카이사: tftChampSquareUrl('TFT17_Kaisa'),
  오른: tftChampSquareUrl('TFT17_Ornn'),
  학살자: tftTraitIconUrl('TFT17_Rogue'),
  마법사: tftTraitIconUrl('TFT17_Fateweaver'),
  감시자: tftTraitIconUrl('TFT17_Vanguard'),
  전략가: tftTraitIconUrl('TFT17_Stargazer'),
  결투가: tftTraitIconUrl('TFT17_Challenger'),
  '구인수의 격노검': fallbackItemIconUrl('TFT_Item_GuinsoosRageblade'),
  '워모그의 갑옷': fallbackItemIconUrl('TFT_Item_WarmogsArmor'),
  '쇼진의 창': fallbackItemIconUrl('TFT_Item_SpearOfShojin'),
  '이온 충격기': fallbackItemIconUrl('TFT_Item_IonicSpark'),
  '거인 학살자': fallbackItemIconUrl('TFT_Item_GiantSlayer'),
}

function getBaseTarget(target: string) {
  return target.replace(/\s샘플\s\d+$/, '')
}

export function getPatchChangeImageUrl(change: PatchChange) {
  const directImageUrl = change.imageUrl?.trim()
  if (directImageUrl) return directImageUrl

  return targetImageUrl[getBaseTarget(change.target)] ?? categoryImageUrl[change.category]
}
