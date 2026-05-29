import { useMemo, useState } from 'react'
import { ArrowLeft, Map, Swords, Sparkles, Trophy } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import { useCDragonLocale } from '../../hooks/useCDragonLocale'
import { getAugmentName, getChampionDetail, getChampionName, getChampionShortName, getItemName, getTraitName } from '../../api/cdragonLocale'
import type { TFTLocale } from '../../api/cdragonLocale'
import { tftItemIconUrl } from '../../api/communityDragonAssets'
import type { ChampionSummary, MetaDeck } from '../Dashboard/dashboardData'
import styles from './DeckDetail.module.css'

const BOARD_ROWS = 4
const BOARD_COLS = 7

function isCarry(champ: ChampionSummary): boolean {
  return (champ.recommendedItems?.length ?? 0) > 0
}

function getCost(champ: ChampionSummary, locale: TFTLocale | undefined): number {
  return getChampionDetail(champ.imageUrl, locale)?.cost ?? champ.cost ?? 0
}

function costLimitForLevel(level: number): number {
  if (level <= 6) return 3
  if (level <= 8) return 4
  return 5
}

function canUseAtLevel(champ: ChampionSummary, level: number, locale: TFTLocale | undefined): boolean {
  const cost = getCost(champ, locale)
  return cost > 0 && cost <= costLimitForLevel(level)
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

function HexBoard({ deck, locale }: { deck: MetaDeck; locale: TFTLocale | undefined }) {
  const all = deck.champions

  const prioritized = useMemo(() => {
    return [...all].sort((a, b) => {
      const ac = isCarry(a)
      const bc = isCarry(b)
      if (ac !== bc) return bc ? 1 : -1
      return getCost(b, locale) - getCost(a, locale)
    })
  }, [all, locale])

  const maxLevel = Math.min(9, all.length)
  const availableLevels = useMemo(
    () => Array.from({ length: Math.max(0, maxLevel - 4) }, (_, i) => i + 5),
    [maxLevel],
  )
  const [level, setLevel] = useState<number>(maxLevel)

  const visibleUnits = useMemo(
    () => prioritized.filter((champ) => canUseAtLevel(champ, level, locale)).slice(0, level),
    [prioritized, level, locale],
  )

  const placed = useMemo(
    () => buildBoardPositions(visibleUnits, locale),
    [visibleUnits, locale],
  )

  if (all.length === 0) return null

  function champAt(row: number, col: number): PlacedChamp | undefined {
    return placed.find((p) => p.row === row && p.col === col)
  }

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <Map size={16} />
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
                    ? (p.champ.recommendedItems ?? []).slice(0, 3).map((id) => tftItemIconUrl(id))
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
                              onError={(e) => { e.currentTarget.style.opacity = '0' }}
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
                onClick={() => setLevel(lv)}
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

function AugmentsPanel({ deck, locale }: { deck: MetaDeck; locale: TFTLocale | undefined }) {
  const augments = deck.topAugments ?? []
  if (augments.length === 0) return null

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <Sparkles size={16} />
        <h2>추천 증강체</h2>
        <span className={styles.panelSub}>승률 기준 상위 증강</span>
      </div>
      <div className={styles.augList}>
        {augments.map((aug) => (
          <div key={aug.augmentId} className={styles.augEntry}>
            <span className={`${styles.augTier} ${aug.isRecommended ? styles.augTierGold : styles.augTierSilver}`}>
              {aug.isRecommended ? '추천' : '참고'}
            </span>
            <div className={styles.augContent}>
              <span className={styles.augEntryName}>
                {getAugmentName(aug.augmentId, locale)}
              </span>
              <span className={styles.augEntryDesc}>승률 {aug.winRate}</span>
            </div>
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
                      onError={(e) => { e.currentTarget.style.opacity = '0.3' }}
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

function DeckDetail() {
  const { deckId } = useParams<{ deckId: string }>()
  const navigate = useNavigate()
  const { data: metaDeckResponse, isLoading } = useMetaSnapshot()
  const { data: locale } = useCDragonLocale()
  const metaDecks = metaDeckResponse?.decks ?? []
  const deck = metaDecks.find((d) => String(d.rank) === deckId)

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
            <small>승률</small>
            <strong className={styles.green}>{deck.winRate}</strong>
          </div>
          <div className={styles.statDivider} />
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

        <HexBoard deck={deck} locale={locale} />

        <section className={styles.panel}>
          <div className={styles.panelHead}>
            <Trophy size={16} />
            <h2>시너지 구성</h2>
          </div>
          <div className={styles.traitList}>
            {deck.traits.map((trait) => (
              <div key={trait.name} className={styles.traitItem}>
                <TraitHexBadge
                  count={trait.count}
                  iconUrl={trait.iconUrl}
                  name={getTraitName(trait.name, locale)}
                  tone={trait.tone}
                />
                <span className={styles.traitName}>{getTraitName(trait.name, locale)}</span>
                <span className={styles.traitCount}>{trait.count}조각</span>
              </div>
            ))}
          </div>
        </section>

        <ItemsPanel deck={deck} locale={locale} />
        <AugmentsPanel deck={deck} locale={locale} />
      </div>
    </AppLayout>
  )
}

export default DeckDetail
