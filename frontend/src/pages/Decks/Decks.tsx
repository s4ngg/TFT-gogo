import { ChevronDown, ChevronUp, ChevronsUpDown, Search } from 'lucide-react'
import { useState } from 'react'
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
   영웅 증강 / 유물 목 데이터
════════════════════════════ */
interface HeroAugmentDeck {
  hero: string
  augment: string
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
  reason: string
  winRate: string
}

interface ArtifactRec {
  itemName: string
  itemIcon: string
  units: ArtifactUnit[]
}

const itemIconUrls = {
  infinityEdge: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_InfinityEdge.TFT_Set13.tex'),
  warmogsArmor: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_WarmogsArmor.TFT_Set13.tex'),
  rabadonsDeathcap: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_RabadonsDeathcap.TFT_Set13.tex'),
  spearOfShojin: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_SpearOfShojin.TFT_Set13.tex'),
  blueBuff: communityDragonAssetUrl('ASSETS/Maps/TFT/Icons/Items/Hexcore/TFT_Item_BlueBuff.TFT_Set13.tex'),
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
}

const HERO_AUGMENT_DECKS: HeroAugmentDeck[] = [
  {
    hero: '진',
    augment: '마지막 공연',
    winRate: '63.2%',
    avgPlace: '2.71',
    pickRate: '2.8%',
    description: '4번째 공격 데미지가 폭발적으로 증가. 암흑의 별 6개 + 진 3성 조합 시 1등 확정급 화력 보장.',
    tags: ['하이리스크', '3성 필수', '암흑의 별 시너지'],
    traits: [],
    champions: [
      { name: '진', imageUrl: champUrls.jhin, stars: 3 },
      { name: '카이사', imageUrl: champUrls.kaisa, stars: 2 },
    ],
  },
  {
    hero: '아우렐리온 솔',
    augment: '우주의 폭발',
    winRate: '61.8%',
    avgPlace: '2.94',
    pickRate: '2.1%',
    description: '궁극기 발동 시 광역 폭발 피해가 3배 증가. 요새 덱과 조합해 생존하며 폭딜.',
    tags: ['장기전', '요새 시너지', '후반 캐리'],
    traits: [],
    champions: [
      { name: '아우렐리온 솔', imageUrl: champUrls.aurelionSol, stars: 3 },
      { name: '빅토르', imageUrl: champUrls.viktor, stars: 2 },
    ],
  },
  {
    hero: '마스터 이',
    augment: '알파 스트라이크 강화',
    winRate: '59.5%',
    avgPlace: '3.12',
    pickRate: '3.4%',
    description: '알파 스트라이크 추가 타겟 +2, 치명타 시 쿨타임 초기화. 습격자 4 조합 시 무한 광역 공격.',
    tags: ['습격자 필수', '3성 추천', '중반 캐리'],
    traits: [],
    champions: [
      { name: '마스터 이', imageUrl: champUrls.masterYi, stars: 3 },
      { name: '자야', imageUrl: champUrls.xayah, stars: 2 },
    ],
  },
  {
    hero: '소나',
    augment: '하모닉 웨이브',
    winRate: '57.3%',
    avgPlace: '3.38',
    pickRate: '1.9%',
    description: '소나 스킬 사용 시 전 아군 체력 회복 + 공격력 증가. 정령족 + 우주 그루브와 시너지.',
    tags: ['힐 덱', '장기전', '정령족 조합'],
    traits: [],
    champions: [
      { name: '소나', imageUrl: champUrls.sona, stars: 3 },
      { name: '카르마', imageUrl: champUrls.karma, stars: 2 },
    ],
  },
]

const ARTIFACT_RECS: ArtifactRec[] = [
  {
    itemName: '무한의 대검',
    itemIcon: itemIconUrls.infinityEdge,
    units: [
      { name: '진', imageUrl: champUrls.jhin, reason: '4번째 공격이 항상 치명타 → 극강 폭딜', winRate: '62.1%' },
      { name: '카이사', imageUrl: champUrls.kaisa, reason: '빠른 공격속도로 치명타 누적', winRate: '58.4%' },
      { name: '자야', imageUrl: champUrls.xayah, reason: '도전자 시너지로 공속 증가 후 치명타 폭딜', winRate: '56.2%' },
    ],
  },
  {
    itemName: '워모그의 갑옷',
    itemIcon: itemIconUrls.warmogsArmor,
    units: [
      { name: '오른', imageUrl: champUrls.ornn, reason: '체력 비례 방어력 스킬과 시너지', winRate: '64.8%' },
      { name: '일라오이', imageUrl: champUrls.illaoi, reason: '체력 회복 + 촉수 생성으로 무한 지속', winRate: '60.3%' },
      { name: '람머스', imageUrl: champUrls.rammus, reason: '요새 시너지로 방어력 → 데미지 전환', winRate: '57.9%' },
    ],
  },
  {
    itemName: '라바돈의 죽음모자',
    itemIcon: itemIconUrls.rabadonsDeathcap,
    units: [
      { name: '아우렐리온 솔', imageUrl: champUrls.aurelionSol, reason: '광역 마법 피해 극대화', winRate: '66.2%' },
      { name: '벡스', imageUrl: champUrls.vex, reason: '연속 스킬 발동 시 AP 폭발 효과', winRate: '61.7%' },
      { name: '빅토르', imageUrl: champUrls.viktor, reason: '복제자 시너지로 AP 2배 적용', winRate: '58.5%' },
    ],
  },
  {
    itemName: '쇼진의 창',
    itemIcon: itemIconUrls.spearOfShojin,
    units: [
      { name: '소나', imageUrl: champUrls.sona, reason: '스킬 반복 사용으로 힐 + 버프 지속', winRate: '59.4%' },
      { name: '카르마', imageUrl: champUrls.karma, reason: '빠른 궁극기 발동으로 팀 강화', winRate: '57.1%' },
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

/** 정렬 아이콘 */
function SortIcon({ col, cur, dir }: { col: SortKey; cur: SortKey; dir: SortDir }) {
  if (col !== cur) return <ChevronsUpDown size={12} className={styles.sortIcon} />
  return dir === 'asc'
    ? <ChevronUp size={12} className={`${styles.sortIcon} ${styles.sortActive}`} />
    : <ChevronDown size={12} className={`${styles.sortIcon} ${styles.sortActive}`} />
}

/** 덱 행 — showTier=false 이면 티어 컬럼 숨김 */
function DeckRow({ deck, showTier = true }: { deck: MetaDeck; showTier?: boolean }) {
  return (
    <tr className={styles.deckRow}>
      <td>
        <strong className={styles.rank} data-top={deck.rank <= 3 ? deck.rank : undefined}>
          {deck.rank}
        </strong>
      </td>
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
function TableHead({ sortKey, sortDir, onSort, showTier = true }: {
  sortKey: SortKey; sortDir: SortDir; onSort: (k: SortKey) => void; showTier?: boolean
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
        <Th label="순위" col="rank" />
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
   탭 1 — 덱모음
   (티어 없음 + 영웅 증강 + 유물 추천)
════════════════════════════ */

function HeroAugmentSection() {
  return (
    <section className={styles.specialSection}>
      <div className={styles.specialHeader}>
        <span className={styles.specialBadge}>영웅 증강</span>
        <h2>영웅 증강 특수 덱</h2>
        <p>특정 영웅 증강을 보유했을 때만 가능한 고승률 전략</p>
      </div>
      <div className={styles.augmentGrid}>
        {HERO_AUGMENT_DECKS.map((d) => (
          <article key={`${d.hero}-${d.augment}`} className={styles.augmentCard}>
            <div className={styles.augCardTop}>
              <div className={styles.augHeroName}>{d.hero}</div>
              <div className={styles.augName}>[{d.augment}]</div>
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

function ArtifactSection() {
  return (
    <section className={styles.specialSection}>
      <div className={styles.specialHeader}>
        <span className={styles.specialBadge} style={{ background: 'rgba(249,200,96,0.18)', color: '#f9c860', borderColor: 'rgba(249,200,96,0.35)' }}>유물 추천</span>
        <h2>유물별 최적 유닛</h2>
        <p>시너지 무관, 해당 유물 장착 시 승률이 크게 오르는 유닛</p>
      </div>
      <div className={styles.artifactList}>
        {ARTIFACT_RECS.map((rec) => (
          <div key={rec.itemName} className={styles.artifactRow}>
            <div className={styles.artifactItem}>
              <img src={rec.itemIcon} alt={rec.itemName} className={styles.artifactIcon} />
              <span className={styles.artifactName}>{rec.itemName}</span>
            </div>
            <div className={styles.artifactUnits}>
              {rec.units.map((u) => (
                <div key={u.name} className={styles.artifactUnit}>
                  <img src={u.imageUrl} alt={u.name} className={styles.artifactChampImg} />
                  <div className={styles.artifactUnitInfo}>
                    <span className={styles.artifactUnitName}>{u.name}</span>
                    <span className={styles.artifactUnitReason}>{u.reason}</span>
                  </div>
                  <span className={styles.artifactWinRate}>{u.winRate}</span>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

function DeckListView({ decks }: { decks: MetaDeck[] }) {
  const [search, setSearch] = useState('')
  const [sortKey, setSortKey] = useState<SortKey>('rank')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

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
          <TableHead sortKey={sortKey} sortDir={sortDir} onSort={handleSort} showTier={false} />
          <tbody>
            {filtered.length === 0
              ? <tr><td colSpan={7} className={styles.empty}>검색 결과가 없습니다.</td></tr>
              : filtered.map((d) => <DeckRow key={d.rank} deck={d} showTier={false} />)
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
        <TableHead sortKey={sortKey} sortDir={sortDir} onSort={handleSort} showTier />
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
                        {tier === 'S' ? '최상위 픽 · 강력 추천'
                          : tier === 'A+' ? '상위권 안정적 덱'
                          : tier === 'A' ? '중상위권 범용 덱'
                          : tier === 'B' ? '중위권 상황 의존적'
                          : tier === 'C' ? '하위권 전문 운영 필요'
                          : '비추천 · 낮은 안정성'}
                      </span>
                      <span className={styles.tierCount}>{tierDecks.length}개</span>
                    </span>
                  </td>
                </tr>
                {tierDecks.map((d) => <DeckRow key={d.rank} deck={d} showTier />)}
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
