import { useMemo, useState } from 'react'
import { ArrowLeft, Map, Trophy, Swords, Sparkles } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import { useCDragonLocale } from '../../hooks/useCDragonLocale'
import { getChampionName, getTraitName, getAugmentName } from '../../api/cdragonLocale'
import type { TFTLocale } from '../../api/cdragonLocale'
import { tftItemIconUrl } from '../../api/communityDragonAssets'
import type { MetaDeck, ChampionSummary } from '../Dashboard/dashboardData'
import styles from './DeckDetail.module.css'

/* ════════════════════════════
   육각형 배치판 (동적)
   - 탱커(아이템 없음)  → rows 2,3  (시각적 상단)
   - 딜러/캐리(아이템 있음) → rows 0,1  (시각적 하단)
   - 레벨 탭: Lv.N 선택 시 N개 유닛 표시
════════════════════════════ */
const BOARD_ROWS = 4
const BOARD_COLS = 7

function isCarry(champ: ChampionSummary): boolean {
  return (champ.recommendedItems?.length ?? 0) > 0
}

interface PlacedChamp {
  champ: ChampionSummary
  row: number
  col: number
}

function buildBoardPositions(visible: ChampionSummary[]): PlacedChamp[] {
  const tanks   = visible.filter((c) => !isCarry(c))
  const carries = visible.filter((c) => isCarry(c))

  const result: PlacedChamp[] = []

  // 탱커 → rows 3,2 (상단)
  placeLine(tanks, [3, 2], result)
  // 딜러 → rows 0,1 (하단)
  placeLine(carries, [0, 1], result)

  return result
}

function placeLine(units: ChampionSummary[], rows: number[], result: PlacedChamp[]) {
  if (units.length === 0) return
  const rowCount = rows.length
  const perRow   = Math.ceil(units.length / rowCount)
  let placed = 0
  for (const row of rows) {
    const chunk = units.slice(placed, placed + perRow)
    if (chunk.length === 0) break
    const startCol = Math.floor((BOARD_COLS - chunk.length) / 2)
    chunk.forEach((champ, i) => result.push({ champ, row, col: startCol + i }))
    placed += chunk.length
  }
}

function HexBoard({ deck, locale }: { deck: MetaDeck; locale: TFTLocale | undefined }) {
  const all = deck.champions

  // 우선순위 정렬: 캐리(아이템 있음) → 코스트 내림차순 → 레벨 선택 시 상위 N개 표시
  const prioritized = useMemo(() => {
    return [...all].sort((a, b) => {
      const ac = isCarry(a), bc = isCarry(b)
      if (ac !== bc) return bc ? 1 : -1
      return (b.cost ?? 0) - (a.cost ?? 0)
    })
  }, [all])

  const maxLevel = Math.min(9, all.length)
  const availableLevels = useMemo(
    () => Array.from({ length: Math.max(0, maxLevel - 4) }, (_, i) => i + 5),
    [maxLevel],
  )
  const [level, setLevel] = useState<number>(maxLevel)

  const placed = useMemo(
    () => buildBoardPositions(prioritized.slice(0, level)),
    [prioritized, level],
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
        <span className={styles.panelSub}>위 2줄 탱커 · 아래 2줄 딜러</span>
      </div>
      <div className={styles.boardWrap}>
        <div className={styles.board}>
          {Array.from({ length: BOARD_ROWS }, (_, vi) => {
            const row      = BOARD_ROWS - 1 - vi
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
                            <img key={idx} src={url} alt="" className={styles.hexItemIcon}
                              onError={(e) => { (e.currentTarget as HTMLImageElement).style.opacity = '0' }} />
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

/* ════════════════════════════
   추천 증강체 (API 실데이터)
════════════════════════════ */
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

/* ════════════════════════════
   추천 아이템 (API 실데이터)
════════════════════════════ */
function ItemsPanel({ deck }: { deck: MetaDeck }) {
  const items = deck.topItems ?? []
  if (items.length === 0) return null

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <Swords size={16} />
        <h2>추천 아이템</h2>
        <span className={styles.panelSub}>등수 향상 효과 기준</span>
      </div>
      <div className={styles.itemList}>
        {items.map((item) => (
          <div key={item.itemId} className={styles.itemCard}>
            <div className={styles.itemChampCol}>
              <img
                src={tftItemIconUrl(item.itemId)}
                alt={item.itemName}
                className={styles.itemChampImg}
                onError={(e) => { (e.currentTarget as HTMLImageElement).style.opacity = '0.3' }}
              />
              <span className={styles.itemChampName}>{item.itemName}</span>
            </div>
            <div className={styles.coreSection}>
              <span className={styles.itemSectionLabel}>승률</span>
              <span style={{ color: '#04ede0', fontWeight: 700 }}>{item.winRate}</span>
            </div>
            <div className={styles.altSection}>
              <span className={styles.itemSectionLabel}>픽률</span>
              <span style={{ color: '#8e97a1' }}>{item.playRate}</span>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

/* ════════════════════════════
   메인
════════════════════════════ */
function DeckDetail() {
  const { deckId }  = useParams<{ deckId: string }>()
  const navigate    = useNavigate()
  const { data: metaDeckResponse, isLoading } = useMetaSnapshot()
  const { data: locale } = useCDragonLocale()
  const metaDecks   = metaDeckResponse?.decks ?? []

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

  // 덱 표시명: 상위 2 트레잇을 한국어로 변환
  const displayName = deck.traits.length > 0
    ? deck.traits.slice(0, 2).map((t) => getTraitName(t.name, locale)).join(' ')
    : deck.name

  return (
    <AppLayout>
      <div className={styles.page}>

        {/* 뒤로가기 */}
        <button type="button" className={styles.backBtn} onClick={() => navigate('/decks')}>
          <ArrowLeft size={16} /> 덱모음으로
        </button>

        {/* 헤더 */}
        <div className={styles.header}>
          <TierBadge value={deck.grade} />
          <h1 className={styles.deckName}>{displayName}</h1>
          <span className={styles.rankLabel}>메타 #{deck.rank}</span>
        </div>

        {/* 스탯 */}
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
            <small>픽률</small>
            <strong className={styles.gold}>{deck.pickRate}</strong>
          </div>
        </div>

        {/* 배치판 (동적) */}
        <HexBoard deck={deck} locale={locale} />

        {/* 시너지 */}
        <section className={styles.panel}>
          <div className={styles.panelHead}>
            <Trophy size={16} />
            <h2>시너지 구성</h2>
          </div>
          <div className={styles.traitList}>
            {deck.traits.map((t) => (
              <div key={t.name} className={styles.traitItem}>
                <TraitHexBadge count={t.count} iconUrl={t.iconUrl} name={getTraitName(t.name, locale)} tone={t.tone} />
                <span className={styles.traitName}>{getTraitName(t.name, locale)}</span>
                <span className={styles.traitCount}>{t.count}조각</span>
              </div>
            ))}
          </div>
        </section>

        {/* 추천 아이템 */}
        <ItemsPanel deck={deck} />

        {/* 추천 증강체 */}
        <AugmentsPanel deck={deck} locale={locale} />

      </div>
    </AppLayout>
  )
}

export default DeckDetail
