import { ChevronDown, ChevronLeft, ChevronRight, ChevronUp, ChevronsUpDown, Search } from 'lucide-react'
import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import ChampionCard from '../../components/common/ChampionCard'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import { communityDragonAssetUrl } from '../../api/communityDragonAssets'
import type { MetaDeck, TraitSummary, ChampionSummary } from '../Dashboard/dashboardData'
import type { TierBadgeValue } from '../../components/common/TierBadge'
import styles from './Decks.module.css'

/* ════════════════════════════
   타입
════════════════════════════ */
type Tab = '덱모음' | '메타통계'
type SortKey = 'rank' | 'winRate' | 'top4' | 'avgPlace' | 'pickRate'
type SortDir = 'asc' | 'desc'

const TIER_ORDER: TierBadgeValue[] = ['S', 'A+', 'A', 'B', 'C', 'D']
const TIER_COLOR: Record<TierBadgeValue, string> = {
  S: '#04f3e5', 'A+': '#f7d26d', A: '#a78bfa', B: '#60a5fa', C: '#818cf8', D: '#6b7280',
}

/* ════════════════════════════
   영웅 증강 / 유물 타입
════════════════════════════ */
interface HeroAugmentDeck {
  hero: string
  augment: string
  recommended: boolean
  winRate: string
  avgPlace: string
  pickRate: string
  description: string
  tags: string[]
  traits: TraitSummary[]
  champions: ChampionSummary[]
}

interface ArtifactUnit {
  name: string
  imageUrl: string
  frequency: string       // 빈도수
  winRate: string         // 승률
  avgImprovement: string  // 평균 등수 향상 (예: '+0.94')
  top4: string            // TOP4 확률
}

interface ArtifactRec {
  itemName: string
  itemIcon: string
  units: ArtifactUnit[]
}

/* ════════════════════════════
   에셋 URL
════════════════════════════ */
const itemIconUrls = {
  infinityEdge:    communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_InfinityEdge.TFT_Set13.tex'),
  warmogsArmor:    communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_WarmogsArmor.TFT_Set13.tex'),
  rabadonsDeathcap:communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_RabadonsDeathcap.TFT_Set13.tex'),
  spearOfShojin:   communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_SpearOfShojin.TFT_Set13.tex'),
  blueBuff:        communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_BlueBuff.TFT_Set13.tex'),
  giantSlayer:     communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_GiantSlayer.TFT_Set13.tex'),
  dragonsClaw:     communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_DragonsClaw.TFT_Set13.tex'),
  morellonomicon:  communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_Morellonomicon.TFT_Set13.tex'),
  ionicSpark:      communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_IonicSpark.TFT_Set13.tex'),
  titansResolve:   communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_TitansResolve.TFT_Set13.tex'),
}

const champUrls = {
  jhin:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Jhin/Skins/Base/Images/TFT17_Jhin_splash_tile_37.TFT_Set17.tex'),
  kaisa:       communityDragonAssetUrl('ASSETS/Characters/TFT17_Kaisa/Skins/Base/Images/TFT17_Kaisa_splash_tile_69.TFT_Set17.tex'),
  xayah:       communityDragonAssetUrl('ASSETS/Characters/TFT17_Xayah/Skins/Base/Images/TFT17_Xayah_splash_tile_1.TFT_Set17.tex'),
  ornn:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Ornn/Skins/Base/Images/TFT17_Ornn_splash_tile_11.TFT_Set17.tex'),
  illaoi:      communityDragonAssetUrl('ASSETS/Characters/TFT17_Illaoi/Skins/Base/Images/TFT17_Illaoi_splash_tile_27.TFT_Set17.tex'),
  rammus:      communityDragonAssetUrl('ASSETS/Characters/TFT17_Rammus/Skins/Base/Images/TFT17_Rammus_splash_tile_17.TFT_Set17.tex'),
  aurelionSol: communityDragonAssetUrl('ASSETS/Characters/TFT17_AurelionSol/Skins/Base/Images/TFT17_AurelionSol_splash_tile_2.TFT_Set17.tex'),
  vex:         communityDragonAssetUrl('ASSETS/Characters/TFT17_Vex/Skins/Base/Images/TFT17_vex_splash_tile_10.TFT_Set17.tex'),
  viktor:      communityDragonAssetUrl('ASSETS/Characters/TFT17_Viktor/Skins/Base/Images/TFT17_Viktor_splash_tile_5.TFT_Set17.tex'),
  sona:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Sona/Skins/Base/Images/TFT17_Sona_splash_tile_17.TFT_Set17.tex'),
  karma:       communityDragonAssetUrl('ASSETS/Characters/TFT17_Karma/Skins/Base/Images/TFT17_Karma_splash_tile_8.TFT_Set17.tex'),
  masterYi:    communityDragonAssetUrl('ASSETS/Characters/TFT17_MasterYi/Skins/Base/Images/TFT17_MasterYi_splash_tile_33.TFT_Set17.tex'),
  azir:        communityDragonAssetUrl('ASSETS/Characters/TFT17_Azir/Skins/Base/Images/TFT17_Azir_splash_tile_1.TFT_Set17.tex'),
  sejuani:     communityDragonAssetUrl('ASSETS/Characters/TFT17_Sejuani/Skins/Base/Images/TFT17_Sejuani_splash_tile_1.TFT_Set17.tex'),
  yasuo:       communityDragonAssetUrl('ASSETS/Characters/TFT17_Yasuo/Skins/Base/Images/TFT17_Yasuo_splash_tile_1.TFT_Set17.tex'),
  lux:         communityDragonAssetUrl('ASSETS/Characters/TFT17_Lux/Skins/Base/Images/TFT17_Lux_splash_tile_1.TFT_Set17.tex'),
}

/* ════════════════════════════
   목 데이터 — 영웅 증강
   recommended:true → 왼쪽 (추천)
   recommended:false → 오른쪽 (비추천)
════════════════════════════ */
const HERO_AUGMENT_DECKS: HeroAugmentDeck[] = [
  /* ── 추천 ── */
  {
    hero: '진', augment: '마지막 공연', recommended: true,
    winRate: '63.2%', avgPlace: '2.71', pickRate: '2.8%',
    description: '4번째 공격 데미지가 폭발적으로 증가. 암흑의 별 6개 + 진 3성 조합 시 1등 확정급 화력 보장.',
    tags: ['하이리스크', '3성 필수', '암흑의 별 시너지'],
    traits: [],
    champions: [
      { name: '진',   imageUrl: champUrls.jhin,  stars: 3 },
      { name: '카이사', imageUrl: champUrls.kaisa, stars: 2 },
    ],
  },
  {
    hero: '아우렐리온 솔', augment: '우주의 폭발', recommended: true,
    winRate: '61.8%', avgPlace: '2.94', pickRate: '2.1%',
    description: '궁극기 발동 시 광역 폭발 피해가 3배 증가. 요새 덱과 조합해 생존하며 폭딜.',
    tags: ['장기전', '요새 시너지', '후반 캐리'],
    traits: [],
    champions: [
      { name: '아우렐리온 솔', imageUrl: champUrls.aurelionSol, stars: 3 },
      { name: '빅토르',       imageUrl: champUrls.viktor,      stars: 2 },
    ],
  },
  {
    hero: '마스터 이', augment: '알파 스트라이크 강화', recommended: true,
    winRate: '59.5%', avgPlace: '3.12', pickRate: '3.4%',
    description: '알파 스트라이크 추가 타겟 +2, 치명타 시 쿨타임 초기화. 습격자 4 조합 시 무한 광역 공격.',
    tags: ['습격자 필수', '3성 추천', '중반 캐리'],
    traits: [],
    champions: [
      { name: '마스터 이', imageUrl: champUrls.masterYi, stars: 3 },
      { name: '자야',     imageUrl: champUrls.xayah,    stars: 2 },
    ],
  },
  {
    hero: '소나', augment: '하모닉 웨이브', recommended: true,
    winRate: '57.3%', avgPlace: '3.38', pickRate: '1.9%',
    description: '소나 스킬 사용 시 전 아군 체력 회복 + 공격력 증가. 정령족 + 우주 그루브와 시너지.',
    tags: ['힐 덱', '장기전', '정령족 조합'],
    traits: [],
    champions: [
      { name: '소나',   imageUrl: champUrls.sona,  stars: 3 },
      { name: '카르마', imageUrl: champUrls.karma, stars: 2 },
    ],
  },
  /* ── 비추천 ── */
  {
    hero: '아지르', augment: '황제의 군단', recommended: false,
    winRate: '48.2%', avgPlace: '4.31', pickRate: '1.2%',
    description: '황제 시너지 자원 요구량이 높아 안정적인 운영이 어렵고, 일반 덱 대비 효율이 낮음.',
    tags: ['고자원 필요', '별 3 필수', '우주 시너지'],
    traits: [],
    champions: [
      { name: '아지르', imageUrl: champUrls.azir,        stars: 3 },
      { name: '빅토르', imageUrl: champUrls.viktor,      stars: 2 },
    ],
  },
  {
    hero: '세주아니', augment: '빙하 폭풍', recommended: false,
    winRate: '45.7%', avgPlace: '4.68', pickRate: '0.9%',
    description: '발동 조건이 까다롭고 현 메타 카운터 아이템이 다수. 특정 조합에서만 제한적으로 유효.',
    tags: ['발동 불안정', '메타 불리', '탱커 필요'],
    traits: [],
    champions: [
      { name: '세주아니', imageUrl: champUrls.sejuani, stars: 3 },
      { name: '오른',    imageUrl: champUrls.ornn,    stars: 2 },
    ],
  },
  {
    hero: '야스오', augment: '허무의 검', recommended: false,
    winRate: '43.1%', avgPlace: '4.92', pickRate: '0.7%',
    description: '치명타 아이템 의존도가 높아 아이템 없이 불안정. 초반 골드 경쟁에서 불리한 포지션.',
    tags: ['아이템 의존', '불안정', '초반 불리'],
    traits: [],
    champions: [
      { name: '야스오', imageUrl: champUrls.yasuo,    stars: 3 },
      { name: '진',    imageUrl: champUrls.jhin,     stars: 2 },
    ],
  },
  {
    hero: '럭스', augment: '최후의 섬광', recommended: false,
    winRate: '41.3%', avgPlace: '5.14', pickRate: '0.5%',
    description: '후반 캐리형이나 현 패치 스킬 쿨타임 너프로 효율 크게 감소. 패치 상황 지속 모니터링 필요.',
    tags: ['너프 대상', '후반 의존', '현 패치 비추'],
    traits: [],
    champions: [
      { name: '럭스',   imageUrl: champUrls.lux,  stars: 3 },
      { name: '벡스',   imageUrl: champUrls.vex,  stars: 2 },
    ],
  },
]

/* ════════════════════════════
   목 데이터 — 유물 (10개, 초기 4개만 노출)
════════════════════════════ */
const INITIAL_ARTIFACT_COUNT = 4

const ARTIFACT_RECS: ArtifactRec[] = [
  {
    itemName: '무한의 대검', itemIcon: itemIconUrls.infinityEdge,
    units: [
      { name: '진',    imageUrl: champUrls.jhin,  frequency: '18.3%', winRate: '62.1%', avgImprovement: '+0.94', top4: '71.2%' },
      { name: '카이사', imageUrl: champUrls.kaisa, frequency: '14.1%', winRate: '58.4%', avgImprovement: '+0.71', top4: '66.8%' },
      { name: '자야',  imageUrl: champUrls.xayah,  frequency: '11.7%', winRate: '56.2%', avgImprovement: '+0.58', top4: '63.4%' },
    ],
  },
  {
    itemName: '워모그의 갑옷', itemIcon: itemIconUrls.warmogsArmor,
    units: [
      { name: '오른',    imageUrl: champUrls.ornn,   frequency: '22.5%', winRate: '64.8%', avgImprovement: '+1.12', top4: '74.3%' },
      { name: '일라오이', imageUrl: champUrls.illaoi, frequency: '17.8%', winRate: '60.3%', avgImprovement: '+0.87', top4: '69.1%' },
      { name: '람머스',  imageUrl: champUrls.rammus,  frequency: '13.2%', winRate: '57.9%', avgImprovement: '+0.64', top4: '65.7%' },
    ],
  },
  {
    itemName: '라바돈의 죽음모자', itemIcon: itemIconUrls.rabadonsDeathcap,
    units: [
      { name: '아우렐리온 솔', imageUrl: champUrls.aurelionSol, frequency: '15.6%', winRate: '66.2%', avgImprovement: '+1.28', top4: '76.4%' },
      { name: '벡스',         imageUrl: champUrls.vex,         frequency: '12.4%', winRate: '61.7%', avgImprovement: '+0.95', top4: '70.8%' },
      { name: '빅토르',       imageUrl: champUrls.viktor,      frequency: '10.9%', winRate: '58.5%', avgImprovement: '+0.73', top4: '67.2%' },
    ],
  },
  {
    itemName: '쇼진의 창', itemIcon: itemIconUrls.spearOfShojin,
    units: [
      { name: '소나',   imageUrl: champUrls.sona,  frequency: '16.2%', winRate: '59.4%', avgImprovement: '+0.82', top4: '68.5%' },
      { name: '카르마', imageUrl: champUrls.karma, frequency: '13.7%', winRate: '57.1%', avgImprovement: '+0.67', top4: '65.1%' },
    ],
  },
  {
    itemName: '블루 버프', itemIcon: itemIconUrls.blueBuff,
    units: [
      { name: '소나',   imageUrl: champUrls.sona,   frequency: '19.4%', winRate: '65.3%', avgImprovement: '+1.18', top4: '75.2%' },
      { name: '카르마', imageUrl: champUrls.karma,   frequency: '15.8%', winRate: '62.1%', avgImprovement: '+0.96', top4: '71.6%' },
      { name: '빅토르', imageUrl: champUrls.viktor,  frequency: '12.3%', winRate: '58.7%', avgImprovement: '+0.74', top4: '67.5%' },
    ],
  },
  {
    itemName: '거인 슬레이어', itemIcon: itemIconUrls.giantSlayer,
    units: [
      { name: '진',      imageUrl: champUrls.jhin,     frequency: '16.7%', winRate: '61.2%', avgImprovement: '+0.91', top4: '70.3%' },
      { name: '마스터 이', imageUrl: champUrls.masterYi, frequency: '14.2%', winRate: '59.8%', avgImprovement: '+0.79', top4: '68.9%' },
      { name: '카이사',  imageUrl: champUrls.kaisa,    frequency: '11.8%', winRate: '57.4%', avgImprovement: '+0.63', top4: '65.8%' },
    ],
  },
  {
    itemName: '용의 발톱', itemIcon: itemIconUrls.dragonsClaw,
    units: [
      { name: '오른',    imageUrl: champUrls.ornn,   frequency: '20.1%', winRate: '63.1%', avgImprovement: '+1.05', top4: '73.2%' },
      { name: '람머스',  imageUrl: champUrls.rammus,  frequency: '16.4%', winRate: '60.7%', avgImprovement: '+0.88', top4: '70.1%' },
      { name: '일라오이', imageUrl: champUrls.illaoi, frequency: '13.5%', winRate: '55.3%', avgImprovement: '+0.52', top4: '63.9%' },
    ],
  },
  {
    itemName: '모렐로노미콘', itemIcon: itemIconUrls.morellonomicon,
    units: [
      { name: '아우렐리온 솔', imageUrl: champUrls.aurelionSol, frequency: '14.8%', winRate: '64.5%', avgImprovement: '+1.15', top4: '74.8%' },
      { name: '벡스',         imageUrl: champUrls.vex,         frequency: '12.1%', winRate: '60.2%', avgImprovement: '+0.88', top4: '69.4%' },
      { name: '카르마',       imageUrl: champUrls.karma,       frequency: '9.8%',  winRate: '56.8%', avgImprovement: '+0.61', top4: '64.7%' },
    ],
  },
  {
    itemName: '이온 스파크', itemIcon: itemIconUrls.ionicSpark,
    units: [
      { name: '벡스',   imageUrl: champUrls.vex,   frequency: '13.6%', winRate: '62.3%', avgImprovement: '+0.97', top4: '71.9%' },
      { name: '빅토르', imageUrl: champUrls.viktor, frequency: '11.3%', winRate: '60.5%', avgImprovement: '+0.85', top4: '70.2%' },
      { name: '카르마', imageUrl: champUrls.karma,  frequency: '9.4%',  winRate: '57.2%', avgImprovement: '+0.66', top4: '65.4%' },
    ],
  },
  {
    itemName: '타이탄의 결의', itemIcon: itemIconUrls.titansResolve,
    units: [
      { name: '오른',    imageUrl: champUrls.ornn,   frequency: '24.8%', winRate: '68.1%', avgImprovement: '+1.42', top4: '78.5%' },
      { name: '일라오이', imageUrl: champUrls.illaoi, frequency: '19.3%', winRate: '62.4%', avgImprovement: '+1.07', top4: '72.8%' },
      { name: '람머스',  imageUrl: champUrls.rammus,  frequency: '15.7%', winRate: '59.0%', avgImprovement: '+0.81', top4: '68.3%' },
    ],
  },
]

/* ════════════════════════════
   유틸
════════════════════════════ */
function numVal(s: string) { return parseFloat(s.replace('%', '')) }

function sortDecks(decks: MetaDeck[], key: SortKey, dir: SortDir) {
  return [...decks].sort((a, b) => {
    const av = key === 'rank' ? a.rank : numVal(a[key])
    const bv = key === 'rank' ? b.rank : numVal(b[key])
    const naturalAsc = key === 'avgPlace' || key === 'rank'
    const base = av < bv ? -1 : av > bv ? 1 : 0
    return (naturalAsc ? base : -base) * (dir === 'asc' ? 1 : -1)
  })
}

/* ════════════════════════════
   공통 컴포넌트
════════════════════════════ */
function SortIcon({ col, cur, dir }: { col: SortKey; cur: SortKey; dir: SortDir }) {
  if (col !== cur) return <ChevronsUpDown size={12} className={styles.sortIcon} />
  return dir === 'asc'
    ? <ChevronUp   size={12} className={`${styles.sortIcon} ${styles.sortActive}`} />
    : <ChevronDown size={12} className={`${styles.sortIcon} ${styles.sortActive}`} />
}

/** 덱 행 */
function DeckRow({
  deck, showTier = true, showRank = true,
}: { deck: MetaDeck; showTier?: boolean; showRank?: boolean }) {
  const navigate = useNavigate()
  return (
    <tr className={styles.deckRow} onClick={() => navigate(`/decks/${deck.rank}`)} style={{ cursor: 'pointer' }}>
      {showRank && (
        <td>
          <strong className={styles.rank} data-top={deck.rank <= 3 ? deck.rank : undefined}>
            {deck.rank}
          </strong>
        </td>
      )}
      {showTier && <td><TierBadge value={deck.grade} /></td>}
      <td className={styles.nameCol}>
        <span className={styles.deckName}>{deck.name}</span>
        <span className={styles.traits}>
          {deck.traits.map((t) => (
            <TraitHexBadge key={`${t.name}-${t.count}`} count={t.count} iconUrl={t.iconUrl} name={t.name} tone={t.tone} />
          ))}
        </span>
      </td>
      <td className={styles.champCol}>
        <span className={styles.champions}>
          {deck.champions.map((c, i) => (
            <ChampionCard key={`${c.name}-${i}`} imageUrl={c.imageUrl} items={c.items} label={c.name} stars={c.stars} toneIndex={i} />
          ))}
        </span>
      </td>
      <td className={styles.winRate}>{deck.winRate}</td>
      <td className={styles.top4}>{deck.top4}</td>
      <td className={styles.avgPlace}><span className={styles.avgHash}>#</span>{deck.avgPlace}</td>
      <td className={styles.pickRate}>{deck.pickRate}</td>
    </tr>
  )
}

/** 테이블 헤더 */
function TableHead({ sortKey, sortDir, onSort, showTier = true, showRank = true }: {
  sortKey: SortKey; sortDir: SortDir; onSort: (k: SortKey) => void
  showTier?: boolean; showRank?: boolean
}) {
  function Th({ label, col }: { label: string; col: SortKey }) {
    return (
      <th className={styles.sortTh} onClick={() => onSort(col)}>
        {label}<SortIcon col={col} cur={sortKey} dir={sortDir} />
      </th>
    )
  }
  return (
    <thead>
      <tr>
        {showRank && <Th label="순위" col="rank" />}
        {showTier && <th>티어</th>}
        <th className={styles.nameCol}>덱 이름 / 시너지</th>
        <th className={styles.champCol}>챔피언 구성</th>
        <Th label="승률" col="winRate" />
        <Th label="TOP 4" col="top4" />
        <Th label="평균 등수" col="avgPlace" />
        <Th label="픽률" col="pickRate" />
      </tr>
    </thead>
  )
}

/* ════════════════════════════
   영웅 증강 섹션 (캐러셀)
════════════════════════════ */
function HeroAugmentSection() {
  const scrollRef = useRef<HTMLDivElement>(null)

  function scrollCarousel(dir: 'left' | 'right') {
    if (!scrollRef.current) return
    scrollRef.current.scrollBy({ left: dir === 'left' ? -316 : 316, behavior: 'smooth' })
  }

  // 추천(true) → 왼쪽, 비추천(false) → 오른쪽
  const sorted = [...HERO_AUGMENT_DECKS].sort((a, b) => {
    if (a.recommended === b.recommended) return 0
    return a.recommended ? -1 : 1
  })

  return (
    <section className={styles.specialSection}>
      <div className={styles.specialHeader}>
        <span className={styles.specialBadge}>영웅 증강</span>
        <div className={styles.specialHeaderText}>
          <h2>영웅 증강 특수 덱</h2>
          <p>특정 영웅 증강을 보유했을 때만 가능한 고승률 전략</p>
        </div>
        <div className={styles.carouselBtns}>
          <button type="button" className={styles.carouselBtn} onClick={() => scrollCarousel('left')}>
            <ChevronLeft size={16} />
          </button>
          <button type="button" className={styles.carouselBtn} onClick={() => scrollCarousel('right')}>
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

      <div className={styles.augmentCarousel} ref={scrollRef}>
        {sorted.map((d) => (
          <article
            key={`${d.hero}-${d.augment}`}
            className={styles.augmentCard}
            data-recommended={d.recommended ? 'true' : 'false'}
          >
            <div className={styles.augCardTop}>
              <div>
                <div className={styles.augHeroName}>{d.hero}</div>
                <div className={styles.augName}>[{d.augment}]</div>
              </div>
              <span className={d.recommended ? styles.augRecommendBadge : styles.augNotRecommendBadge}>
                {d.recommended ? '추천' : '비추천'}
              </span>
            </div>
            <p className={styles.augDesc}>{d.description}</p>
            <div className={styles.augTags}>
              {d.tags.map((tag) => <span key={tag} className={styles.augTag}>{tag}</span>)}
            </div>
            <div className={styles.augChamps}>
              {d.champions.map((c, i) => (
                <ChampionCard key={c.name} imageUrl={c.imageUrl} label={c.name} stars={c.stars} toneIndex={i} />
              ))}
            </div>
            <div className={styles.augStats}>
              <div><small>승률</small><strong className={styles.winRate}>{d.winRate}</strong></div>
              <div><small>평균 등수</small><strong className={styles.avgPlace}><span className={styles.avgHash}>#</span>{d.avgPlace}</strong></div>
              <div><small>픽률</small><strong className={styles.pickRate}>{d.pickRate}</strong></div>
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}

/* ════════════════════════════
   유물 섹션 (검색 상시 노출 + 더보기)
════════════════════════════ */
function ArtifactSection() {
  const [showAll, setShowAll] = useState(false)
  const [search, setSearch]   = useState('')

  const searchActive = search.trim() !== ''
  // 검색 중이면 전체에서 필터, 아니면 showAll 여부에 따라 slice
  const allFiltered = ARTIFACT_RECS.filter((r) => !searchActive || r.itemName.includes(search))
  const visible     = searchActive || showAll ? allFiltered : allFiltered.slice(0, INITIAL_ARTIFACT_COUNT)
  const hiddenCount = ARTIFACT_RECS.length - INITIAL_ARTIFACT_COUNT

  return (
    <section className={styles.specialSection}>
      <div className={styles.specialHeader}>
        <span
          className={styles.specialBadge}
          style={{ background: 'rgba(249,200,96,0.18)', color: '#f9c860', borderColor: 'rgba(249,200,96,0.35)' }}
        >
          유물 추천
        </span>
        <div className={styles.specialHeaderText}>
          <h2>유물별 최적 유닛</h2>
          <p>시너지 무관, 해당 유물 장착 시 승률이 크게 오르는 유닛</p>
        </div>
        {/* 검색 — 항상 표시 */}
        <div className={styles.artifactSearch}>
          <Search size={14} />
          <input
            placeholder="유물 이름 검색"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      <div className={styles.artifactList}>
        {visible.length === 0 ? (
          <p className={styles.empty}>검색 결과가 없습니다.</p>
        ) : (
          visible.map((rec) => (
            <div key={rec.itemName} className={styles.artifactRow}>
              {/* 아이템 아이콘 + 이름 */}
              <div className={styles.artifactItem}>
                <img src={rec.itemIcon} alt={rec.itemName} className={styles.artifactIcon} />
                <span className={styles.artifactName}>{rec.itemName}</span>
              </div>

              {/* 유닛 목록 + 컬럼 헤더 */}
              <div className={styles.artifactUnits}>
                <div className={styles.artifactUnitHeader}>
                  <span />
                  <span>유닛</span>
                  <span>빈도수</span>
                  <span>승률</span>
                  <span>평균 등수 향상</span>
                  <span>TOP4</span>
                </div>
                {rec.units.map((u) => (
                  <div key={u.name} className={styles.artifactUnit}>
                    <img src={u.imageUrl} alt={u.name} className={styles.artifactChampImg} />
                    <span className={styles.artifactUnitName}>{u.name}</span>
                    <span className={`${styles.artifactStat} ${styles.artifactStatFreq}`}>{u.frequency}</span>
                    <span className={`${styles.artifactStat} ${styles.artifactStatWin}`}>{u.winRate}</span>
                    <span className={`${styles.artifactStat} ${styles.artifactStatImp}`}>{u.avgImprovement}</span>
                    <span className={`${styles.artifactStat} ${styles.artifactStatTop4}`}>{u.top4}</span>
                  </div>
                ))}
              </div>
            </div>
          ))
        )}
      </div>

      {/* 더보기 / 접기 — 검색 중이 아닐 때만 표시 */}
      {!searchActive && hiddenCount > 0 && (
        <button
          type="button"
          className={styles.showMoreBtn}
          onClick={() => setShowAll((v) => !v)}
        >
          {showAll ? '접기' : `더보기 (${hiddenCount}개 더)`}
        </button>
      )}
    </section>
  )
}

/* ════════════════════════════
   탭 1 — 덱모음
   (순위·티어 없음 + 영웅 증강 + 유물 추천)
════════════════════════════ */
function DeckListView({ decks }: { decks: MetaDeck[] }) {
  const [search, setSearch]   = useState('')
  const [sortKey, setSortKey] = useState<SortKey>('winRate')
  const [sortDir, setSortDir] = useState<SortDir>('desc')

  function handleSort(key: SortKey) {
    if (sortKey === key) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    else { setSortKey(key); setSortDir(key === 'avgPlace' ? 'asc' : 'desc') }
  }

  const filtered = sortDecks(decks.filter((d) => d.name.includes(search)), sortKey, sortDir)

  return (
    <>
      <div className={styles.toolBar}>
        <div className={styles.searchBox}>
          <Search size={14} />
          <input placeholder="덱 이름 검색" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <span className={styles.countLabel}>{filtered.length}개 덱</span>
      </div>
      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <TableHead sortKey={sortKey} sortDir={sortDir} onSort={handleSort} showTier={false} showRank={false} />
          <tbody>
            {filtered.length === 0
              ? <tr><td colSpan={6} className={styles.empty}>검색 결과가 없습니다.</td></tr>
              : filtered.map((d) => <DeckRow key={d.rank} deck={d} showTier={false} showRank={false} />)
            }
          </tbody>
        </table>
      </div>
      <HeroAugmentSection />
      <ArtifactSection />
    </>
  )
}

/* ════════════════════════════
   탭 2 — 메타통계
   (lolchess 스타일, 티어별 수직)
════════════════════════════ */
function MetaStatsView({ decks }: { decks: MetaDeck[] }) {
  const [sortKey, setSortKey] = useState<SortKey>('rank')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  function handleSort(key: SortKey) {
    if (sortKey === key) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    else { setSortKey(key); setSortDir(key === 'avgPlace' ? 'asc' : 'desc') }
  }

  return (
    <div className={styles.tableWrap}>
      <table className={styles.table}>
        <TableHead sortKey={sortKey} sortDir={sortDir} onSort={handleSort} showTier showRank />
        <tbody>
          {TIER_ORDER.map((tier) => {
            const tierDecks = sortDecks(decks.filter((d) => d.grade === tier), sortKey, sortDir)
            if (tierDecks.length === 0) return null
            const color = TIER_COLOR[tier]
            return (
              <>
                <tr key={`header-${tier}`} className={styles.tierHeaderRow}>
                  <td colSpan={8}>
                    <span className={styles.tierHeaderInner} style={{ borderLeftColor: color }}>
                      <TierBadge value={tier} />
                      <span className={styles.tierName} style={{ color }}>{tier} 티어</span>
                      <span className={styles.tierDesc}>
                        {tier === 'S'  ? '최상위 픽 · 강력 추천'
                          : tier === 'A+' ? '상위권 안정적 덱'
                          : tier === 'A'  ? '중상위권 범용 덱'
                          : tier === 'B'  ? '중위권 상황 의존적'
                          : tier === 'C'  ? '하위권 전문 운영 필요'
                          : '비추천 · 낮은 안정성'}
                      </span>
                      <span className={styles.tierCount}>{tierDecks.length}개</span>
                    </span>
                  </td>
                </tr>
                {tierDecks.map((d) => <DeckRow key={d.rank} deck={d} showTier showRank />)}
              </>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

/* ════════════════════════════
   메인
════════════════════════════ */
function Decks() {
  const { data: decks = [] } = useMetaSnapshot()
  const [tab, setTab] = useState<Tab>('덱모음')

  return (
    <AppLayout>
      <div className={styles.page}>
        <div className={styles.pageHeader}>
          <div className={styles.titleBlock}>
            <h1>덱모음</h1>
            <p>현재 패치 기준 전체 메타 덱 · 승률 · 픽률 · 평균 등수</p>
          </div>
          <div className={styles.tabBar}>
            <button type="button" className={tab === '덱모음' ? styles.activeTab : ''} onClick={() => setTab('덱모음')}>
              덱모음
            </button>
            <button type="button" className={tab === '메타통계' ? styles.activeTab : ''} onClick={() => setTab('메타통계')}>
              메타통계
            </button>
          </div>
        </div>

        {tab === '덱모음'
          ? <DeckListView decks={decks} />
          : <MetaStatsView decks={decks} />
        }
      </div>
    </AppLayout>
  )
}

export default Decks
