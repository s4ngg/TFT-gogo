import { ChevronDown, ChevronUp, Coins, RefreshCcw, Search, Swords } from 'lucide-react'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { communityDragonAssetUrl, communityDragonProfileIconUrl } from '../../api/communityDragonAssets'
import { AppLayout } from '../../components/layout'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import type { TraitHexBadgeTone } from '../../components/common/TraitHexBadge'
import styles from './SummonerDetail.module.css'

/* ── 소환사 더미 ── */
const DUMMY_TIER = {
  tier: '다이아몬드', division: 'IV', lp: 45,
  wins: 256, losses: 137,
  level: 387, profileIconId: 29,
  avgPlace: 3.6, top4Rate: 67,
}
const DUMMY_RANK_DIST = [18, 22, 21, 20, 19, 17, 16, 10]

/* ── 30게임 요약 ── */
const PLACEMENTS_30 = [
  1, 2, 4, 6, 7, 3, 5, 1, 2, 3,
  4, 1, 6, 5, 8, 2, 3, 1, 4, 7,
  5, 2, 1, 3, 6, 4, 2, 1, 5, 4,
]
const ALL_PLACEMENTS = [
  ...PLACEMENTS_30,
  2, 1, 3, 5, 8, 4, 6, 2, 1, 3,
  7, 4, 2, 1, 5, 3, 6, 4, 2, 1,
  8, 3, 5, 2, 4, 1, 3, 7, 5, 2,
  1, 4, 6, 3, 2, 5, 1, 7, 3, 4,
  2, 1, 5, 3, 8, 4, 2, 6, 1, 3,
  4, 2, 1, 5, 3, 6, 2, 4, 1, 7,
]
const DUMMY_SUMMARY = { games: 30, top4: 20, top4Rate: 66.7, avgPlace: 3.6 }

/* ── 시너지 아이콘 ── */
const turl = (p: string) => communityDragonAssetUrl(p)
const traitIcons = {
  vanguard:  turl('ASSETS/UX/TraitIcons/Trait_Icon_12_Vanguard.TFT_Set12.tex'),
  darkStar:  turl('ASSETS/UX/TraitIcons/Trait_Icon_17_DarkStar.TFT_Set17.tex'),
  spirit:    turl('ASSETS/UX/TraitIcons/Trait_Icon_17_Astronaut.TFT_Set17.tex'),
  rogue:     turl('ASSETS/UX/TraitIcons/Trait_Icon_17_Rogue.TFT_Set17.tex'),
  stargazer: turl('ASSETS/UX/TraitIcons/Trait_Icon_17_Stargazer.TFT_Set17.tex'),
}

const DUMMY_TOP_TRAITS = [
  { name: '선봉대',    count: 4, iconUrl: traitIcons.vanguard,  tone: 'gold' as TraitHexBadgeTone, games: 32, avgPlace: 3.2 },
  { name: '암흑의 별', count: 6, iconUrl: traitIcons.darkStar,  tone: 'gold' as TraitHexBadgeTone, games: 28, avgPlace: 3.6 },
  { name: '정령족',    count: 4, iconUrl: traitIcons.spirit,    tone: 'gold' as TraitHexBadgeTone, games: 21, avgPlace: 3.9 },
  { name: '불한당',    count: 6, iconUrl: traitIcons.rogue,     tone: 'gold' as TraitHexBadgeTone, games: 18, avgPlace: 4.1 },
  { name: '별돌보미',  count: 4, iconUrl: traitIcons.stargazer, tone: 'gold' as TraitHexBadgeTone, games: 14, avgPlace: 4.3 },
]

/* ── 챔피언 이미지 (Community Dragon CDN) ── */
const curl = (p: string) => communityDragonAssetUrl(p)
const ci = {
  vex:        curl('ASSETS/Characters/TFT17_Vex/Skins/Base/Images/TFT17_vex_splash_tile_10.TFT_Set17.tex'),
  jhin:       curl('ASSETS/Characters/TFT17_Jhin/Skins/Base/Images/TFT17_Jhin_splash_tile_37.TFT_Set17.tex'),
  corki:      curl('ASSETS/Characters/TFT17_Corki/Skins/Base/Images/TFT17_Corki_splash_tile_26.TFT_Set17.tex'),
  masterYi:   curl('ASSETS/Characters/TFT17_MasterYi/Skins/Base/Images/TFT17_MasterYi_splash_tile_33.TFT_Set17.tex'),
  lulu:       curl('ASSETS/Characters/TFT17_Lulu/Skins/Base/Images/TFT17_Lulu_splash_tile_14.TFT_Set17.tex'),
  akali:      curl('ASSETS/Characters/TFT17_Akali/Skins/Base/Images/TFT17_Akali_splash_tile_68.TFT_Set17.tex'),
  illaoi:     curl('ASSETS/Characters/TFT17_Illaoi/Skins/Base/Images/TFT17_Illaoi_splash_tile_27.TFT_Set17.tex'),
  xayah:      curl('ASSETS/Characters/TFT17_Xayah/Skins/Base/Images/TFT17_Xayah_splash_tile_1.TFT_Set17.tex'),
  aurora:     curl('ASSETS/Characters/TFT17_Aurora/Skins/Base/Images/TFT17_Aurora_splash_tile_1.TFT_Set17.tex'),
  poppy:      curl('ASSETS/Characters/TFT17_Poppy/Skins/Base/Images/TFT17_Poppy_splash_tile_16.TFT_Set17.tex'),
  briar:      curl('ASSETS/Characters/TFT17_Briar/Skins/Base/Images/TFT17_Briar_splash_tile_10.TFT_Set17.tex'),
  kaisa:      curl('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  karma:      curl('ASSETS/Characters/TFT17_Karma/Skins/Base/Images/TFT17_Karma_splash_tile_8.TFT_Set17.tex'),
  morgana:    curl('ASSETS/Characters/TFT17_Morgana/Skins/Base/Images/TFT17_Morgana_splash_tile_50.TFT_Set17.tex'),
  belveth:    curl('ASSETS/Characters/TFT17_Belveth/Skins/Base/Images/TFT17_Belveth_splash_tile_19.TFT_Set17.tex'),
  pyke:       curl('ASSETS/Characters/TFT17_Pyke/Skins/Base/Images/TFT17_Pyke_splash_tile_25.TFT_Set17.tex'),
  bard:       curl('ASSETS/Characters/TFT17_Bard/Skins/Base/Images/TFT17_Bard_splash_tile_8.TFT_Set17.tex'),
  sona:       curl('ASSETS/Characters/TFT17_Sona/Skins/Base/Images/TFT17_Sona_splash_tile_17.TFT_Set17.tex'),
  gnar:       curl('ASSETS/Characters/TFT17_Gnar/Skins/Base/Images/TFT17_Gnar_splash_tile_15.TFT_Set17.tex'),
  shen:       curl('ASSETS/Characters/TFT17_Shen/Skins/Base/Images/TFT17_shen_splash_tile_49.TFT_Set17.tex'),
  zed:        curl('ASSETS/Characters/TFT17_Zed/Skins/Base/Images/TFT17_Zed_splash_tile_68.TFT_Set17.tex'),
  ornn:       curl('ASSETS/Characters/TFT17_Ornn/Skins/Base/Images/TFT17_Ornn_splash_tile_11.TFT_Set17.tex'),
  rammus:     curl('ASSETS/Characters/TFT17_Rammus/Skins/Base/Images/TFT17_Rammus_splash_tile_17.TFT_Set17.tex'),
  blitzcrank: curl('ASSETS/Characters/TFT17_Blitzcrank/Skins/Base/Images/TFT17_Blitzcrank_splash_tile_65.TFT_Set17.tex'),
  viktor:     curl('ASSETS/Characters/TFT17_Viktor/Skins/Base/Images/TFT17_Viktor_splash_tile_5.TFT_Set17.tex'),
  jinx:       curl('ASSETS/Characters/TFT17_Jinx/Skins/Base/Images/TFT17_Jinx_splash_tile_38.TFT_Set17.tex'),
  riven:      curl('ASSETS/Characters/TFT17_Riven/Skins/Base/Images/TFT17_Riven_splash_tile_18.TFT_Set17.tex'),
}

/* ── 아이템 이미지 (Guide.tsx와 동일한 경로 사용) ── */
const itUrl = (name: string) =>
  communityDragonAssetUrl(`ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_${name}.TFT_Set13.tex`)
const it = {
  rabadon:    itUrl('RabadonsDeathcap'),
  ie:         itUrl('InfinityEdge'),
  jeweled:    itUrl('JeweledGauntlet'),
  archangel:  itUrl('ArchangelsStaff'),
  blue:       itUrl('BlueBuff'),
  deathblade: itUrl('Bloodthirster'),
  warmog:     itUrl('WarmogsArmor'),
  titans:     itUrl('TitansResolve'),
  ionic:      itUrl('GuinsoosRageblade'),
  bramble:    itUrl('GargoyleStoneplate'),
  statikk:    itUrl('LastWhisper'),
  nashors:    itUrl('SpearOfShojin'),
  hand:       itUrl('UnstableConcoction'),
  dragon:     itUrl('DragonsClaw'),
  morello:    itUrl('Morellonomicon'),
}

const IT_NAME_MAP: Record<string, string> = {
  [it.rabadon]:    '라바돈의 죽음모자',
  [it.ie]:         '무한의 대검',
  [it.jeweled]:    '보석 건틀릿',
  [it.archangel]:  '대천사의 지팡이',
  [it.blue]:       '블루 버프',
  [it.deathblade]: '피의 갈증',
  [it.warmog]:     '웜풀의 갑옷',
  [it.titans]:     '타이탄의 결의',
  [it.ionic]:      '기뇨스의 분노검',
  [it.bramble]:    '가고일 석판',
  [it.statikk]:    '최후의 속삭임',
  [it.nashors]:    '쇼진의 창',
  [it.hand]:       '불안정한 혼합물',
  [it.dragon]:     '용의 발톱',
  [it.morello]:    '모렐로노미콘',
}

const DUMMY_TOP_CHAMPIONS = [
  { name: '벡스',      cost: 4, imageUrl: ci.vex,      games: 38, avgPlace: 3.1 },
  { name: '진',        cost: 4, imageUrl: ci.jhin,     games: 31, avgPlace: 3.4 },
  { name: '코르키',    cost: 3, imageUrl: ci.corki,    games: 26, avgPlace: 3.7 },
  { name: '마스터 이', cost: 4, imageUrl: ci.masterYi, games: 22, avgPlace: 3.9 },
  { name: '룰루',      cost: 3, imageUrl: ci.lulu,     games: 19, avgPlace: 4.0 },
  { name: '아칼리',    cost: 3, imageUrl: ci.akali,    games: 17, avgPlace: 4.2 },
]

type Unit = { name: string; imageUrl: string; stars: 1 | 2 | 3; items: string[] }
const u = (name: string, imageUrl: string, stars: 1 | 2 | 3, items: string[]): Unit => ({ name, imageUrl, stars, items })

/* ── 덱 구성 템플릿 8종 (30게임 순환 사용) ── */
const MATCH_CONFIGS = [
  { deckName: '선봉대 벡스', units: [
    u('벡스', ci.vex, 3, [it.rabadon, it.jeweled, it.archangel]),
    u('일라오이', ci.illaoi, 2, [it.warmog, it.titans]),
    u('자야', ci.xayah, 2, [it.ionic]),
    u('오로라', ci.aurora, 2, []),
    u('뽀삐', ci.poppy, 2, [it.bramble]),
    u('브라이어', ci.briar, 2, []),
  ]},
  { deckName: '6암흑의 별 진', units: [
    u('진', ci.jhin, 3, [it.ie, it.deathblade, it.jeweled]),
    u('카이사', ci.kaisa, 2, [it.statikk, it.nashors]),
    u('카르마', ci.karma, 2, [it.blue]),
    u('모르가나', ci.morgana, 2, [it.morello]),
    u('벨베스', ci.belveth, 2, []),
    u('파이크', ci.pyke, 2, [it.titans]),
  ]},
  { deckName: '정령족 코르키 백류', units: [
    u('코르키', ci.corki, 3, [it.blue, it.archangel, it.nashors]),
    u('벡스', ci.vex, 2, [it.rabadon]),
    u('오로라', ci.aurora, 2, []),
    u('바드', ci.bard, 2, [it.hand]),
    u('소나', ci.sona, 2, [it.warmog]),
    u('나르', ci.gnar, 2, []),
  ]},
  { deckName: '습격자 마스터 이', units: [
    u('마스터 이', ci.masterYi, 3, [it.ie, it.deathblade, it.hand]),
    u('파이크', ci.pyke, 2, [it.titans]),
    u('쉔', ci.shen, 2, [it.bramble, it.dragon]),
    u('제드', ci.zed, 2, [it.statikk]),
    u('벨베스', ci.belveth, 2, []),
    u('아칼리', ci.akali, 2, [it.jeweled]),
  ]},
  { deckName: '별돌보미 블루', units: [
    u('룰루', ci.lulu, 3, [it.blue, it.archangel, it.morello]),
    u('뽀삐', ci.poppy, 2, [it.warmog]),
    u('자야', ci.xayah, 2, [it.ionic, it.nashors]),
    u('바드', ci.bard, 2, [it.hand]),
    u('소나', ci.sona, 2, []),
    u('나르', ci.gnar, 2, [it.titans]),
  ]},
  { deckName: '8요새 웜풀', units: [
    u('람머스', ci.rammus, 3, [it.warmog, it.bramble, it.dragon]),
    u('뽀삐', ci.poppy, 2, [it.titans]),
    u('오른', ci.ornn, 2, [it.warmog]),
    u('블리츠', ci.blitzcrank, 2, [it.ionic]),
    u('소나', ci.sona, 2, [it.blue]),
    u('진', ci.jhin, 2, [it.ie]),
  ]},
  { deckName: '4그림자 암살자', units: [
    u('쉔', ci.shen, 3, [it.deathblade, it.ie, it.jeweled]),
    u('아칼리', ci.akali, 2, [it.statikk]),
    u('제드', ci.zed, 2, [it.titans]),
    u('파이크', ci.pyke, 2, [it.hand]),
    u('리븐', ci.riven, 2, []),
    u('진', ci.jhin, 2, [it.nashors]),
  ]},
  { deckName: '발명의 대가 빅토르', units: [
    u('빅토르', ci.viktor, 3, [it.rabadon, it.archangel, it.blue]),
    u('코르키', ci.corki, 2, [it.nashors]),
    u('징크스', ci.jinx, 2, [it.ie, it.deathblade]),
    u('소나', ci.sona, 2, [it.warmog]),
    u('모르가나', ci.morgana, 2, [it.morello]),
    u('파이크', ci.pyke, 2, []),
  ]},
]

/* ── 30게임 전적 생성 ── */
const LP_MAP: Record<number, number> = { 1: 22, 2: 13, 3: 8, 4: 5, 5: -9, 6: -14, 7: -19, 8: -26 }
const TIME_LABELS = [
  '방금 전',  '10분 전',  '30분 전',  '1시간 전', '2시간 전', '3시간 전',
  '5시간 전', '8시간 전', '12시간 전','어제',     '어제',     '2일 전',
  '2일 전',  '3일 전',   '3일 전',   '4일 전',   '4일 전',   '5일 전',
  '5일 전',  '6일 전',   '6일 전',   '7일 전',   '7일 전',   '8일 전',
  '8일 전',  '9일 전',   '9일 전',   '10일 전',  '10일 전',  '11일 전',
  '11일 전', '12일 전',  '12일 전',  '13일 전',  '13일 전',  '14일 전',
  '14일 전', '15일 전',  '15일 전',  '16일 전',  '16일 전',  '17일 전',
  '17일 전', '18일 전',  '18일 전',  '19일 전',  '19일 전',  '20일 전',
  '20일 전', '21일 전',  '21일 전',  '22일 전',  '22일 전',  '23일 전',
  '23일 전', '24일 전',  '24일 전',  '25일 전',  '25일 전',  '26일 전',
  '26일 전', '27일 전',  '27일 전',  '28일 전',  '28일 전',  '29일 전',
  '29일 전', '30일 전',  '30일 전',  '31일 전',  '31일 전',  '32일 전',
  '32일 전', '33일 전',  '33일 전',  '34일 전',  '34일 전',  '35일 전',
  '35일 전', '36일 전',  '36일 전',  '37일 전',  '37일 전',  '38일 전',
  '38일 전', '39일 전',  '39일 전',  '40일 전',  '40일 전',  '41일 전',
]

const DUMMY_MATCHES = ALL_PLACEMENTS.map((placement, i) => {
  const cfg = MATCH_CONFIGS[i % MATCH_CONFIGS.length]
  return { id: i + 1, placement, lpDelta: LP_MAP[placement], timeAgo: TIME_LABELS[i], deckName: cfg.deckName, units: cfg.units }
})

const COST_COLOR: Record<number, string> = { 1: '#808080', 2: '#3cb371', 3: '#4169e1', 4: '#9932cc', 5: '#ffd700' }

/* ── 매치 상세 타입 ── */
type MatchDetailPlayer = {
  rank: number; name: string; tag: string; stage: string
  traits: { iconUrl: string; count: number; name: string }[]
  augments: string[]; units: Unit[]; kills: number; gold: number; isMe?: boolean
}

const STAGE_BY_RANK = ['6-5', '6-3', '6-2', '5-6', '5-5', '5-3', '5-2', '4-6']

const FILLER_PLAYERS: Omit<MatchDetailPlayer, 'rank'>[] = [
  { name: 'Faker', tag: 'T1', stage: '6-3', kills: 18, gold: 3,
    traits: [{ iconUrl: traitIcons.darkStar, count: 6, name: '암흑의 별' }, { iconUrl: traitIcons.spirit, count: 4, name: '정령족' }],
    augments: ['⚡', '🌙', '💎'],
    units: [u('진', ci.jhin, 3, [it.ie, it.deathblade, it.jeweled]), u('카이사', ci.kaisa, 2, [it.statikk]), u('카르마', ci.karma, 2, [it.blue]), u('모르가나', ci.morgana, 2, [it.morello]), u('벨베스', ci.belveth, 2, []), u('파이크', ci.pyke, 2, [it.titans])],
  },
  { name: '마포이는 카...', tag: 'HAPPY', stage: '5-6', kills: 14, gold: 7,
    traits: [{ iconUrl: traitIcons.vanguard, count: 4, name: '선봉대' }, { iconUrl: traitIcons.spirit, count: 4, name: '정령족' }],
    augments: ['🔮', '🌿', '✨'],
    units: [u('코르키', ci.corki, 3, [it.blue, it.archangel, it.nashors]), u('벡스', ci.vex, 2, [it.rabadon]), u('오로라', ci.aurora, 2, []), u('바드', ci.bard, 2, [it.hand]), u('소나', ci.sona, 2, [it.warmog]), u('나르', ci.gnar, 2, [])],
  },
  { name: '서은지', tag: '봄봄하다가7', stage: '6-2', kills: 11, gold: 12,
    traits: [{ iconUrl: traitIcons.rogue, count: 6, name: '불한당' }, { iconUrl: traitIcons.darkStar, count: 4, name: '암흑의 별' }],
    augments: ['🎯', '💫', '🗡️'],
    units: [u('마스터 이', ci.masterYi, 3, [it.ie, it.deathblade, it.hand]), u('파이크', ci.pyke, 2, [it.titans]), u('쉔', ci.shen, 2, [it.bramble]), u('제드', ci.zed, 2, [it.statikk]), u('벨베스', ci.belveth, 2, []), u('아칼리', ci.akali, 2, [it.jeweled])],
  },
  { name: '산천어풍보대사', tag: '1234', stage: '5-5', kills: 9, gold: 8,
    traits: [{ iconUrl: traitIcons.stargazer, count: 4, name: '별돌보미' }, { iconUrl: traitIcons.spirit, count: 4, name: '정령족' }],
    augments: ['🔱', '⚗️', '🌀'],
    units: [u('룰루', ci.lulu, 3, [it.blue, it.archangel, it.morello]), u('뽀삐', ci.poppy, 2, [it.warmog]), u('자야', ci.xayah, 2, [it.ionic]), u('바드', ci.bard, 2, [it.hand]), u('소나', ci.sona, 2, []), u('나르', ci.gnar, 2, [it.titans])],
  },
  { name: '복조선특크...', tag: 'KR1', stage: '5-3', kills: 6, gold: 5,
    traits: [{ iconUrl: traitIcons.vanguard, count: 8, name: '선봉대' }],
    augments: ['🏆', '💥', '🌊'],
    units: [u('람머스', ci.rammus, 3, [it.warmog, it.bramble, it.dragon]), u('뽀삐', ci.poppy, 2, [it.titans]), u('오른', ci.ornn, 2, [it.warmog]), u('블리츠', ci.blitzcrank, 2, [it.ionic]), u('소나', ci.sona, 2, [it.blue]), u('진', ci.jhin, 2, [it.ie])],
  },
  { name: '판키', tag: '5597', stage: '5-2', kills: 4, gold: 11,
    traits: [{ iconUrl: traitIcons.rogue, count: 4, name: '불한당' }, { iconUrl: traitIcons.vanguard, count: 4, name: '선봉대' }],
    augments: ['🎭', '🔰', '☄️'],
    units: [u('쉔', ci.shen, 3, [it.deathblade, it.ie, it.jeweled]), u('아칼리', ci.akali, 2, [it.statikk]), u('제드', ci.zed, 2, [it.titans]), u('파이크', ci.pyke, 2, [it.hand]), u('리븐', ci.riven, 2, []), u('진', ci.jhin, 2, [it.nashors])],
  },
  { name: '뻐코영', tag: 'KR1', stage: '4-6', kills: 3, gold: 15,
    traits: [{ iconUrl: traitIcons.spirit, count: 6, name: '정령족' }],
    augments: ['🧿', '🎪', '🌸'],
    units: [u('빅토르', ci.viktor, 3, [it.rabadon, it.archangel, it.blue]), u('코르키', ci.corki, 2, [it.nashors]), u('징크스', ci.jinx, 2, [it.ie, it.deathblade]), u('소나', ci.sona, 2, [it.warmog]), u('모르가나', ci.morgana, 2, [it.morello]), u('파이크', ci.pyke, 2, [])],
  },
]

function buildMatchDetail(match: typeof DUMMY_MATCHES[0]): MatchDetailPlayer[] {
  const myRank = match.placement
  const myPlayer: MatchDetailPlayer = {
    rank: myRank, name: 'SanChess', tag: 'king', stage: STAGE_BY_RANK[myRank - 1],
    traits: [{ iconUrl: traitIcons.vanguard, count: 4, name: '선봉대' }, { iconUrl: traitIcons.darkStar, count: 6, name: '암흑의 별' }],
    augments: ['🛡️', '⚔️', '🔥'],
    units: match.units,
    kills: Math.max(3, 20 - myRank * 2),
    gold: (match.id * 3 + myRank) % 18 + 2,
    isMe: true,
  }
  const otherRanks = [1, 2, 3, 4, 5, 6, 7, 8].filter((r) => r !== myRank)
  const others: MatchDetailPlayer[] = FILLER_PLAYERS.map((p, i) => ({ ...p, rank: otherRanks[i] }))
  return [...others, myPlayer].sort((a, b) => a.rank - b.rank)
}

/* ── 30게임 요약 ── */
function RecentSummary() {
  const { games, top4, top4Rate, avgPlace } = DUMMY_SUMMARY
  const losses = games - top4
  return (
    <section className={styles.summarySection}>
      <div className={styles.winRateDonut} style={{ '--pct': `${top4Rate}%` } as React.CSSProperties}>
        <div className={styles.winRateInner}>
          <strong className={styles.winRatePct}>{top4Rate}%</strong>
        </div>
      </div>
      <div className={styles.summaryStats}>
        <p className={styles.summaryStatLabel}>순방 확률</p>
        <p className={styles.summaryStatValue}>
          {top4}W {losses}L <span className={styles.summaryStatSub}>({top4Rate}%)</span>
        </p>
        <p className={styles.summaryStatLabel}>평균 순위</p>
        <p className={styles.summaryStatValue}>
          {avgPlace}<span className={styles.summaryStatTh}>th</span> / 8
        </p>
      </div>
    </section>
  )
}

/* ── 매치 상세 패널 ── */
function MatchDetailPanel({ match }: { match: typeof DUMMY_MATCHES[0] }) {
  const players = buildMatchDetail(match)
  return (
    <div className={styles.matchDetailPanel}>
      <div className={styles.matchDetailHeader}>
        <span>#</span><span>소환사</span><span>스테이지</span><span>시너지</span><span>증강</span><span>챔피언</span><span>킬</span><span>잔여골드</span>
      </div>
      {players.map((player) => (
        <div key={player.rank} className={`${styles.matchDetailRow} ${player.isMe ? styles.myMatchDetailRow : ''}`}>
          <span className={styles.detailRank}>{player.rank}위</span>
          <div className={styles.detailPlayer}>
            <span className={styles.detailName}>{player.name}</span>
            <span className={styles.detailTag}>#{player.tag}</span>
          </div>
          <span className={styles.detailStage}>{player.stage}</span>
          <div className={styles.detailTraits}>
            {player.traits.map((tr) => (
              <div key={tr.name} className={styles.detailTraitBadge} title={tr.name}>
                <img src={tr.iconUrl} alt={tr.name} className={styles.detailTraitIcon} />
                <span>{tr.count}</span>
              </div>
            ))}
          </div>
          <div className={styles.detailAugments}>
            {player.augments.map((aug, i) => (
              <span key={i} className={styles.detailAugIcon}>{aug}</span>
            ))}
          </div>
          <div className={styles.detailUnits}>
            {player.units.map((unit) => (
              <div key={unit.name} className={`${styles.detailUnitWrap} ${unit.stars === 3 ? styles.detailUnitStar3 : ''} ${styles.tip}`} data-tip={unit.name}>
                <img className={styles.detailUnitImg} src={unit.imageUrl} alt={unit.name} />
                {unit.items.length > 0 && (
                  <div className={styles.detailUnitItems}>
                    {unit.items.slice(0, 3).map((item, ii) => (
                      <span key={ii} className={`${styles.itemTipWrap} ${styles.tip}`} data-tip={IT_NAME_MAP[item] ?? ''}>
                        <img className={styles.detailUnitItemImg} src={item} alt="" />
                      </span>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
          <span className={styles.detailKills}><Swords size={11} />{player.kills}</span>
          <span className={styles.detailGold}><Coins size={11} />{player.gold}</span>
        </div>
      ))}
    </div>
  )
}

function placementTone(n: number) {
  if (n === 1) return styles.gold
  if (n <= 4) return styles.top4
  return styles.bot4
}

/* ── 순위 분포 바 ── */
function RankDistribution() {
  const max = Math.max(...DUMMY_RANK_DIST)
  return (
    <div className={styles.rankDist}>
      {DUMMY_RANK_DIST.map((count, i) => (
        <div key={i} className={styles.rankDistItem}>
          <div className={styles.rankDistBarWrap}>
            <div className={`${styles.rankDistBar} ${i < 4 ? styles.top4Bar : styles.bot4Bar}`} style={{ height: `${Math.max(4, (count / max) * 56)}px` }} />
          </div>
          <span className={styles.rankDistNum}>{i + 1}</span>
          <span className={styles.rankDistCount}>{count}</span>
        </div>
      ))}
    </div>
  )
}

/* ── 많이 플레이한 시너지 ── */
function TopTraits() {
  return (
    <section className={styles.statSection}>
      <h2 className={styles.statSectionTitle}>많이 플레이한 시너지</h2>
      <div className={styles.topTraitList}>
        {DUMMY_TOP_TRAITS.map((tr, i) => (
          <div key={tr.name} className={styles.topTraitRow}>
            <span className={styles.topRank}>{i + 1}</span>
            <TraitHexBadge count={tr.count} iconUrl={tr.iconUrl} name={tr.name} tone={tr.tone} />
            <span className={styles.topName}>{tr.name}</span>
            <span className={styles.topGames}>{tr.games}게임</span>
            <span className={styles.topAvg}>평균 {tr.avgPlace}등</span>
          </div>
        ))}
      </div>
    </section>
  )
}

/* ── 많이 플레이한 챔피언 ── */
function TopChampions() {
  return (
    <section className={styles.statSection}>
      <h2 className={styles.statSectionTitle}>많이 플레이한 챔피언</h2>
      <div className={styles.topChampList}>
        {DUMMY_TOP_CHAMPIONS.map((champ, i) => (
          <div key={champ.name} className={styles.topChampRow}>
            <span className={styles.topRank}>{i + 1}</span>
            <div className={styles.champThumbWrap}>
              <img className={styles.champThumb} src={champ.imageUrl} alt={champ.name} />
              <span className={styles.champCost} style={{ background: COST_COLOR[champ.cost] }}>{champ.cost}</span>
            </div>
            <span className={styles.topName}>{champ.name}</span>
            <span className={styles.topGames}>{champ.games}게임</span>
            <span className={styles.topAvg}>평균 {champ.avgPlace}등</span>
          </div>
        ))}
      </div>
    </section>
  )
}

/* ── 메인 ── */
function SummonerDetail() {
  const { gameName, tagLine } = useParams<{ gameName: string; tagLine: string }>()
  const [query, setQuery] = useState('')
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [visibleCount, setVisibleCount] = useState(30)
  const navigate = useNavigate()

  const name = decodeURIComponent(gameName ?? '')
  const tag = tagLine ?? 'KR1'
  const winRate = Math.round(DUMMY_TIER.wins / (DUMMY_TIER.wins + DUMMY_TIER.losses) * 100)

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = query.trim()
    if (!trimmed) return
    const [n = trimmed, tg = 'KR1'] = trimmed.split('#')
    navigate(`/summoner/${encodeURIComponent(n)}/${tg}`)
    setQuery('')
  }

  return (
    <AppLayout>
      <div className={styles.page}>
        <form className={styles.topSearch} onSubmit={handleSearch}>
          <input placeholder="소환사명#태그 검색" value={query} onChange={(e) => setQuery(e.target.value)} />
          <button type="submit" aria-label="검색"><Search size={20} /></button>
        </form>

        <section className={styles.profileCard}>
          <div className={styles.profileIconWrap}>
            <img className={styles.profileIcon} src={communityDragonProfileIconUrl(DUMMY_TIER.profileIconId)} alt="프로필 아이콘" />
            <span className={styles.profileLevel}>{DUMMY_TIER.level}</span>
          </div>
          <div className={styles.emblem} />
          <div className={styles.profileInfo}>
            <h1>{name}<span className={styles.tag}>#{tag}</span></h1>
            <p className={styles.tierLine}>{DUMMY_TIER.tier} {DUMMY_TIER.division} · {DUMMY_TIER.lp} LP</p>
            <p className={styles.recordLine}>
              <span>{DUMMY_TIER.wins}승 {DUMMY_TIER.losses}패</span>
              <span className={styles.winRateText}>승률 {winRate}%</span>
              <span className={styles.avgPlaceText}>평균 {DUMMY_TIER.avgPlace}등</span>
              <span className={styles.top4Text}>TOP4 {DUMMY_TIER.top4Rate}%</span>
            </p>
          </div>
          <div className={styles.profileRight}>
            <button type="button" className={styles.updateBtn}><RefreshCcw size={16} />전적 업데이트</button>
            <RankDistribution />
          </div>
        </section>

        <div className={styles.statGrid}>
          <TopTraits />
          <TopChampions />
        </div>

        <section className={styles.matchSection}>
          <h2>최근 {DUMMY_SUMMARY.games}게임</h2>
          <RecentSummary />
          <div className={styles.matchList}>
            {DUMMY_MATCHES.slice(0, visibleCount).map((match) => {
              const isOpen = expandedId === match.id
              return (
                <div key={match.id} className={styles.matchItem}>
                  <article
                    className={`${styles.matchRow} ${placementTone(match.placement)} ${isOpen ? styles.matchRowOpen : ''}`}
                    onClick={() => setExpandedId(isOpen ? null : match.id)}
                  >
                    <div className={styles.placementBadge}>
                      <span>{match.placement}위</span>
                    </div>
                    <div className={styles.matchMeta}>
                      <p className={styles.deckName}>{match.deckName}</p>
                      <p className={styles.timeAgo}>{match.timeAgo}</p>
                    </div>
                    <div className={styles.unitList}>
                      {match.units.map((unit) => (
                        <div key={unit.name} className={`${styles.unitWrap} ${unit.stars === 3 ? styles.unitStar3 : ''} ${styles.tip}`} data-tip={unit.name}>
                          <img className={styles.unitImg} src={unit.imageUrl} alt={unit.name} />
                          {unit.items.length > 0 && (
                            <div className={styles.unitItemRow}>
                              {unit.items.slice(0, 3).map((item, ii) => (
                                <span key={ii} className={`${styles.itemTipWrap} ${styles.tip}`} data-tip={IT_NAME_MAP[item] ?? ''}>
                                  <img className={styles.unitItemImg} src={item} alt="" />
                                </span>
                              ))}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                    <div className={`${styles.lpDelta} ${match.lpDelta >= 0 ? styles.lpGain : styles.lpLoss}`}>
                      <span>{match.lpDelta >= 0 ? '+' : ''}{match.lpDelta} LP</span>
                      {isOpen ? <ChevronUp size={14} className={styles.chevron} /> : <ChevronDown size={14} className={styles.chevron} />}
                    </div>
                  </article>
                  {isOpen && <MatchDetailPanel match={match} />}
                </div>
              )
            })}
          </div>
          {visibleCount < DUMMY_MATCHES.length && (
            <button
              type="button"
              className={styles.loadMoreBtn}
              onClick={() => setVisibleCount((v) => v + 30)}
            >
              30개 더 보기 ({DUMMY_MATCHES.length - visibleCount}개 남음)
            </button>
          )}
        </section>
      </div>
    </AppLayout>
  )
}

export default SummonerDetail
