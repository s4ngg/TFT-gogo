import { communityDragonAssetUrl } from '../../api/communityDragonAssets'
import type { ChangeCategory, PatchChange } from '../../api/patchNotes'

export const PATCH_FALLBACK_IMAGE = '/assets/emblems/patch-meta-emblem-pink.png'

const categoryImageUrl: Record<ChangeCategory, string> = {
  챔피언: communityDragonAssetUrl('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  시너지: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Challenger.TFT_Set17.tex'),
  아이템: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GuinsoosRageblade.TFT_Set13.tex'),
  증강체: communityDragonAssetUrl('ASSETS/UX/TFT/Augments/Augment_Silver.tex'),
  시스템: PATCH_FALLBACK_IMAGE,
}

const targetImageUrl: Record<string, string> = {
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

function getBaseTarget(target: string) {
  return target.trim()
}

export function getPatchChangeImageUrl(change: PatchChange) {
  const directImageUrl = change.imageUrl?.trim()
  if (directImageUrl) return directImageUrl

  return targetImageUrl[getBaseTarget(change.target)] ?? categoryImageUrl[change.category]
}
