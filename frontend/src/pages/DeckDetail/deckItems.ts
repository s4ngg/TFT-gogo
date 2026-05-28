/**
 * 덱별 추천 아이템 (삼신기 + 슬롯별 대체 아이템)
 * ※ 실제 서비스 시 백엔드 API로 교체 예정
 */
import { communityDragonAssetUrl } from '../../api/communityDragonAssets'

export interface ItemRec {
  name: string
  imageUrl: string
}

export interface ChampionItemRec {
  /** deck.champions 에서 이미지를 조회할 때 사용 */
  champName: string
  stars: 2 | 3
  /** 추천 삼신기 */
  coreItems: [ItemRec, ItemRec, ItemRec]
  /** 슬롯별 대체 아이템 (3슬롯 × n개) */
  alternatives: [ItemRec[], ItemRec[], ItemRec[]]
}

/* ── 아이템 이미지 ── */
const I: Record<string, ItemRec> = {
  guinsoos:   { name: '구인수의 격노검',    imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GuinsoosRageblade.TFT_Set13.tex') },
  ie:         { name: '무한의 대검',        imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_InfinityEdge.TFT_Set13.tex') },
  lw:         { name: '최후의 속삭임',      imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_LastWhisper.TFT_Set13.tex') },
  bt:         { name: '피바라기',           imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_Bloodthirster.TFT_Set13.tex') },
  bb:         { name: '푸른 파수꾼',        imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_BlueBuff.TFT_Set13.tex') },
  jg:         { name: '보석 건틀릿',        imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_JeweledGauntlet.TFT_Set13.tex') },
  rdc:        { name: '라바돈의 죽음모자',  imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_RabadonsDeathcap.TFT_Set13.tex') },
  archangel:  { name: '대천사의 지팡이',    imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_ArchangelsStaff.TFT_Set13.tex') },
  shojin:     { name: '쇼진의 창',          imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_SpearOfShojin.TFT_Set13.tex') },
  morellos:   { name: '모렐로노미콘',       imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_Morellonomicon.TFT_Set13.tex') },
  warmogs:    { name: '워모그의 갑옷',      imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_WarmogsArmor.TFT_Set13.tex') },
  gargoyle:   { name: '가고일 돌갑옷',      imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GargoyleStoneplate.TFT_Set13.tex') },
  dclaw:      { name: '용의 발톱',          imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_DragonsClaw.TFT_Set13.tex') },
  steraks:    { name: '스테락의 도전',      imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_SteraksGage.TFT_Set13.tex') },
  titans:     { name: '거인의 결의',        imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_TitansResolve.TFT_Set13.tex') },
  hoj:        { name: '정의의 손길',        imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_UnstableConcoction.TFT_Set13.tex') },
  ionic:      { name: '이온 충격기',        imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_IonicSpark.TFT_Set13.tex') },
  crownguard: { name: '크라운가드',         imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_Crownguard.TFT_Set13.tex') },
  adaptive:   { name: '적응형 투구',        imageUrl: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_AdaptiveHelm.TFT_Set13.tex') },
}

export const DECK_ITEMS: Record<number, ChampionItemRec[]> = {
  1: [ // 선봉대 벡스
    {
      champName: '오로라', stars: 3,
      coreItems: [I.bb, I.jg, I.rdc],
      alternatives: [
        [I.archangel, I.shojin],
        [I.morellos, I.crownguard],
        [I.ionic, I.adaptive],
      ],
    },
    {
      champName: '뽀삐', stars: 2,
      coreItems: [I.warmogs, I.gargoyle, I.dclaw],
      alternatives: [
        [I.steraks, I.titans],
        [I.titans, I.steraks],
        [I.adaptive, I.ionic],
      ],
    },
  ],
  2: [ // 6암흑의 별 진
    {
      champName: '진', stars: 3,
      coreItems: [I.guinsoos, I.ie, I.lw],
      alternatives: [
        [I.bt, I.hoj],
        [I.hoj, I.jg],
        [I.bt, I.guinsoos],
      ],
    },
    {
      champName: '모르가나', stars: 3,
      coreItems: [I.bb, I.jg, I.rdc],
      alternatives: [
        [I.archangel, I.shojin],
        [I.morellos, I.crownguard],
        [I.ionic, I.morellos],
      ],
    },
  ],
  3: [ // 정령족 코르키 백류
    {
      champName: '바드', stars: 3,
      coreItems: [I.shojin, I.archangel, I.morellos],
      alternatives: [
        [I.bb, I.ionic],
        [I.rdc, I.jg],
        [I.adaptive, I.crownguard],
      ],
    },
    {
      champName: '소나', stars: 2,
      coreItems: [I.bb, I.morellos, I.jg],
      alternatives: [
        [I.archangel, I.shojin],
        [I.ionic, I.adaptive],
        [I.rdc, I.crownguard],
      ],
    },
  ],
  4: [ // 습격자 마스터 이
    {
      champName: '제드', stars: 3,
      coreItems: [I.guinsoos, I.ie, I.lw],
      alternatives: [
        [I.bt, I.hoj],
        [I.jg, I.hoj],
        [I.bt, I.guinsoos],
      ],
    },
    {
      champName: '아칼리', stars: 2,
      coreItems: [I.ie, I.hoj, I.bt],
      alternatives: [
        [I.guinsoos, I.lw],
        [I.jg, I.crownguard],
        [I.steraks, I.titans],
      ],
    },
  ],
  5: [ // 별돌보미 룰루
    {
      champName: '트페', stars: 3,
      coreItems: [I.bb, I.jg, I.rdc],
      alternatives: [
        [I.archangel, I.shojin],
        [I.morellos, I.ionic],
        [I.archangel, I.crownguard],
      ],
    },
    {
      champName: '자야', stars: 2,
      coreItems: [I.guinsoos, I.ie, I.lw],
      alternatives: [
        [I.bt, I.hoj],
        [I.hoj, I.bt],
        [I.guinsoos, I.bt],
      ],
    },
  ],
  6: [ // 8요새 럼블
    {
      champName: '아우솔', stars: 3,
      coreItems: [I.shojin, I.archangel, I.morellos],
      alternatives: [
        [I.bb, I.ionic],
        [I.rdc, I.jg],
        [I.adaptive, I.ionic],
      ],
    },
    {
      champName: '블리츠', stars: 2,
      coreItems: [I.warmogs, I.gargoyle, I.dclaw],
      alternatives: [
        [I.steraks, I.titans],
        [I.titans, I.steraks],
        [I.adaptive, I.ionic],
      ],
    },
  ],
  7: [ // 4그림자 암살자
    {
      champName: '파이크', stars: 3,
      coreItems: [I.ie, I.hoj, I.bt],
      alternatives: [
        [I.guinsoos, I.lw],
        [I.jg, I.guinsoos],
        [I.steraks, I.titans],
      ],
    },
    {
      champName: '진', stars: 2,
      coreItems: [I.guinsoos, I.ie, I.lw],
      alternatives: [
        [I.bt, I.hoj],
        [I.jg, I.hoj],
        [I.bt, I.guinsoos],
      ],
    },
  ],
  8: [ // 발명의 대가 하이머딩거
    {
      champName: '징크스', stars: 3,
      coreItems: [I.guinsoos, I.ie, I.lw],
      alternatives: [
        [I.bt, I.hoj],
        [I.jg, I.hoj],
        [I.bt, I.guinsoos],
      ],
    },
    {
      champName: '소나', stars: 2,
      coreItems: [I.bb, I.morellos, I.jg],
      alternatives: [
        [I.archangel, I.shojin],
        [I.ionic, I.crownguard],
        [I.rdc, I.crownguard],
      ],
    },
  ],
  9: [ // 4저격수 징크스
    {
      champName: '징크스', stars: 3,
      coreItems: [I.guinsoos, I.ie, I.lw],
      alternatives: [
        [I.bt, I.hoj],
        [I.jg, I.hoj],
        [I.bt, I.guinsoos],
      ],
    },
    {
      champName: '나미', stars: 2,
      coreItems: [I.shojin, I.morellos, I.adaptive],
      alternatives: [
        [I.bb, I.ionic],
        [I.ionic, I.crownguard],
        [I.crownguard, I.titans],
      ],
    },
  ],
  10: [ // 복제자 빅토르
    {
      champName: '빅토르', stars: 3,
      coreItems: [I.shojin, I.archangel, I.morellos],
      alternatives: [
        [I.bb, I.ionic],
        [I.rdc, I.jg],
        [I.crownguard, I.adaptive],
      ],
    },
    {
      champName: '오른', stars: 2,
      coreItems: [I.warmogs, I.gargoyle, I.dclaw],
      alternatives: [
        [I.steraks, I.titans],
        [I.titans, I.steraks],
        [I.adaptive, I.ionic],
      ],
    },
  ],
  11: [ // 우주그루브 소나
    {
      champName: '소나', stars: 3,
      coreItems: [I.bb, I.morellos, I.jg],
      alternatives: [
        [I.archangel, I.shojin],
        [I.ionic, I.crownguard],
        [I.rdc, I.crownguard],
      ],
    },
    {
      champName: '뽀삐', stars: 2,
      coreItems: [I.warmogs, I.gargoyle, I.dclaw],
      alternatives: [
        [I.steraks, I.titans],
        [I.titans, I.steraks],
        [I.adaptive, I.ionic],
      ],
    },
  ],
  12: [ // 운명술사 트페
    {
      champName: '트페', stars: 3,
      coreItems: [I.bb, I.jg, I.rdc],
      alternatives: [
        [I.archangel, I.shojin],
        [I.morellos, I.crownguard],
        [I.crownguard, I.ionic],
      ],
    },
    {
      champName: '나르', stars: 2,
      coreItems: [I.warmogs, I.gargoyle, I.dclaw],
      alternatives: [
        [I.steraks, I.titans],
        [I.titans, I.steraks],
        [I.adaptive, I.ionic],
      ],
    },
  ],
  13: [ // 6도전자 자야
    {
      champName: '자야', stars: 3,
      coreItems: [I.guinsoos, I.ie, I.lw],
      alternatives: [
        [I.bt, I.hoj],
        [I.jg, I.bt],
        [I.bt, I.guinsoos],
      ],
    },
    {
      champName: '잭스', stars: 2,
      coreItems: [I.bt, I.titans, I.steraks],
      alternatives: [
        [I.warmogs, I.gargoyle],
        [I.warmogs, I.titans],
        [I.dclaw, I.adaptive],
      ],
    },
  ],
  14: [ // 길잡이 나미
    {
      champName: '나미', stars: 3,
      coreItems: [I.shojin, I.morellos, I.adaptive],
      alternatives: [
        [I.bb, I.ionic],
        [I.ionic, I.crownguard],
        [I.crownguard, I.titans],
      ],
    },
    {
      champName: '오로라', stars: 2,
      coreItems: [I.bb, I.jg, I.rdc],
      alternatives: [
        [I.archangel, I.shojin],
        [I.morellos, I.crownguard],
        [I.ionic, I.adaptive],
      ],
    },
  ],
}
