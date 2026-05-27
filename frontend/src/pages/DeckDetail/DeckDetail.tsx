import { useState } from 'react'
import { ArrowLeft, Map, Trophy, Swords, Sparkles } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import type { MetaDeck } from '../Dashboard/dashboardData'
import { getPositionsForLevel, getFlexPicksForLevel } from './deckPositions'
import type { FlexPick } from './deckPositions'
import { DECK_AUGMENTS } from './deckAugments'
import type { AugmentRec } from './deckAugments'
import { DECK_ITEMS } from './deckItems'
import styles from './DeckDetail.module.css'

const LEVELS = [5, 6, 7, 8, 9, 10] as const
type Level = typeof LEVELS[number]

/* ════════════════════════════
   육각형 배치판
════════════════════════════ */
const BOARD_ROWS = 4
const BOARD_COLS = 7

type DisplayChamp = {
  name: string
  imageUrl: string
  stars?: 1 | 2 | 3
  items?: { imageUrl: string; name: string }[]
}

function HexBoard({ deck }: { deck: MetaDeck }) {
  const [level, setLevel] = useState<Level>(9)
  const positions = getPositionsForLevel(deck.rank, level)
  const flexPicks = getFlexPicksForLevel(deck.rank, level)
  const hasPositions = Object.keys(positions).length > 0

  if (!hasPositions) return null

  function champAt(row: number, col: number): DisplayChamp | undefined {
    const core = deck.champions.find((c) => {
      const pos = positions[c.name]
      return pos?.[0] === row && pos?.[1] === col
    })
    if (core) return core
    return flexPicks.find((f: FlexPick) => f.position[0] === row && f.position[1] === col)
  }

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <Map size={16} />
        <h2>추천 배치</h2>
      </div>
      <div className={styles.boardWrap}>
        <div className={styles.board}>
          {Array.from({ length: BOARD_ROWS }, (_, vi) => {
            const row = BOARD_ROWS - 1 - vi
            const isOffset = row % 2 !== 0
            return (
              <div key={row} className={`${styles.boardRow} ${isOffset ? styles.boardRowOffset : ''}`}>
                {Array.from({ length: BOARD_COLS }, (_, col) => {
                  const champ = champAt(row, col)
                  return (
                    <div key={col} className={styles.hexCell}>
                      <div className={`${styles.hexOuter} ${champ ? styles.hexFilled : styles.hexEmpty}`}>
                        {champ && (
                          <>
                            <img src={champ.imageUrl} alt={champ.name} className={styles.hexImg} />
                            <span className={styles.hexStars}>{'★'.repeat(champ.stars ?? 2)}</span>
                          </>
                        )}
                      </div>
                      {champ && champ.items && champ.items.length > 0 && (
                        <div className={styles.hexItems}>
                          {champ.items.slice(0, 3).map((item) => (
                            <img key={item.name} src={item.imageUrl} alt={item.name} className={styles.hexItemIcon} />
                          ))}
                        </div>
                      )}
                      {champ && <span className={styles.hexName}>{champ.name}</span>}
                    </div>
                  )
                })}
              </div>
            )
          })}
        </div>
        {/* 레벨 탭 */}
        <div className={styles.levelTabs}>
          {LEVELS.map((lv) => (
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
      </div>
    </section>
  )
}

/* ════════════════════════════
   추천 아이템
════════════════════════════ */
function ItemsPanel({ deck }: { deck: MetaDeck }) {
  const carryRecs = DECK_ITEMS[deck.rank] ?? []
  if (carryRecs.length === 0) return null

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <Swords size={16} />
        <h2>추천 아이템</h2>
        <span className={styles.panelSub}>캐리 기물 기준</span>
      </div>
      <div className={styles.itemList}>
        {carryRecs.map((rec) => {
          const champData = deck.champions.find((c) => c.name === rec.champName)
          return (
            <div key={rec.champName} className={styles.itemCard}>
              {/* 챔피언 */}
              <div className={styles.itemChampCol}>
                <div className={styles.itemChampImgWrap}>
                  <img src={champData?.imageUrl ?? ''} alt={rec.champName} className={styles.itemChampImg} />
                  <span className={styles.itemChampStars}>{'★'.repeat(rec.stars)}</span>
                </div>
                <span className={styles.itemChampName}>{rec.champName}</span>
              </div>

              {/* 추천 삼신기 */}
              <div className={styles.coreSection}>
                <span className={styles.itemSectionLabel}>추천 삼신기</span>
                <div className={styles.coreItemRow}>
                  {rec.coreItems.map((item) => (
                    <div key={item.name} className={styles.coreItemEntry}>
                      <img src={item.imageUrl} alt={item.name} className={styles.coreItemIcon} />
                      <span className={styles.coreItemName}>{item.name}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* 대체 아이템 */}
              <div className={styles.altSection}>
                <span className={styles.itemSectionLabel}>대체 아이템</span>
                <div className={styles.altSlots}>
                  {rec.alternatives.map((alts, slotIdx) => (
                    <div key={slotIdx} className={styles.altSlot}>
                      <span className={styles.altSlotLabel}>슬롯 {slotIdx + 1}</span>
                      <div className={styles.altSlotIcons}>
                        {alts.map((alt) => (
                          <img
                            key={alt.name}
                            src={alt.imageUrl}
                            alt={alt.name}
                            title={alt.name}
                            className={styles.altItemIcon}
                          />
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )
        })}
      </div>
    </section>
  )
}

/* ════════════════════════════
   추천 증강체
════════════════════════════ */
const TIER_LABEL: Record<AugmentRec['tier'], string> = {
  prismatic: '프리즈매틱',
  gold: '금빛',
  silver: '은빛',
}
const TIER_CLASS: Record<AugmentRec['tier'], string> = {
  prismatic: styles.augTierPrismatic,
  gold: styles.augTierGold,
  silver: styles.augTierSilver,
}

function AugmentsPanel({ deck }: { deck: MetaDeck }) {
  const augments = DECK_AUGMENTS[deck.rank] ?? []

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <Sparkles size={16} />
        <h2>추천 증강체</h2>
      </div>
      <div className={styles.augList}>
        {augments.map((aug) => (
          <div key={aug.name} className={styles.augEntry}>
            <span className={`${styles.augTier} ${TIER_CLASS[aug.tier]}`}>
              {TIER_LABEL[aug.tier]}
            </span>
            <div className={styles.augContent}>
              <span className={styles.augEntryName}>{aug.name}</span>
              <span className={styles.augEntryDesc}>{aug.description}</span>
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
  const { deckId } = useParams<{ deckId: string }>()
  const navigate = useNavigate()
  const { data: metaDecks = [], isLoading } = useMetaSnapshot()

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
          <h1 className={styles.deckName}>{deck.name}</h1>
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

        {/* 배치판 */}
        <HexBoard deck={deck} />

        {/* 시너지 */}
        <section className={styles.panel}>
          <div className={styles.panelHead}>
            <Trophy size={16} />
            <h2>시너지 구성</h2>
          </div>
          <div className={styles.traitList}>
            {deck.traits.map((t) => (
              <div key={t.name} className={styles.traitItem}>
                <TraitHexBadge count={t.count} iconUrl={t.iconUrl} name={t.name} tone={t.tone} />
                <span className={styles.traitName}>{t.name}</span>
                <span className={styles.traitCount}>{t.count}조각</span>
              </div>
            ))}
          </div>
        </section>

        {/* 추천 아이템 */}
        <ItemsPanel deck={deck} />

        {/* 추천 증강체 */}
        <AugmentsPanel deck={deck} />

      </div>
    </AppLayout>
  )
}

export default DeckDetail
