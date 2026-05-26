/**
 * 덱별 · 레벨별 추천 배치 포지션 + 플렉스 픽
 *
 * - DECK_POSITIONS : 레벨별 핵심 6인 포지션 (5~6레벨 → 폴백 없이 사용)
 * - FLEX_PICKS     : 레벨 7~9에서 추가되는 플렉스 유닛 (이미지 포함)
 *
 * row 0 = 앞줄(탱커), row 3 = 뒷줄(캐리)  /  col 0~6
 * 레벨 미정의 시 가장 가까운 낮은 레벨로 폴백
 * ※ 실제 서비스 시 백엔드 API로 교체 예정
 */

import { communityDragonAssetUrl } from '../../api/communityDragonAssets'

/* ── champion image URLs (shared) ── */
const C = {
  akali:       communityDragonAssetUrl('ASSETS/Characters/TFT17_Akali/Skins/Base/Images/TFT17_Akali_splash_tile_68.TFT_Set17.tex'),
  aurelionSol: communityDragonAssetUrl('ASSETS/Characters/TFT17_AurelionSol/Skins/Base/Images/TFT17_AurelionSol_splash_tile_2.TFT_Set17.tex'),
  aurora:      communityDragonAssetUrl('ASSETS/Characters/TFT17_Aurora/Skins/Base/Images/TFT17_Aurora_splash_tile_1.TFT_Set17.tex'),
  bard:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Bard/Skins/Base/Images/TFT17_Bard_splash_tile_8.TFT_Set17.tex'),
  belveth:     communityDragonAssetUrl('ASSETS/Characters/TFT17_Belveth/Skins/Base/Images/TFT17_Belveth_splash_tile_19.TFT_Set17.tex'),
  blitzcrank:  communityDragonAssetUrl('ASSETS/Characters/TFT17_Blitzcrank/Skins/Base/Images/TFT17_Blitzcrank_splash_tile_65.TFT_Set17.tex'),
  corki:       communityDragonAssetUrl('ASSETS/Characters/TFT17_Corki/Skins/Base/Images/TFT17_Corki_splash_tile_26.TFT_Set17.tex'),
  gnar:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Gnar/Skins/Base/Images/TFT17_Gnar_splash_tile_15.TFT_Set17.tex'),
  jax:         communityDragonAssetUrl('ASSETS/Characters/TFT17_Jax/Skins/Base/Images/TFT17_Jax_Mobile.TFT_Set17.tex'),
  jhin:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Jhin/Skins/Base/Images/TFT17_Jhin_splash_tile_37.TFT_Set17.tex'),
  jinx:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Jinx/Skins/Base/Images/TFT17_Jinx_splash_tile_38.TFT_Set17.tex'),
  kaisa:       communityDragonAssetUrl('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  karma:       communityDragonAssetUrl('ASSETS/Characters/TFT17_Karma/Skins/Base/Images/TFT17_Karma_splash_tile_8.TFT_Set17.tex'),
  lulu:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Lulu/Skins/Base/Images/TFT17_Lulu_splash_tile_14.TFT_Set17.tex'),
  masterYi:    communityDragonAssetUrl('ASSETS/Characters/TFT17_MasterYi/Skins/Base/Images/TFT17_MasterYi_splash_tile_33.TFT_Set17.tex'),
  morgana:     communityDragonAssetUrl('ASSETS/Characters/TFT17_Morgana/Skins/Base/Images/TFT17_Morgana_splash_tile_50.TFT_Set17.tex'),
  nami:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Nami/Skins/Base/Images/TFT17_Nami_splash_tile_41.TFT_Set17.tex'),
  ornn:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Ornn/Skins/Base/Images/TFT17_Ornn_splash_tile_11.TFT_Set17.tex'),
  poppy:       communityDragonAssetUrl('ASSETS/Characters/TFT17_Poppy/Skins/Base/Images/TFT17_Poppy_splash_tile_16.TFT_Set17.tex'),
  pyke:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Pyke/Skins/Base/Images/TFT17_Pyke_splash_tile_25.TFT_Set17.tex'),
  riven:       communityDragonAssetUrl('ASSETS/Characters/TFT17_Riven/Skins/Base/Images/TFT17_Riven_splash_tile_18.TFT_Set17.tex'),
  shen:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Shen/Skins/Base/Images/TFT17_shen_splash_tile_49.TFT_Set17.tex'),
  sona:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Sona/Skins/Base/Images/TFT17_Sona_splash_tile_17.TFT_Set17.tex'),
  twistedFate: communityDragonAssetUrl('ASSETS/Characters/TFT17_TwistedFate/Skins/Base/Images/TFT17_TwistedFate_splash_tile_45.TFT_Set17.tex'),
  vex:         communityDragonAssetUrl('ASSETS/Characters/TFT17_Vex/Skins/Base/Images/TFT17_vex_splash_tile_10.TFT_Set17.tex'),
  viktor:      communityDragonAssetUrl('ASSETS/Characters/TFT17_Viktor/Skins/Base/Images/TFT17_Viktor_splash_tile_5.TFT_Set17.tex'),
  xayah:       communityDragonAssetUrl('ASSETS/Characters/TFT17_Xayah/Skins/Base/Images/TFT17_Xayah_splash_tile_1.TFT_Set17.tex'),
  zed:         communityDragonAssetUrl('ASSETS/Characters/TFT17_Zed/Skins/Base/Images/TFT17_Zed_splash_tile_68.TFT_Set17.tex'),
}

/* ════════════════════════════
   핵심 포지션 데이터 (Lv.5~6)
════════════════════════════ */
type LevelMap = Partial<Record<number, Record<string, [number, number]>>>

export const DECK_POSITIONS: Record<number, LevelMap> = {
  1: { // 선봉대 벡스
    5: { '뽀삐': [0,2], '일라오이': [0,3], '오로라': [3,2], '벡스': [3,3], '자야': [3,4] },
    6: { '뽀삐': [0,2], '일라오이': [0,3], '브라이어': [0,4], '오로라': [3,2], '벡스': [3,3], '자야': [3,4] },
  },
  2: { // 6암흑의 별 진
    5: { '파이크': [0,2], '벨베스': [0,4], '카이사': [2,5], '모르가나': [3,2], '진': [3,4] },
    6: { '파이크': [0,2], '벨베스': [0,4], '카르마': [1,1], '카이사': [2,5], '모르가나': [3,2], '진': [3,4] },
  },
  3: { // 정령족 코르키 백류
    5: { '소나': [1,2], '오로라': [2,2], '벡스': [2,4], '바드': [3,1], '코르키': [3,4] },
    6: { '나르': [0,3], '소나': [1,2], '오로라': [2,2], '벡스': [2,4], '바드': [3,1], '코르키': [3,4] },
  },
  4: { // 습격자 마스터 이
    5: { '쉔': [0,2], '벨베스': [0,4], '파이크': [1,1], '제드': [2,3], '마스터 이': [3,3] },
    6: { '쉔': [0,2], '벨베스': [0,4], '파이크': [1,1], '아칼리': [1,5], '제드': [2,3], '마스터 이': [3,3] },
  },
  5: { // 별돌보미 룰루
    5: { '뽀삐': [0,2], '잭스': [0,4], '나미': [1,1], '트페': [3,2], '룰루': [3,4] },
    6: { '뽀삐': [0,2], '잭스': [0,4], '나미': [1,1], '자야': [2,5], '트페': [3,2], '룰루': [3,4] },
  },
  6: { // 8요새 럼블
    5: { '람머스': [0,1], '뽀삐': [0,3], '블리츠': [1,2], '나미': [1,4], '아우솔': [3,3] },
    6: { '람머스': [0,1], '뽀삐': [0,3], '오른': [0,5], '블리츠': [1,2], '나미': [1,4], '아우솔': [3,3] },
  },
  7: { // 4그림자 암살자
    5: { '쉔': [0,3], '아칼리': [2,2], '제드': [2,4], '파이크': [3,3], '진': [3,5] },
    6: { '쉔': [0,3], '리븐': [0,5], '아칼리': [2,2], '제드': [2,4], '파이크': [3,3], '진': [3,5] },
  },
  8: { // 발명의 대가 하이머딩거
    5: { '나미': [1,1], '코르키': [2,2], '아우솔': [2,4], '징크스': [3,2], '빅토르': [3,4] },
    6: { '나미': [1,1], '소나': [1,5], '코르키': [2,2], '아우솔': [2,4], '징크스': [3,2], '빅토르': [3,4] },
  },
  9: { // 4저격수 징크스
    5: { '뽀삐': [0,2], '나미': [1,1], '진': [2,2], '카이사': [2,4], '징크스': [3,3] },
    6: { '뽀삐': [0,2], '모르가나': [0,4], '나미': [1,1], '진': [2,2], '카이사': [2,4], '징크스': [3,3] },
  },
  10: { // 복제자 빅토르
    5: { '오른': [0,2], '블리츠': [0,4], '소나': [1,1], '코르키': [2,3], '빅토르': [3,3] },
    6: { '오른': [0,2], '블리츠': [0,4], '소나': [1,1], '코르키': [2,3], '아우솔': [2,5], '빅토르': [3,3] },
  },
  11: { // 우주그루브 소나
    5: { '뽀삐': [0,2], '룰루': [1,1], '나미': [1,3], '바드': [1,5], '소나': [3,3] },
    6: { '뽀삐': [0,2], '나르': [0,4], '룰루': [1,1], '나미': [1,3], '바드': [1,5], '소나': [3,3] },
  },
  12: { // 운명술사 트페
    5: { '뽀삐': [0,2], '나미': [1,1], '오로라': [2,3], '바드': [3,2], '트페': [3,4] },
    6: { '뽀삐': [0,2], '나르': [0,4], '나미': [1,1], '오로라': [2,3], '바드': [3,2], '트페': [3,4] },
  },
  13: { // 6도전자 자야
    5: { '잭스': [0,2], '아칼리': [1,4], '제드': [2,3], '자야': [3,3], '진': [3,5] },
    6: { '잭스': [0,2], '아칼리': [1,4], '제드': [2,3], '카이사': [3,1], '자야': [3,3], '진': [3,5] },
  },
  14: { // 길잡이 나미
    5: { '뽀삐': [0,3], '소나': [1,2], '나미': [3,2], '바드': [3,3], '오로라': [3,4] },
    6: { '뽀삐': [0,3], '소나': [1,2], '룰루': [1,4], '나미': [3,2], '바드': [3,3], '오로라': [3,4] },
  },
}

/* ════════════════════════════
   플렉스 픽 데이터 (Lv.7~9)
════════════════════════════ */
export interface FlexPick {
  name: string
  imageUrl: string
  stars: 2 | 3
  position: [number, number]
}

/**
 * 각 덱의 레벨 7 · 8 · 9 플렉스 픽
 * - Lv.7: +1유닛 (총 7)
 * - Lv.8: +2유닛 (총 8)
 * - Lv.9: +3유닛 (총 9)
 * - Lv.10: Lv.9와 동일 (폴백)
 */
export const FLEX_PICKS: Record<number, Partial<Record<number, FlexPick[]>>> = {
  1: { // 선봉대 벡스
    7: [
      { name: '나미',  imageUrl: C.nami, stars: 2, position: [1, 3] },
    ],
    8: [
      { name: '나미',  imageUrl: C.nami, stars: 2, position: [1, 3] },
      { name: '바드',  imageUrl: C.bard, stars: 2, position: [1, 5] },
    ],
    9: [
      { name: '나미',  imageUrl: C.nami,  stars: 2, position: [1, 3] },
      { name: '바드',  imageUrl: C.bard,  stars: 2, position: [1, 5] },
      { name: '소나',  imageUrl: C.sona,  stars: 3, position: [2, 3] },
    ],
  },
  2: { // 6암흑의 별 진
    7: [
      { name: '아칼리', imageUrl: C.akali, stars: 2, position: [2, 3] },
    ],
    8: [
      { name: '아칼리', imageUrl: C.akali, stars: 2, position: [2, 3] },
      { name: '제드',   imageUrl: C.zed,   stars: 2, position: [2, 1] },
    ],
    9: [
      { name: '아칼리', imageUrl: C.akali, stars: 2, position: [2, 3] },
      { name: '제드',   imageUrl: C.zed,   stars: 2, position: [2, 1] },
      { name: '리븐',   imageUrl: C.riven, stars: 2, position: [0, 6] },
    ],
  },
  3: { // 정령족 코르키 백류
    7: [
      { name: '룰루', imageUrl: C.lulu, stars: 2, position: [1, 4] },
    ],
    8: [
      { name: '룰루', imageUrl: C.lulu, stars: 2, position: [1, 4] },
      { name: '나미', imageUrl: C.nami, stars: 2, position: [0, 1] },
    ],
    9: [
      { name: '룰루', imageUrl: C.lulu,  stars: 2, position: [1, 4] },
      { name: '나미', imageUrl: C.nami,  stars: 2, position: [0, 1] },
      { name: '뽀삐', imageUrl: C.poppy, stars: 2, position: [0, 5] },
    ],
  },
  4: { // 습격자 마스터 이
    7: [
      { name: '리븐', imageUrl: C.riven, stars: 2, position: [0, 6] },
    ],
    8: [
      { name: '리븐',  imageUrl: C.riven, stars: 2, position: [0, 6] },
      { name: '카이사', imageUrl: C.kaisa, stars: 2, position: [3, 1] },
    ],
    9: [
      { name: '리븐',  imageUrl: C.riven, stars: 2, position: [0, 6] },
      { name: '카이사', imageUrl: C.kaisa, stars: 2, position: [3, 1] },
      { name: '진',    imageUrl: C.jhin,  stars: 3, position: [3, 5] },
    ],
  },
  5: { // 별돌보미 룰루
    7: [
      { name: '바드', imageUrl: C.bard, stars: 2, position: [1, 3] },
    ],
    8: [
      { name: '바드', imageUrl: C.bard, stars: 2, position: [1, 3] },
      { name: '소나', imageUrl: C.sona, stars: 2, position: [1, 5] },
    ],
    9: [
      { name: '바드',  imageUrl: C.bard,   stars: 2, position: [1, 3] },
      { name: '소나',  imageUrl: C.sona,   stars: 2, position: [1, 5] },
      { name: '오로라', imageUrl: C.aurora, stars: 3, position: [2, 3] },
    ],
  },
  6: { // 8요새 럼블
    7: [
      { name: '빅토르', imageUrl: C.viktor, stars: 2, position: [3, 5] },
    ],
    8: [
      { name: '빅토르', imageUrl: C.viktor, stars: 2, position: [3, 5] },
      { name: '징크스', imageUrl: C.jinx,   stars: 2, position: [3, 1] },
    ],
    9: [
      { name: '빅토르', imageUrl: C.viktor, stars: 2, position: [3, 5] },
      { name: '징크스', imageUrl: C.jinx,   stars: 2, position: [3, 1] },
      { name: '소나',   imageUrl: C.sona,   stars: 2, position: [1, 6] },
    ],
  },
  7: { // 4그림자 암살자
    7: [
      { name: '마스터 이', imageUrl: C.masterYi, stars: 2, position: [3, 1] },
    ],
    8: [
      { name: '마스터 이', imageUrl: C.masterYi, stars: 2, position: [3, 1] },
      { name: '카이사',    imageUrl: C.kaisa,     stars: 2, position: [3, 6] },
    ],
    9: [
      { name: '마스터 이', imageUrl: C.masterYi, stars: 2, position: [3, 1] },
      { name: '카이사',    imageUrl: C.kaisa,     stars: 2, position: [3, 6] },
      { name: '벨베스',    imageUrl: C.belveth,   stars: 2, position: [0, 1] },
    ],
  },
  8: { // 발명의 대가 하이머딩거
    7: [
      { name: '오른', imageUrl: C.ornn, stars: 2, position: [0, 3] },
    ],
    8: [
      { name: '오른',   imageUrl: C.ornn,       stars: 2, position: [0, 3] },
      { name: '블리츠', imageUrl: C.blitzcrank, stars: 2, position: [0, 5] },
    ],
    9: [
      { name: '오른',   imageUrl: C.ornn,       stars: 2, position: [0, 3] },
      { name: '블리츠', imageUrl: C.blitzcrank, stars: 2, position: [0, 5] },
      { name: '뽀삐',   imageUrl: C.poppy,      stars: 2, position: [0, 1] },
    ],
  },
  9: { // 4저격수 징크스
    7: [
      { name: '자야', imageUrl: C.xayah, stars: 2, position: [3, 1] },
    ],
    8: [
      { name: '자야', imageUrl: C.xayah, stars: 2, position: [3, 1] },
      { name: '잭스', imageUrl: C.jax,   stars: 2, position: [0, 6] },
    ],
    9: [
      { name: '자야', imageUrl: C.xayah, stars: 2, position: [3, 1] },
      { name: '잭스', imageUrl: C.jax,   stars: 2, position: [0, 6] },
      { name: '바드', imageUrl: C.bard,  stars: 2, position: [1, 3] },
    ],
  },
  10: { // 복제자 빅토르
    7: [
      { name: '나미', imageUrl: C.nami, stars: 2, position: [1, 3] },
    ],
    8: [
      { name: '나미',  imageUrl: C.nami,  stars: 2, position: [1, 3] },
      { name: '카르마', imageUrl: C.karma, stars: 2, position: [1, 5] },
    ],
    9: [
      { name: '나미',  imageUrl: C.nami,  stars: 2, position: [1, 3] },
      { name: '카르마', imageUrl: C.karma, stars: 2, position: [1, 5] },
      { name: '징크스', imageUrl: C.jinx,  stars: 2, position: [3, 1] },
    ],
  },
  11: { // 우주그루브 소나
    7: [
      { name: '오로라', imageUrl: C.aurora, stars: 2, position: [2, 2] },
    ],
    8: [
      { name: '오로라', imageUrl: C.aurora, stars: 2, position: [2, 2] },
      { name: '벡스',   imageUrl: C.vex,    stars: 2, position: [2, 4] },
    ],
    9: [
      { name: '오로라', imageUrl: C.aurora, stars: 2, position: [2, 2] },
      { name: '벡스',   imageUrl: C.vex,    stars: 2, position: [2, 4] },
      { name: '잭스',   imageUrl: C.jax,    stars: 2, position: [0, 6] },
    ],
  },
  12: { // 운명술사 트페
    7: [
      { name: '소나', imageUrl: C.sona, stars: 2, position: [1, 3] },
    ],
    8: [
      { name: '소나', imageUrl: C.sona, stars: 2, position: [1, 3] },
      { name: '룰루', imageUrl: C.lulu, stars: 2, position: [1, 5] },
    ],
    9: [
      { name: '소나', imageUrl: C.sona, stars: 2, position: [1, 3] },
      { name: '룰루', imageUrl: C.lulu, stars: 2, position: [1, 5] },
      { name: '벡스', imageUrl: C.vex,  stars: 2, position: [2, 5] },
    ],
  },
  13: { // 6도전자 자야
    7: [
      { name: '벨베스', imageUrl: C.belveth, stars: 2, position: [0, 4] },
    ],
    8: [
      { name: '벨베스', imageUrl: C.belveth, stars: 2, position: [0, 4] },
      { name: '리븐',   imageUrl: C.riven,   stars: 2, position: [0, 6] },
    ],
    9: [
      { name: '벨베스',   imageUrl: C.belveth,  stars: 2, position: [0, 4] },
      { name: '리븐',     imageUrl: C.riven,    stars: 2, position: [0, 6] },
      { name: '마스터 이', imageUrl: C.masterYi, stars: 2, position: [2, 5] },
    ],
  },
  14: { // 길잡이 나미
    7: [
      { name: '나르', imageUrl: C.gnar, stars: 2, position: [0, 1] },
    ],
    8: [
      { name: '나르', imageUrl: C.gnar, stars: 2, position: [0, 1] },
      { name: '벡스', imageUrl: C.vex,  stars: 2, position: [2, 5] },
    ],
    9: [
      { name: '나르', imageUrl: C.gnar,        stars: 2, position: [0, 1] },
      { name: '벡스', imageUrl: C.vex,         stars: 2, position: [2, 5] },
      { name: '트페', imageUrl: C.twistedFate, stars: 3, position: [3, 6] },
    ],
  },
}

/* ════════════════════════════
   헬퍼 함수
════════════════════════════ */

/** 해당 덱·레벨의 핵심 포지션 맵 반환 (낮은 레벨 폴백) */
export function getPositionsForLevel(
  deckRank: number,
  level: number,
): Record<string, [number, number]> {
  const deckPos = DECK_POSITIONS[deckRank]
  if (!deckPos) return {}
  for (let l = level; l >= 5; l--) {
    if (deckPos[l]) return deckPos[l]!
  }
  return {}
}

/** 해당 덱·레벨의 플렉스 픽 목록 반환 (낮은 레벨 폴백, 5~6레벨은 없음) */
export function getFlexPicksForLevel(
  deckRank: number,
  level: number,
): FlexPick[] {
  if (level < 7) return []
  const deckFlex = FLEX_PICKS[deckRank]
  if (!deckFlex) return []
  // Lv.10 → Lv.9 폴백, 그 이하도 동일 로직
  for (let l = Math.min(level, 9); l >= 7; l--) {
    if (deckFlex[l]) return deckFlex[l]!
  }
  return []
}
