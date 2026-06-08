import { communityDragonAssetUrl, tftTraitIconUrl } from '../api/communityDragonAssets'
import type {
  SummonerProfileResponse,
  MatchSummaryResponse,
  MatchUnitResponse,
  MatchTraitResponse,
  MatchParticipantResponse,
} from '../api/summonerApi'

// ── CDN 헬퍼 ───────────────────────────────────────────────
const cImg = (p: string) => communityDragonAssetUrl(p)
const iImg = (name: string) =>
  communityDragonAssetUrl(`ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_${name}.TFT_Set13.tex`)

// ── 시너지 아이콘 (communityDragonAssets.ts 단일 소스) ───────
const traitIcons = {
  vanguard:   tftTraitIconUrl('TFT17_Vanguard'),
  bastion:    tftTraitIconUrl('TFT17_Bastion'),
  darkStar:   tftTraitIconUrl('TFT17_DarkStar'),
  spirit:     tftTraitIconUrl('TFT17_Astronaut'),
  rogue:      tftTraitIconUrl('TFT17_Rogue'),
  stargazer:  tftTraitIconUrl('TFT17_Stargazer'),
  guide:      tftTraitIconUrl('TFT17_Shepherd'),
  sniper:     tftTraitIconUrl('TFT17_Sniper'),
  replicator: tftTraitIconUrl('TFT17_Replicator'),
  psyOps:     tftTraitIconUrl('TFT17_PsyOps'),
}

// ── 챔피언 이미지 ───────────────────────────────────────────
const ci = {
  vex:        cImg('ASSETS/Characters/TFT17_Vex/Skins/Base/Images/TFT17_vex_splash_tile_10.TFT_Set17.tex'),
  jhin:       cImg('ASSETS/Characters/TFT17_Jhin/Skins/Base/Images/TFT17_Jhin_splash_tile_37.TFT_Set17.tex'),
  corki:      cImg('ASSETS/Characters/TFT17_Corki/Skins/Base/Images/TFT17_Corki_splash_tile_26.TFT_Set17.tex'),
  masterYi:   cImg('ASSETS/Characters/TFT17_MasterYi/Skins/Base/Images/TFT17_MasterYi_splash_tile_33.TFT_Set17.tex'),
  lulu:       cImg('ASSETS/Characters/TFT17_Lulu/Skins/Base/Images/TFT17_Lulu_splash_tile_14.TFT_Set17.tex'),
  akali:      cImg('ASSETS/Characters/TFT17_Akali/Skins/Base/Images/TFT17_Akali_splash_tile_68.TFT_Set17.tex'),
  illaoi:     cImg('ASSETS/Characters/TFT17_Illaoi/Skins/Base/Images/TFT17_Illaoi_splash_tile_27.TFT_Set17.tex'),
  xayah:      cImg('ASSETS/Characters/TFT17_Xayah/Skins/Base/Images/TFT17_Xayah_splash_tile_1.TFT_Set17.tex'),
  aurora:     cImg('ASSETS/Characters/TFT17_Aurora/Skins/Base/Images/TFT17_Aurora_splash_tile_1.TFT_Set17.tex'),
  poppy:      cImg('ASSETS/Characters/TFT17_Poppy/Skins/Base/Images/TFT17_Poppy_splash_tile_16.TFT_Set17.tex'),
  briar:      cImg('ASSETS/Characters/TFT17_Briar/Skins/Base/Images/TFT17_Briar_splash_tile_10.TFT_Set17.tex'),
  kaisa:      cImg('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  karma:      cImg('ASSETS/Characters/TFT17_Karma/Skins/Base/Images/TFT17_Karma_splash_tile_8.TFT_Set17.tex'),
  morgana:    cImg('ASSETS/Characters/TFT17_Morgana/Skins/Base/Images/TFT17_Morgana_splash_tile_50.TFT_Set17.tex'),
  belveth:    cImg('ASSETS/Characters/TFT17_Belveth/Skins/Base/Images/TFT17_Belveth_splash_tile_19.TFT_Set17.tex'),
  pyke:       cImg('ASSETS/Characters/TFT17_Pyke/Skins/Base/Images/TFT17_Pyke_splash_tile_25.TFT_Set17.tex'),
  bard:       cImg('ASSETS/Characters/TFT17_Bard/Skins/Base/Images/TFT17_Bard_splash_tile_8.TFT_Set17.tex'),
  sona:       cImg('ASSETS/Characters/TFT17_Sona/Skins/Base/Images/TFT17_Sona_splash_tile_17.TFT_Set17.tex'),
  gnar:       cImg('ASSETS/Characters/TFT17_Gnar/Skins/Base/Images/TFT17_Gnar_splash_tile_15.TFT_Set17.tex'),
  shen:       cImg('ASSETS/Characters/TFT17_Shen/Skins/Base/Images/TFT17_shen_splash_tile_49.TFT_Set17.tex'),
  zed:        cImg('ASSETS/Characters/TFT17_Zed/Skins/Base/Images/TFT17_Zed_splash_tile_68.TFT_Set17.tex'),
  ornn:       cImg('ASSETS/Characters/TFT17_Ornn/Skins/Base/Images/TFT17_Ornn_splash_tile_11.TFT_Set17.tex'),
  rammus:     cImg('ASSETS/Characters/TFT17_Rammus/Skins/Base/Images/TFT17_Rammus_splash_tile_17.TFT_Set17.tex'),
  blitzcrank: cImg('ASSETS/Characters/TFT17_Blitzcrank/Skins/Base/Images/TFT17_Blitzcrank_splash_tile_65.TFT_Set17.tex'),
  viktor:     cImg('ASSETS/Characters/TFT17_Viktor/Skins/Base/Images/TFT17_Viktor_splash_tile_5.TFT_Set17.tex'),
  jinx:       cImg('ASSETS/Characters/TFT17_Jinx/Skins/Base/Images/TFT17_Jinx_splash_tile_38.TFT_Set17.tex'),
  riven:      cImg('ASSETS/Characters/TFT17_Riven/Skins/Base/Images/TFT17_Riven_splash_tile_18.TFT_Set17.tex'),
  nami:       cImg('ASSETS/Characters/TFT17_Nami/Skins/Base/Images/TFT17_Nami_splash_tile_41.TFT_Set17.tex'),
}

// ── 아이템 이미지 ───────────────────────────────────────────
const it = {
  rabadon:    iImg('RabadonsDeathcap'),
  ie:         iImg('InfinityEdge'),
  jeweled:    iImg('JeweledGauntlet'),
  archangel:  iImg('ArchangelsStaff'),
  blue:       iImg('BlueBuff'),
  deathblade: iImg('Bloodthirster'),
  warmog:     iImg('WarmogsArmor'),
  titans:     iImg('TitansResolve'),
  ionic:      iImg('GuinsoosRageblade'),
  bramble:    iImg('GargoyleStoneplate'),
  statikk:    iImg('LastWhisper'),
  nashors:    iImg('SpearOfShojin'),
  hand:       iImg('UnstableConcoction'),
  dragon:     iImg('DragonsClaw'),
  morello:    iImg('Morellonomicon'),
}

// ── 유닛 헬퍼 ──────────────────────────────────────────────
const u = (
  characterId: string, imageUrl: string, stars: 1 | 2 | 3, itemImageUrls: string[],
): MatchUnitResponse => ({ characterId, imageUrl, stars, itemImageUrls })

// ── 시너지 헬퍼 ────────────────────────────────────────────
const tr = (
  traitId: string, name: string, iconUrl: string, count: number,
  tone: 'bronze' | 'silver' | 'gold' | 'prismatic' = 'gold',
): MatchTraitResponse => ({ traitId, name, iconUrl, count, tone })

// ── 덱 구성 8종 ────────────────────────────────────────────
const DECK_CONFIGS: { compositionName: string; traits: MatchTraitResponse[]; units: MatchUnitResponse[] }[] = [
  {
    compositionName: '선봉대 벡스',
    traits: [
      tr('TFT17_Vanguard', '선봉대', traitIcons.vanguard, 4),
      tr('TFT17_DarkStar', '암흑의 별', traitIcons.darkStar, 4, 'silver'),
    ],
    units: [
      u('TFT17_Vex', ci.vex, 3, [it.rabadon, it.jeweled, it.archangel]),
      u('TFT17_Illaoi', ci.illaoi, 2, [it.warmog, it.titans]),
      u('TFT17_Xayah', ci.xayah, 2, [it.ionic]),
      u('TFT17_Aurora', ci.aurora, 2, []),
      u('TFT17_Poppy', ci.poppy, 2, [it.bramble]),
      u('TFT17_Briar', ci.briar, 2, []),
    ],
  },
  {
    compositionName: '6암흑의 별 진',
    traits: [
      tr('TFT17_DarkStar', '암흑의 별', traitIcons.darkStar, 6),
      tr('TFT17_Sniper', '저격수', traitIcons.sniper, 2, 'bronze'),
    ],
    units: [
      u('TFT17_Jhin', ci.jhin, 3, [it.ie, it.deathblade, it.jeweled]),
      u('TFT17_Kaisa', ci.kaisa, 2, [it.statikk, it.nashors]),
      u('TFT17_Karma', ci.karma, 2, [it.blue]),
      u('TFT17_Morgana', ci.morgana, 2, [it.morello]),
      u('TFT17_Belveth', ci.belveth, 2, []),
      u('TFT17_Pyke', ci.pyke, 2, [it.titans]),
    ],
  },
  {
    compositionName: '정령족 코르키',
    traits: [
      tr('TFT17_Astronaut', '정령족', traitIcons.spirit, 4),
      tr('TFT17_Bastion', '요새', traitIcons.bastion, 2, 'bronze'),
    ],
    units: [
      u('TFT17_Corki', ci.corki, 3, [it.blue, it.archangel, it.nashors]),
      u('TFT17_Vex', ci.vex, 2, [it.rabadon]),
      u('TFT17_Aurora', ci.aurora, 2, []),
      u('TFT17_Bard', ci.bard, 2, [it.hand]),
      u('TFT17_Sona', ci.sona, 2, [it.warmog]),
      u('TFT17_Gnar', ci.gnar, 2, []),
    ],
  },
  {
    compositionName: '습격자 마스터 이',
    traits: [
      tr('TFT17_Rogue', '습격자', traitIcons.rogue, 4),
      tr('TFT17_DarkStar', '암흑의 별', traitIcons.darkStar, 4, 'silver'),
    ],
    units: [
      u('TFT17_MasterYi', ci.masterYi, 3, [it.ie, it.deathblade, it.hand]),
      u('TFT17_Pyke', ci.pyke, 2, [it.titans]),
      u('TFT17_Shen', ci.shen, 2, [it.bramble, it.dragon]),
      u('TFT17_Zed', ci.zed, 2, [it.statikk]),
      u('TFT17_Belveth', ci.belveth, 2, []),
      u('TFT17_Akali', ci.akali, 2, [it.jeweled]),
    ],
  },
  {
    compositionName: '별돌보미 블루',
    traits: [
      tr('TFT17_Stargazer', '별돌보미', traitIcons.stargazer, 4),
      tr('TFT17_Astronaut', '정령족', traitIcons.spirit, 4, 'silver'),
    ],
    units: [
      u('TFT17_Lulu', ci.lulu, 3, [it.blue, it.archangel, it.morello]),
      u('TFT17_Poppy', ci.poppy, 2, [it.warmog]),
      u('TFT17_Xayah', ci.xayah, 2, [it.ionic, it.nashors]),
      u('TFT17_Bard', ci.bard, 2, [it.hand]),
      u('TFT17_Sona', ci.sona, 2, []),
      u('TFT17_Gnar', ci.gnar, 2, [it.titans]),
    ],
  },
  {
    compositionName: '8요새 웜풀',
    traits: [
      tr('TFT17_Vanguard', '선봉대', traitIcons.vanguard, 8),
    ],
    units: [
      u('TFT17_Rammus', ci.rammus, 3, [it.warmog, it.bramble, it.dragon]),
      u('TFT17_Poppy', ci.poppy, 2, [it.titans]),
      u('TFT17_Ornn', ci.ornn, 2, [it.warmog]),
      u('TFT17_Blitzcrank', ci.blitzcrank, 2, [it.ionic]),
      u('TFT17_Sona', ci.sona, 2, [it.blue]),
      u('TFT17_Jhin', ci.jhin, 2, [it.ie]),
    ],
  },
  {
    compositionName: '4그림자 암살자',
    traits: [
      tr('TFT17_Rogue', '습격자', traitIcons.rogue, 4),
      tr('TFT17_DarkStar', '암흑의 별', traitIcons.darkStar, 4, 'silver'),
    ],
    units: [
      u('TFT17_Shen', ci.shen, 3, [it.deathblade, it.ie, it.jeweled]),
      u('TFT17_Akali', ci.akali, 2, [it.statikk]),
      u('TFT17_Zed', ci.zed, 2, [it.titans]),
      u('TFT17_Pyke', ci.pyke, 2, [it.hand]),
      u('TFT17_Riven', ci.riven, 2, []),
      u('TFT17_Jhin', ci.jhin, 2, [it.nashors]),
    ],
  },
  {
    compositionName: '발명의 대가 빅토르',
    traits: [
      tr('TFT17_Replicator', '복제자', traitIcons.replicator, 4, 'silver'),
      tr('TFT17_PsyOps', '초능력', traitIcons.psyOps, 3, 'bronze'),
    ],
    units: [
      u('TFT17_Viktor', ci.viktor, 3, [it.rabadon, it.archangel, it.blue]),
      u('TFT17_Corki', ci.corki, 2, [it.nashors]),
      u('TFT17_Jinx', ci.jinx, 2, [it.ie, it.deathblade]),
      u('TFT17_Sona', ci.sona, 2, [it.warmog]),
      u('TFT17_Morgana', ci.morgana, 2, [it.morello]),
      u('TFT17_Pyke', ci.pyke, 2, []),
    ],
  },
]

// ── 참가자 더미 7명 (내 플레이어 제외) ────────────────────────
const FILLER_PARTICIPANTS: Omit<MatchParticipantResponse, 'placement'>[] = [
  {
    puuid: 'mock-puuid-filler-01', riotIdGameName: 'Faker', riotIdTagline: 'T1',
    stage: '6-3',
    playersEliminated: 2, goldLeft: 3,
    traits: [tr('TFT17_DarkStar', '암흑의 별', traitIcons.darkStar, 6), tr('TFT17_Astronaut', '정령족', traitIcons.spirit, 4, 'silver')],
    units: [u('TFT17_Jhin', ci.jhin, 3, [it.ie, it.deathblade, it.jeweled]), u('TFT17_Kaisa', ci.kaisa, 2, [it.statikk]), u('TFT17_Karma', ci.karma, 2, [it.blue])],
  },
  {
    puuid: 'mock-puuid-filler-02', riotIdGameName: '마포이는카', riotIdTagline: 'HAPPY',
    stage: '5-6',
    playersEliminated: 1, goldLeft: 7,
    traits: [tr('TFT17_Vanguard', '선봉대', traitIcons.vanguard, 4), tr('TFT17_Astronaut', '정령족', traitIcons.spirit, 4, 'silver')],
    units: [u('TFT17_Corki', ci.corki, 3, [it.blue, it.archangel, it.nashors]), u('TFT17_Vex', ci.vex, 2, [it.rabadon]), u('TFT17_Aurora', ci.aurora, 2, [])],
  },
  {
    puuid: 'mock-puuid-filler-03', riotIdGameName: '서은지', riotIdTagline: '봄봄7',
    stage: '6-2',
    playersEliminated: 1, goldLeft: 12,
    traits: [tr('TFT17_Rogue', '습격자', traitIcons.rogue, 6), tr('TFT17_DarkStar', '암흑의 별', traitIcons.darkStar, 4, 'silver')],
    units: [u('TFT17_MasterYi', ci.masterYi, 3, [it.ie, it.deathblade, it.hand]), u('TFT17_Pyke', ci.pyke, 2, [it.titans]), u('TFT17_Akali', ci.akali, 2, [it.jeweled])],
  },
  {
    puuid: 'mock-puuid-filler-04', riotIdGameName: '산천어풍보대사', riotIdTagline: '1234',
    stage: '5-5',
    playersEliminated: 0, goldLeft: 8,
    traits: [tr('TFT17_Stargazer', '별돌보미', traitIcons.stargazer, 4), tr('TFT17_Astronaut', '정령족', traitIcons.spirit, 4, 'silver')],
    units: [u('TFT17_Lulu', ci.lulu, 3, [it.blue, it.archangel, it.morello]), u('TFT17_Poppy', ci.poppy, 2, [it.warmog]), u('TFT17_Xayah', ci.xayah, 2, [it.ionic])],
  },
  {
    puuid: 'mock-puuid-filler-05', riotIdGameName: '복조선특크', riotIdTagline: 'KR1',
    stage: '5-3',
    playersEliminated: 1, goldLeft: 5,
    traits: [tr('TFT17_Vanguard', '선봉대', traitIcons.vanguard, 8)],
    units: [u('TFT17_Rammus', ci.rammus, 3, [it.warmog, it.bramble, it.dragon]), u('TFT17_Poppy', ci.poppy, 2, [it.titans]), u('TFT17_Ornn', ci.ornn, 2, [it.warmog])],
  },
  {
    puuid: 'mock-puuid-filler-06', riotIdGameName: '판키', riotIdTagline: '5597',
    stage: '5-2',
    playersEliminated: 0, goldLeft: 11,
    traits: [tr('TFT17_Rogue', '습격자', traitIcons.rogue, 4), tr('TFT17_Vanguard', '선봉대', traitIcons.vanguard, 4, 'silver')],
    units: [u('TFT17_Shen', ci.shen, 3, [it.deathblade, it.ie, it.jeweled]), u('TFT17_Akali', ci.akali, 2, [it.statikk]), u('TFT17_Zed', ci.zed, 2, [it.titans])],
  },
  {
    puuid: 'mock-puuid-filler-07', riotIdGameName: '뻐코영', riotIdTagline: 'KR1',
    stage: '4-6',
    playersEliminated: 0, goldLeft: 15,
    traits: [tr('TFT17_Astronaut', '정령족', traitIcons.spirit, 6)],
    units: [u('TFT17_Viktor', ci.viktor, 3, [it.rabadon, it.archangel, it.blue]), u('TFT17_Corki', ci.corki, 2, [it.nashors]), u('TFT17_Jinx', ci.jinx, 2, [it.ie])],
  },
]

const STAGE_BY_RANK = ['6-5', '6-3', '6-2', '5-6', '5-5', '5-3', '5-2', '4-6']

// 90게임 배치 결과
const PLACEMENTS_90 = [
  1, 2, 4, 6, 7, 3, 5, 1, 2, 3, 4, 1, 6, 5, 8, 2, 3, 1, 4, 7,
  5, 2, 1, 3, 6, 4, 2, 1, 5, 4, 2, 1, 3, 5, 8, 4, 6, 2, 1, 3,
  7, 4, 2, 1, 5, 3, 6, 4, 2, 1, 8, 3, 5, 2, 4, 1, 3, 7, 5, 2,
  1, 4, 6, 3, 2, 5, 1, 7, 3, 4, 2, 1, 5, 3, 8, 4, 2, 6, 1, 3,
  4, 2, 1, 5, 3, 6, 2, 4, 1, 7,
]

// Unix timestamp: 가장 최근 게임부터 역순
const BASE_TIME = 1779784800000
const TIME_STEP = 3600000 * 2  // 2시간 간격

// ── 소환사 프로필 mock ──────────────────────────────────────
export const mockSummonerProfile: SummonerProfileResponse = {
  puuid: 'mock-puuid-player-01',
  gameName: 'TFTgogo',
  tagLine: 'KR1',
  profileIconId: 29,
  summonerLevel: 387,
  tier: 'DIAMOND',
  rank: 'IV',
  leaguePoints: 45,
  wins: 256,
  losses: 137,
}

// ── 매치 히스토리 mock (90게임) ─────────────────────────────
export const mockMatchHistory: MatchSummaryResponse[] = PLACEMENTS_90.map((placement, i) => {
  const cfg = DECK_CONFIGS[i % DECK_CONFIGS.length]
  const myRank = placement
  const otherRanks = [1, 2, 3, 4, 5, 6, 7, 8].filter((r) => r !== myRank)
  const participants: MatchParticipantResponse[] = [
    {
      puuid: 'mock-puuid-player-01',
      riotIdGameName: 'TFTgogo',
      riotIdTagline: 'KR1',
      placement: myRank,
      stage: STAGE_BY_RANK[myRank - 1],
      traits: cfg.traits,
      units: cfg.units,
      augments: ['TFT17_Augment_Guidebook', 'TFT17_Augment_Manaflow', 'TFT17_Augment_FinalPolish'],
      playersEliminated: Math.max(0, 3 - myRank + 1),
      goldLeft: (i * 3 + myRank) % 18 + 2,
    },
    ...FILLER_PARTICIPANTS.map((p, fi) => ({ ...p, placement: otherRanks[fi] })),
  ].sort((a, b) => a.placement - b.placement)

  return {
    matchId: `KR_760001${String(i + 1).padStart(4, '0')}`,
    placement,
    gameDateTime: BASE_TIME - i * TIME_STEP,
    gameType: i % 5 === 4 ? 'NORMAL' : 'RANKED',
    compositionName: cfg.compositionName,
    traits: cfg.traits,
    units: cfg.units,
    augments: ['TFT17_Augment_Guidebook', 'TFT17_Augment_Manaflow', 'TFT17_Augment_FinalPolish'],
    participants,
  }
})
