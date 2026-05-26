import { communityDragonAssetUrl } from '../api/communityDragonAssets'
import type { MetaDeck } from '../pages/Dashboard/dashboardData'
import { mockTftMatches } from './riotTftMockData'

const traitIconUrls = {
  guide: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Shepherd.TFT_Set17.tex'),
  starGuardian: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Stargazer.TFT_Set17.tex'),
  bastion: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_9_Bastion.tex'),
  rogue: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Rogue.TFT_Set17.tex'),
  darkStar: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_DarkStar.TFT_Set17.tex'),
  sniper: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_6_Sniper.tex'),
  replicator: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_Replicator.TFT_Set17.tex'),
  psyOps: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_17_PsyOps.TFT_Set17.tex'),
  vanguard: communityDragonAssetUrl('ASSETS/UX/TraitIcons/Trait_Icon_12_Vanguard.TFT_Set12.tex'),
}

const championImageUrls = {
  nami: communityDragonAssetUrl('ASSETS/Characters/TFT17_Nami/Skins/Base/Images/TFT17_Nami_splash_tile_41.TFT_Set17.tex'),
  poppy: communityDragonAssetUrl('ASSETS/Characters/TFT17_Poppy/Skins/Base/Images/TFT17_Poppy_splash_tile_16.TFT_Set17.tex'),
  aurora: communityDragonAssetUrl('ASSETS/Characters/TFT17_Aurora/Skins/Base/Images/TFT17_Aurora_splash_tile_1.TFT_Set17.tex'),
  jinx: communityDragonAssetUrl('ASSETS/Characters/TFT17_Jinx/Skins/Base/Images/TFT17_Jinx_splash_tile_38.TFT_Set17.tex'),
  jhin: communityDragonAssetUrl('ASSETS/Characters/TFT17_Jhin/Skins/Base/Images/TFT17_Jhin_splash_tile_37.TFT_Set17.tex'),
  zed: communityDragonAssetUrl('ASSETS/Characters/TFT17_Zed/Skins/Base/Images/TFT17_Zed_splash_tile_68.TFT_Set17.tex'),
  kaisa: communityDragonAssetUrl('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  viktor: communityDragonAssetUrl('ASSETS/Characters/TFT17_Viktor/Skins/Base/Images/TFT17_Viktor_splash_tile_5.TFT_Set17.tex'),
  rammus: communityDragonAssetUrl('ASSETS/Characters/TFT17_Rammus/Skins/Base/Images/TFT17_Rammus_splash_tile_17.TFT_Set17.tex'),
  vex: communityDragonAssetUrl('ASSETS/Characters/TFT17_Vex/Skins/Base/Images/TFT17_vex_splash_tile_10.TFT_Set17.tex'),
}

const itemIconUrls = {
  spearOfShojin: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_SpearOfShojin.TFT_Set13.tex'),
  morellonomicon: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_Morellonomicon.TFT_Set13.tex'),
  archangelsStaff: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_ArchangelsStaff.TFT_Set13.tex'),
  warmogsArmor: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_WarmogsArmor.TFT_Set13.tex'),
  gargoyleStoneplate: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GargoyleStoneplate.TFT_Set13.tex'),
  blueBuff: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_BlueBuff.TFT_Set13.tex'),
  infinityEdge: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_InfinityEdge.TFT_Set13.tex'),
  giantSlayer: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GiantSlayer.TFT_Set13.tex'),
  guinsoosRageblade: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GuinsoosRageblade.TFT_Set13.tex'),
  bloodthirster: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_Bloodthirster.TFT_Set13.tex'),
  rabadonsDeathcap: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_RabadonsDeathcap.TFT_Set13.tex'),
  dragonsClaw: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_DragonsClaw.TFT_Set13.tex'),
}

export const mockDeckMetaResponse: MetaDeck[] = [
  {
    rank: 1,
    grade: 'S',
    name: '6길잡이 나미',
    winRate: '52.8%',
    top4: '72.4%',
    avgPlace: '3.21',
    pickRate: '8.6%',
    traits: [
      { name: '길잡이', count: 6, iconUrl: traitIconUrls.guide, tone: 'gold' },
      { name: '별수호자', count: 3, iconUrl: traitIconUrls.starGuardian, tone: 'silver' },
      { name: '요새', count: 2, iconUrl: traitIconUrls.bastion, tone: 'bronze' },
    ],
    champions: [
      {
        name: '나미',
        imageUrl: championImageUrls.nami,
        stars: 3,
        items: [
          { name: '쇼진의 창', imageUrl: itemIconUrls.spearOfShojin },
          { name: '모렐로노미콘', imageUrl: itemIconUrls.morellonomicon },
          { name: '대천사의 지팡이', imageUrl: itemIconUrls.archangelsStaff },
        ],
      },
      {
        name: '뽀삐',
        imageUrl: championImageUrls.poppy,
        stars: 2,
        items: [
          { name: '워모그의 갑옷', imageUrl: itemIconUrls.warmogsArmor },
          { name: '가고일 돌갑옷', imageUrl: itemIconUrls.gargoyleStoneplate },
        ],
      },
      { name: '오로라', imageUrl: championImageUrls.aurora, stars: 2, items: [{ name: '블루 버프', imageUrl: itemIconUrls.blueBuff }] },
      { name: '징크스', imageUrl: championImageUrls.jinx, stars: 2 },
    ],
  },
  {
    rank: 2,
    grade: 'A+',
    name: '4습격자 진',
    winRate: '49.7%',
    top4: '68.1%',
    avgPlace: '3.58',
    pickRate: '6.3%',
    traits: [
      { name: '습격자', count: 4, iconUrl: traitIconUrls.rogue, tone: 'gold' },
      { name: '암흑의 별', count: 4, iconUrl: traitIconUrls.darkStar, tone: 'silver' },
      { name: '저격수', count: 2, iconUrl: traitIconUrls.sniper, tone: 'bronze' },
    ],
    champions: [
      {
        name: '진',
        imageUrl: championImageUrls.jhin,
        stars: 2,
        items: [
          { name: '무한의 대검', imageUrl: itemIconUrls.infinityEdge },
          { name: '거인 학살자', imageUrl: itemIconUrls.giantSlayer },
          { name: '구인수의 격노검', imageUrl: itemIconUrls.guinsoosRageblade },
        ],
      },
      { name: '제드', imageUrl: championImageUrls.zed, stars: 2, items: [{ name: '피바라기', imageUrl: itemIconUrls.bloodthirster }] },
      { name: '카이사', imageUrl: championImageUrls.kaisa, stars: 2 },
    ],
  },
  {
    rank: 3,
    grade: 'B',
    name: '4복제자 빅토르',
    winRate: '42.1%',
    top4: '54.7%',
    avgPlace: '4.62',
    pickRate: '3.1%',
    traits: [
      { name: '복제자', count: 4, iconUrl: traitIconUrls.replicator, tone: 'silver' },
      { name: '초능력', count: 3, iconUrl: traitIconUrls.psyOps, tone: 'bronze' },
      { name: '선봉대', count: 2, iconUrl: traitIconUrls.vanguard, tone: 'bronze' },
    ],
    champions: [
      {
        name: '빅토르',
        imageUrl: championImageUrls.viktor,
        stars: 1,
        items: [
          { name: '블루 버프', imageUrl: itemIconUrls.blueBuff },
          { name: '라바돈의 죽음모자', imageUrl: itemIconUrls.rabadonsDeathcap },
        ],
      },
      { name: '람머스', imageUrl: championImageUrls.rammus, stars: 2, items: [{ name: '용의 발톱', imageUrl: itemIconUrls.dragonsClaw }] },
      { name: '벡스', imageUrl: championImageUrls.vex, stars: 2 },
    ],
  },
]

export const mockDeckMetaSource = {
  description: 'Riot TFT match-v1 응답에서 placement, traits, units, itemNames를 집계해 Spring이 내려줄 덱모음 DTO 예시입니다.',
  sourceMatchIds: mockTftMatches.map((match) => match.metadata.match_id),
}
