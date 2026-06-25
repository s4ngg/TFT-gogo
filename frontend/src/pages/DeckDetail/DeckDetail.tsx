import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import TierBadge from '../../components/common/TierBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import { useCDragonLocale } from '../../hooks/useCDragonLocale'
import { getChampionApiName, getChampionShortName, getTraitName } from '../../api/cdragonLocale'
import HexBoard from './components/HexBoard'
import TraitPanel from './components/TraitPanel'
import HeroAugmentsPanel from './components/HeroAugmentsPanel'
import PlayGuidePanel from './components/PlayGuidePanel'
import ItemsPanel from './components/ItemsPanel'
import { parseBoardPositions } from './utils/boardUtils'
import { isCarry, getCost, computeTraitCounts } from './utils/champUtils'
import type { RankFilter } from '../Dashboard/dashboardData'
import styles from './DeckDetail.module.css'

const VALID_RANK_FILTERS = ['MASTER_PLUS', 'DIAMOND_PLUS', 'EMERALD_PLUS'] as const
type ValidRankFilter = typeof VALID_RANK_FILTERS[number]

function isValidRankFilter(v: string | undefined): v is ValidRankFilter {
  return VALID_RANK_FILTERS.includes(v as ValidRankFilter)
}

function DeckDetail() {
  const { deckId, rankFilter: rankFilterParam } = useParams<{ deckId: string; rankFilter: string }>()
  const navigate = useNavigate()
  const rankFilter: ValidRankFilter = isValidRankFilter(rankFilterParam) ? rankFilterParam : 'EMERALD_PLUS'
  const { data: metaDeckResponse, isLoading, isError } = useMetaSnapshot(rankFilter as RankFilter)
  const { data: locale } = useCDragonLocale()
  const metaDecks = metaDeckResponse?.decks ?? []
  const deck = metaDecks.find((d) => String(d.rank) === deckId)

  const all = useMemo(() => deck?.champions ?? [], [deck?.champions])

  const availableLevels = useMemo(() => {
    if (deck?.boardPositions) {
      try {
        const obj = JSON.parse(deck.boardPositions) as Record<string, unknown>
        return Object.keys(obj)
          .map(Number)
          .filter((n) => n >= 5 && n <= 9)
          .sort((a, b) => a - b)
      } catch {
        // fall through to champion-count based logic
      }
    }
    const maxLevel = Math.min(9, all.length)
    return Array.from({ length: Math.max(0, maxLevel - 4) }, (_, i) => i + 5)
  }, [deck?.boardPositions, all.length])

  const maxLevel = availableLevels.length > 0 ? availableLevels[availableLevels.length - 1] : 5
  const [level, setLevel] = useState<number>(9)

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [deckId])

  useEffect(() => {
    setLevel(maxLevel)
  }, [deck?.rank, maxLevel])

  const prioritized = useMemo(
    () =>
      [...all].sort((a, b) => {
        const ca = getCost(a, locale), cb = getCost(b, locale)
        if (ca !== cb) return ca - cb
        const ac = isCarry(a), bc = isCarry(b)
        return ac === bc ? 0 : ac ? -1 : 1
      }),
    [all, locale],
  )

  const visibleUnits = useMemo(() => prioritized.slice(0, level), [prioritized, level])

  const customPosMap = useMemo(
    () => parseBoardPositions(deck?.boardPositions, level),
    [deck?.boardPositions, level],
  )

  const placedUnits = useMemo(() => {
    if (customPosMap.size === 0) return []
    return visibleUnits.filter((champ) => {
      const apiName = getChampionApiName(champ.imageUrl)
      return apiName ? customPosMap.has(apiName) : false
    })
  }, [visibleUnits, customPosMap])

  const traitCounts = useMemo(
    () => computeTraitCounts(placedUnits, locale),
    [placedUnits, locale],
  )

  const displayTraits = useMemo(() => {
    if (!deck) return []
    if (customPosMap.size === 0) return deck.traits
    if (placedUnits.length === 0) return []
    return deck.traits
      .map((t) => ({ ...t, count: traitCounts.get(t.name) ?? 0 }))
      .filter((t) => (traitCounts.get(t.name) ?? 0) >= 2)
      .sort((a, b) => b.count - a.count)
  }, [deck, traitCounts, placedUnits.length, customPosMap.size])

  if (isLoading) {
    return (
      <AppLayout>
        <div className={styles.page}>
          <p className={styles.loading}>불러오는 중...</p>
        </div>
      </AppLayout>
    )
  }

  if (isError) {
    return (
      <AppLayout>
        <div className={styles.page}>
          <button type="button" className={styles.backBtn} onClick={() => navigate('/decks')}>
            <ArrowLeft size={16} /> 덱모음으로
          </button>
          <p className={styles.notFound}>덱 정보를 불러오는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.</p>
        </div>
      </AppLayout>
    )
  }

  if (!deck) {
    return (
      <AppLayout>
        <div className={styles.page}>
          <button type="button" className={styles.backBtn} onClick={() => navigate(`/decks`)}>
            <ArrowLeft size={16} /> 덱모음으로
          </button>
          <p className={styles.notFound}>덱 정보를 찾을 수 없어요.</p>
        </div>
      </AppLayout>
    )
  }

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

        <HexBoard
          visibleUnits={visibleUnits}
          level={level}
          availableLevels={availableLevels}
          onLevelChange={setLevel}
          locale={locale}
          customPosMap={customPosMap}
        />

        <TraitPanel displayTraits={displayTraits} level={level} locale={locale} />

        <HeroAugmentsPanel augments={deck.heroAugments ?? []} />

        <PlayGuidePanel deck={deck} />

        <ItemsPanel deck={deck} locale={locale} />
      </div>
    </AppLayout>
  )
}

export default DeckDetail
