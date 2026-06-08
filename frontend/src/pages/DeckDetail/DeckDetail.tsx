import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft, BookOpen, Map as MapIcon, Swords, Trophy, Zap } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import { useCDragonLocale } from '../../hooks/useCDragonLocale'
import { getChampionApiName, getChampionDetail, getChampionName, getChampionShortName, getItemName, getTraitName } from '../../api/cdragonLocale'
import type { TFTLocale } from '../../api/cdragonLocale'
import { tftItemIconUrl, tftItemIconOnError } from '../../api/communityDragonAssets'
import type { ChampionSummary, MetaDeck, HeroAugmentSummary } from '../Dashboard/dashboardData'
import styles from './DeckDetail.module.css'

const BOARD_ROWS = 4
const BOARD_COLS = 7

// CSS Modules에서 동적 클래스 접근을 Record 상수로 대체 (lint 안전, 타입 안정성)
const BP_TIER_STYLES: Record<string, string> = {
  bronze:    styles.bpBronze,
  silver:    styles.bpSilver,
  gold:      styles.bpGold,
  prismatic: styles.bpPrismatic,
}

function isCarry(champ: ChampionSummary): boolean {
  return (champ.recommendedItems?.length ?? 0) > 0
}

function getCost(champ: ChampionSummary, locale: TFTLocale | undefined): number {
  return getChampionDetail(champ.imageUrl, locale)?.cost ?? champ.cost ?? 0
}

function getRange(champ: ChampionSummary, locale: TFTLocale | undefined): number {
  return getChampionDetail(champ.imageUrl, locale)?.range ?? 1
}

function getRole(champ: ChampionSummary, locale: TFTLocale | undefined): string {
  return getChampionDetail(champ.imageUrl, locale)?.role?.toLowerCase() ?? ''
}

function isFrontline(champ: ChampionSummary, locale: TFTLocale | undefined): boolean {
  const role = getRole(champ, locale)
  return getRange(champ, locale) <= 2 || role.includes('tank') || role.includes('fighter')
}

function isBackline(champ: ChampionSummary, locale: TFTLocale | undefined): boolean {
  const role = getRole(champ, locale)
  return getRange(champ, locale) >= 4 || role.includes('caster') || role.includes('marksman')
}

interface PlacedChamp {
  champ: ChampionSummary
  row: number
  col: number
  items?: string[]  // 관리자 지정 아이템 (없으면 champ.recommendedItems 사용)
}

function placeLine(units: ChampionSummary[], rows: number[], result: PlacedChamp[]) {
  if (units.length === 0) return

  const perRow = Math.ceil(units.length / rows.length)
  let placed = 0

  rows.forEach((row) => {
    const chunk = units.slice(placed, placed + perRow)
    if (chunk.length === 0) return

    const startCol = Math.floor((BOARD_COLS - chunk.length) / 2)
    chunk.forEach((champ, i) => result.push({ champ, row, col: startCol + i }))
    placed += chunk.length
  })
}

function buildBoardPositions(visible: ChampionSummary[], locale: TFTLocale | undefined): PlacedChamp[] {
  const frontliners = visible.filter((champ) => isFrontline(champ, locale))
  const backliners = visible.filter((champ) => isBackline(champ, locale) && !frontliners.includes(champ))
  const flexUnits = visible.filter((champ) => !frontliners.includes(champ) && !backliners.includes(champ))
  const result: PlacedChamp[] = []

  placeLine(frontliners, [3, 2], result)
  placeLine(flexUnits, [2, 1], result)
  placeLine(backliners, [0, 1], result)

  return result
}

/** 챔피언 목록 기반으로 트레이트별 유닛 수를 재계산 */
function computeTraitCounts(
  champions: ChampionSummary[],
  locale: TFTLocale | undefined,
): Map<string, number> {
  const counts = new Map<string, number>()
  if (!locale) return counts
  champions.forEach((champ) => {
    const detail = getChampionDetail(champ.imageUrl, locale)
    detail?.traits.forEach((trait) => {
      counts.set(trait, (counts.get(trait) ?? 0) + 1)
    })
  })
  return counts
}

interface CellPosData { row: number; col: number; items?: string[] }

// 항상 Map 반환 — null/빈 Map → 빈 보드, 내용 있으면 관리자 배치
function parseBoardPositions(json: string | null | undefined, level: number): Map<string, CellPosData> {
  if (!json) return new Map()  // 미설정 → 빈 보드
  try {
    const obj = JSON.parse(json) as Record<string, unknown>
    const levelData = obj[String(level)]
    if (levelData && typeof levelData === 'object') {
      const entries = Object.entries(levelData as Record<string, unknown>)
        .filter((entry): entry is [string, CellPosData] => {
          const v = entry[1]
          return (
            typeof v === 'object' && v !== null &&
            typeof (v as CellPosData).row === 'number' &&
            typeof (v as CellPosData).col === 'number' &&
            ((v as CellPosData).items === undefined || Array.isArray((v as CellPosData).items))
          )
        })
      return new Map(entries)
    }
    return new Map()  // 레벨 미설정 → 빈 보드
  } catch {
    return new Map()
  }
}

function buildCustomBoardPositions(
  visibleUnits: ChampionSummary[],
  posMap: Map<string, CellPosData>,
): PlacedChamp[] {
  return visibleUnits.flatMap((champ) => {
    const apiName = getChampionApiName(champ.imageUrl)
    const pos = apiName ? posMap.get(apiName) : undefined
    return pos ? [{ champ, row: pos.row, col: pos.col, items: pos.items }] : []
  })
}

/* ── HexBoard: visibleUnits·level은 부모(DeckDetail)에서 받음 ── */
interface HexBoardProps {
  visibleUnits: ChampionSummary[]
  level: number
  availableLevels: number[]
  onLevelChange: (lv: number) => void
  locale: TFTLocale | undefined
  customPosMap: Map<string, CellPosData>
}

function HexBoard({ visibleUnits, level, availableLevels, onLevelChange, locale, customPosMap }: HexBoardProps) {

  const placed = useMemo(() => {
    // 관리자 배치 없음 → 전체 자동 배치
    if (customPosMap.size === 0) return buildBoardPositions(visibleUnits, locale)

    // 부분 매핑 검증 — visibleUnits 중 매핑 누락된 유닛이 있으면 자동 배치로 fallback
    // (일부만 매핑된 상태로 커스텀 배치를 쓰면 미매핑 유닛이 보드에서 사라짐)
    const allMapped = visibleUnits.every((c) => {
      const apiName = getChampionApiName(c.imageUrl)
      return apiName ? customPosMap.has(apiName) : false
    })
    if (!allMapped) return buildBoardPositions(visibleUnits, locale)

    // 전체 매핑 확인 → 관리자 배치 사용
    return buildCustomBoardPositions(visibleUnits, customPosMap)
  }, [visibleUnits, customPosMap, locale])

  if (visibleUnits.length === 0) return null

  function champAt(row: number, col: number): PlacedChamp | undefined {
    return placed.find((p) => p.row === row && p.col === col)
  }

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <MapIcon size={16} />
        <h2>추천 배치</h2>
        <span className={styles.panelSub}>위 2줄 탱커, 아래 2줄 딜러</span>
      </div>
      <div className={styles.boardWrap}>
        <div className={styles.board}>
          {Array.from({ length: BOARD_ROWS }, (_, vi) => {
            const row = BOARD_ROWS - 1 - vi
            const isOffset = row % 2 !== 0
            return (
              <div key={row} className={`${styles.boardRow} ${isOffset ? styles.boardRowOffset : ''}`}>
                {Array.from({ length: BOARD_COLS }, (_, col) => {
                  const p = champAt(row, col)
                  const itemUrls = p
                    ? (p.items ?? p.champ.recommendedItems ?? []).slice(0, 3).map((id) => tftItemIconUrl(id))
                    : []

                  return (
                    <div key={col} className={styles.hexCell}>
                      <div className={`${styles.hexOuter} ${p ? styles.hexFilled : styles.hexEmpty}`}>
                        {p && (
                          <>
                            <img src={p.champ.imageUrl} alt={p.champ.name} className={styles.hexImg} />
                            <span className={styles.hexStars}>{'★'.repeat(p.champ.stars ?? 2)}</span>
                          </>
                        )}
                      </div>
                      {p && itemUrls.length > 0 && (
                        <div className={styles.hexItems}>
                          {itemUrls.map((url, idx) => (
                            <img
                              key={idx}
                              src={url}
                              alt=""
                              className={styles.hexItemIcon}
                              onError={tftItemIconOnError}
                            />
                          ))}
                        </div>
                      )}
                      {p && (
                        <span className={styles.hexName}>
                          {getChampionName(p.champ.imageUrl, locale, p.champ.name)}
                        </span>
                      )}
                    </div>
                  )
                })}
              </div>
            )
          })}
        </div>

        {availableLevels.length > 1 && (
          <div className={styles.levelTabs}>
            {availableLevels.map((lv) => (
              <button
                key={lv}
                type="button"
                className={`${styles.levelTab} ${lv === level ? styles.levelTabActive : ''}`}
                onClick={() => onLevelChange(lv)}
              >
                Lv.{lv}
              </button>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}


interface PlayGuide { early: string; mid: string; late: string }

function PlayGuidePanel({ deck }: { deck: MetaDeck }) {
  if (!deck.playGuide) return null
  let guide: PlayGuide
  try { guide = JSON.parse(deck.playGuide) as PlayGuide } catch { return null }
  if (!guide.early && !guide.mid && !guide.late) return null

  const phases: { key: keyof PlayGuide; label: string }[] = [
    { key: 'early', label: '초반' },
    { key: 'mid', label: '중반' },
    { key: 'late', label: '후반' },
  ]

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <BookOpen size={16} />
        <h2>운영 방법</h2>
      </div>
      <div className={styles.guideList}>
        {phases.filter((p) => guide[p.key]).map((p) => (
          <div key={p.key} className={styles.guidePhase}>
            <span className={styles.guidePhaseLabel}>{p.label}</span>
            <p className={styles.guidePhaseText}>{guide[p.key]}</p>
          </div>
        ))}
      </div>
    </section>
  )
}

function ItemsPanel({ deck, locale }: { deck: MetaDeck; locale: TFTLocale | undefined }) {
  const carries = deck.champions
    .filter((champ) => (champ.recommendedItems?.length ?? 0) > 0)
    .slice(0, 3)
  if (carries.length === 0) return null

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <Swords size={16} />
        <h2>추천 아이템</h2>
        <span className={styles.panelSub}>캐리 유닛별 핵심 3개</span>
      </div>
      <div className={styles.itemList}>
        {carries.map((champ) => (
          <div key={champ.name} className={styles.itemCard}>
            <div className={styles.itemChampCol}>
              <img
                src={champ.imageUrl}
                alt={champ.name}
                className={styles.itemChampImg}
                onError={(e) => { e.currentTarget.style.opacity = '0.3' }}
              />
              <span className={styles.itemChampName}>
                {getChampionName(champ.imageUrl, locale, champ.name)}
              </span>
            </div>
            <div className={styles.coreSection}>
              <span className={styles.itemSectionLabel}>핵심 아이템</span>
              <div className={styles.coreItemRow}>
                {(champ.recommendedItems ?? []).slice(0, 3).map((itemId) => (
                  <div key={itemId} className={styles.coreItemEntry}>
                    <img
                      src={tftItemIconUrl(itemId)}
                      alt={getItemName(itemId, locale)}
                      className={styles.coreItemIcon}
                      onError={tftItemIconOnError}
                    />
                    <span className={styles.coreItemName}>{getItemName(itemId, locale)}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

function HeroAugmentsPanel({ augments }: { augments: HeroAugmentSummary[] }) {
  if (augments.length === 0) return null
  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <Zap size={16} />
        <h2>영웅 증강</h2>
        <span className={styles.panelSub}>해금 증강 효과</span>
      </div>
      <div className={styles.heroAugList}>
        {augments.map((aug) => (
          <div key={`${aug.championId}-${aug.augmentName}`} className={styles.heroAugEntry}>
            <div className={styles.heroAugChamp}>
              {aug.imageUrl && (
                <img
                  src={aug.imageUrl}
                  alt={aug.championName}
                  className={styles.heroAugChampImg}
                  onError={(e) => { e.currentTarget.style.opacity = '0.3' }}
                />
              )}
              <span className={styles.heroAugChampName}>{aug.championName}</span>
            </div>
            <div className={styles.heroAugBadge}>{aug.augmentName}</div>
          </div>
        ))}
      </div>
    </section>
  )
}

const VALID_RANK_FILTERS = ['MASTER_PLUS', 'DIAMOND_PLUS', 'EMERALD_PLUS'] as const
type ValidRankFilter = typeof VALID_RANK_FILTERS[number]

function isValidRankFilter(v: string | undefined): v is ValidRankFilter {
  return VALID_RANK_FILTERS.includes(v as ValidRankFilter)
}

function DeckDetail() {
  const { deckId, rankFilter: rankFilterParam } = useParams<{ deckId: string; rankFilter: string }>()
  const navigate = useNavigate()
  // URL 파라미터 타입 가드 — 잘못된 값이면 기본값으로 fallback
  const rankFilter: ValidRankFilter = isValidRankFilter(rankFilterParam) ? rankFilterParam : 'EMERALD_PLUS'
  const { data: metaDeckResponse, isLoading } = useMetaSnapshot(rankFilter)
  const { data: locale } = useCDragonLocale()
  const metaDecks = metaDeckResponse?.decks ?? []
  const deck = metaDecks.find((d) => String(d.rank) === deckId)

  // ── 레벨 상태 (HexBoard에서 끌어올림) ──────────────────────────
  // deck이 없을 때는 빈 배열, 있을 때는 champions 사용
  const all = useMemo(() => deck?.champions ?? [], [deck?.champions])
  const maxLevel = Math.min(9, all.length || 9)
  const availableLevels = useMemo(
    () => Array.from({ length: Math.max(0, maxLevel - 4) }, (_, i) => i + 5),
    [maxLevel],
  )
  const [level, setLevel] = useState<number>(9)

  // 덱이 바뀌면 (다른 덱 상세 진입 시) 레벨을 maxLevel로 초기화
  useEffect(() => {
    setLevel(maxLevel)
  }, [deck?.rank, maxLevel])

  // 코스트 오름차순 정렬 (빌드업 순서) → 같은 코스트 내에서는 캐리 우선
  // 덕분에 Lv.N 탭에서 항상 N개 표시 가능 (cost 필터 없음)
  const prioritized = useMemo(
    () =>
      [...all].sort((a, b) => {
        const ca = getCost(a, locale), cb = getCost(b, locale)
        if (ca !== cb) return ca - cb           // 저코스트 먼저
        const ac = isCarry(a), bc = isCarry(b)
        return ac === bc ? 0 : ac ? -1 : 1     // 같은 코스트에서 캐리 우선
      }),
    [all, locale],
  )

  // cost 필터 없이 level개만 슬라이스 → Lv.6에서 항상 6개 표시
  const visibleUnits = useMemo(
    () => prioritized.slice(0, level),
    [prioritized, level],
  )

  // 배치판 포지션 맵 (DeckDetail에서 관리 → HexBoard·시너지 공유)
  const customPosMap = useMemo(
    () => parseBoardPositions(deck?.boardPositions, level),
    [deck?.boardPositions, level],
  )

  // 배치판에 실제 올라간 유닛만 → 시너지 계산 소스
  const placedUnits = useMemo(() => {
    if (customPosMap.size === 0) return []
    return visibleUnits.filter((champ) => {
      const apiName = getChampionApiName(champ.imageUrl)
      return apiName ? customPosMap.has(apiName) : false
    })
  }, [visibleUnits, customPosMap])

  // 시너지: 배치된 유닛 기반 재계산 → 레벨 탭 바꾸면 자동 동기화
  const traitCounts = useMemo(
    () => computeTraitCounts(placedUnits, locale),
    [placedUnits, locale],
  )
  const displayTraits = useMemo(() => {
    if (!deck) return []
    // 관리자 배치 미설정 → 덱에 저장된 traits 그대로 표시 (시너지 패널 숨김 방지)
    if (customPosMap.size === 0) return deck.traits
    if (placedUnits.length === 0) return []
    return deck.traits
      .map((t) => ({ ...t, count: traitCounts.get(t.name) ?? 0 }))
      .filter((t) => (traitCounts.get(t.name) ?? 0) >= 2)
      .sort((a, b) => b.count - a.count)
  }, [deck, traitCounts, placedUnits.length, customPosMap.size])

  // ── Early returns (hooks 이후) ───────────────────────────────
  if (isLoading) {
    return (
      <AppLayout>
        <div className={styles.page}>
          <p className={styles.loading}>불러오는 중...</p>
        </div>
      </AppLayout>
    )
  }

  if (!deck) {
    return (
      <AppLayout>
        <div className={styles.page}>
          <button type="button" className={styles.backBtn} onClick={() => navigate('/decks')}>
            <ArrowLeft size={16} /> 덱모음으로
          </button>
          <p className={styles.notFound}>덱 정보를 찾을 수 없어요.</p>
        </div>
      </AppLayout>
    )
  }

  // 덱 이름: 저장된 traits 기준 (레벨과 무관하게 고정)
  const traitName = deck.traits.length > 0 ? getTraitName(deck.traits[0].name, locale) : ''
  const carries = deck.champions
    .filter((c) => (c.recommendedItems?.length ?? 0) > 0)
    .slice(0, 2)
    .map((c) => getChampionShortName(c.imageUrl, locale, c.name))
  const displayName = [traitName, ...carries].filter(Boolean).join(' ') || deck.name

  return (
    <AppLayout>
      <div className={styles.page}>
        <button type="button" className={styles.backBtn} onClick={() => navigate('/decks')}>
          <ArrowLeft size={16} /> 덱모음으로
        </button>

        <div className={styles.header}>
          <TierBadge value={deck.grade} />
          <h1 className={styles.deckName}>{displayName}</h1>
          <span className={styles.rankLabel}>메타 #{deck.rank}</span>
        </div>

        <div className={styles.statsRow}>
          <div className={styles.statItem}>
            <small>TOP 4</small>
            <strong className={styles.cyan}>{deck.top4}</strong>
          </div>
          <div className={styles.statDivider} />
          <div className={styles.statItem}>
            <small>평균 등수</small>
            <strong className={styles.purple}>{deck.avgPlace}등</strong>
          </div>
          <div className={styles.statDivider} />
          <div className={styles.statItem}>
            <small>선택률</small>
            <strong className={styles.gold}>{deck.pickRate}</strong>
          </div>
        </div>

        {/* 배치판: level·visibleUnits를 부모에서 공급 */}
        <HexBoard
          visibleUnits={visibleUnits}
          level={level}
          availableLevels={availableLevels}
          onLevelChange={setLevel}
          locale={locale}
          customPosMap={customPosMap}
        />

        {/* 시너지 구성: 배치판 기반 → 배치 미설정 시 숨김 */}
        {displayTraits.length > 0 && <section className={styles.panel}>
          <div className={styles.panelHead}>
            <Trophy size={16} />
            <h2>시너지 구성</h2>
            <span className={styles.panelSub}>Lv.{level} 기준</span>
          </div>
          <div className={styles.traitList}>
            {displayTraits.map((trait) => {
              const traitDetail = locale?.traitDetailBySuffix.get(trait.name.toLowerCase())
              return (
                <div key={trait.name} className={styles.traitItem}>
                  <TraitHexBadge
                    count={trait.count}
                    iconUrl={trait.iconUrl}
                    name={getTraitName(trait.name, locale)}
                    tone={trait.tone}
                  />
                  <div className={styles.traitInfo}>
                    <span className={styles.traitName}>{getTraitName(trait.name, locale)}</span>
                    {traitDetail && traitDetail.breakpoints.length > 0 && (
                      <div className={styles.breakpoints}>
                        {traitDetail.breakpoints.map((bp) => {
                          const tierClass = BP_TIER_STYLES[bp.tier] ?? ''
                          const activeClass = trait.count >= bp.minUnits ? styles.bpActive : ''
                          return (
                            <span
                              key={bp.minUnits}
                              className={`${styles.bpPip} ${tierClass} ${activeClass}`}
                            >
                              {bp.minUnits}
                            </span>
                          )
                        })}
                      </div>
                    )}
                  </div>
                  <span className={styles.traitCount}>{trait.count}조각</span>
                </div>
              )
            })}
          </div>
        </section>}

        <HeroAugmentsPanel augments={deck.heroAugments ?? []} />

        <PlayGuidePanel deck={deck} />

        <ItemsPanel deck={deck} locale={locale} />
      </div>
    </AppLayout>
  )
}

export default DeckDetail
