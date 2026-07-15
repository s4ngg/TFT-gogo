import { Bot } from 'lucide-react'
import { useEffect, useRef } from 'react'
import {
  TRAIT_PAGE_SIZE,
  type GuideCatalog,
  type TraitGuide,
  type TraitTierEffect,
} from '../../../api/guide'
import TraitHexBadge from '../../../components/common/TraitHexBadge'
import { useGuideTabItems } from '../../../hooks/useGuide'
import {
  useGuidePageBounds,
  useGuideTabPagination,
} from '../hooks/useGuideTabPagination'
import {
  EmptyState,
  GuideAssetImage,
  GuidePagination,
  GuideStatusBanner,
  LinkedChampionMini,
} from './GuideShared'
import {
  createGameGuideAiRef,
  type GameGuideAiAskHandler,
} from '../utils/gameGuideAiRefs'
import {
  getGuideHighlightAttrs,
  getGuideHighlightWatchKey,
  type HighlightedGuide,
  isGuideHighlighted,
} from '../utils/guideHighlight'
import { useGuideHighlightScroll } from '../hooks/useGuideHighlightScroll'
import styles from '../Guide.module.css'

interface TraitGuideViewProps {
  fallbackData: GuideCatalog
  highlightedGuide: HighlightedGuide | null
  isGuideFallbackData: boolean
  isGuideFetching: boolean
  onGameGuideAiAsk: GameGuideAiAskHandler
  onChampionSelect: (championName: string) => void
  onGuideRetry: () => void
  onVisibleItemsChange: (items: TraitGuide[]) => void
  patchVersion: string
  query: string
}

const INLINE_TIER_EFFECT_PATTERN = /\((\d+\+?)\)\s*/g
const METRIC_ONLY_PATTERN = /^[+-]?\d[\d,./%\s+-]*$/
const TRAIT_METRIC_LABELS = [
  '공격 속도',
  '마법 저항력',
  '방어력',
  '공격력',
  '주문력',
  '체력',
  '마나',
  '치명타',
  '피해',
  '골드',
]

function inferMetricLabel(summary: string) {
  return TRAIT_METRIC_LABELS.find((label) => summary.includes(label)) ?? ''
}

function normalizeInlineTierEffectDescription(description: string, summary: string) {
  const metricLabel = inferMetricLabel(summary)
  if (!metricLabel || !METRIC_ONLY_PATTERN.test(description) || description.startsWith(metricLabel)) {
    return description
  }
  return `${metricLabel} ${description}`
}

function formatTraitLevel(level: string) {
  const match = level.trim().match(/^(\d+)(\+?)$/)
  if (!match) return level
  return `${match[1]}명${match[2]}`
}

function getChampionSectionMeta(championCount: number) {
  if (championCount === 0) return '특수 조건'
  return `${championCount}명`
}

function splitInlineTierEffects(summary: string) {
  const matches = [...summary.matchAll(INLINE_TIER_EFFECT_PATTERN)]
  if (matches.length === 0) return { summary, tierEffects: [] }

  const tierEffects: TraitTierEffect[] = []
  const baseSummary = summary.slice(0, matches[0].index).trim()

  matches.forEach((match, index) => {
    const startIndex = (match.index ?? 0) + match[0].length
    const endIndex = matches[index + 1]?.index ?? summary.length
    const description = normalizeInlineTierEffectDescription(
      summary.slice(startIndex, endIndex).trim(),
      baseSummary,
    )
    if (description) {
      tierEffects.push({
        description,
        level: match[1],
      })
    }
  })

  return { summary: baseSummary, tierEffects }
}

function getTraitDisplay(traitGuide: TraitGuide) {
  const tierEffects = traitGuide.tierEffects ?? []
  if (tierEffects.length > 0) {
    return { summary: traitGuide.summary, tierEffects }
  }
  return splitInlineTierEffects(traitGuide.summary)
}

function getTraitCardKey(traitGuide: TraitGuide) {
  return [
    traitGuide.name,
    traitGuide.variant,
    traitGuide.levels.join('/'),
  ].filter(Boolean).join('-')
}

function getTraitSummaryLabel(traitGuide: TraitGuide) {
  return traitGuide.variant ? `${traitGuide.variant} 별자리 효과` : '핵심 효과'
}

function TraitGuideView({
  fallbackData,
  highlightedGuide,
  isGuideFallbackData,
  isGuideFetching,
  onGameGuideAiAsk,
  onChampionSelect,
  onGuideRetry,
  onVisibleItemsChange,
  patchVersion,
  query,
}: TraitGuideViewProps) {
  const traitGridRef = useRef<HTMLElement>(null)
  const {
    currentPage,
    setCurrentPage,
  } = useGuideTabPagination({ resetKey: query })
  const traitsQuery = useGuideTabItems({
    fallbackData,
    params: {
      page: currentPage,
      pageSize: TRAIT_PAGE_SIZE,
      patchVersion,
      query,
      tab: 'traits',
    },
  })
  const pageData = traitsQuery.data.data
  const safePage = useGuidePageBounds({
    currentPage,
    setCurrentPage,
    totalPages: pageData.totalPages,
  })
  const visibleTraits = pageData.items
  const isUnavailableData = traitsQuery.data.source === 'unavailable' && !traitsQuery.isFetching
  const highlightWatchKey = getGuideHighlightWatchKey(visibleTraits)

  useGuideHighlightScroll(traitGridRef, 'traits', highlightedGuide, highlightWatchKey)

  useEffect(() => {
    onVisibleItemsChange(visibleTraits)
  }, [onVisibleItemsChange, visibleTraits])

  return (
    <>
      <GuideStatusBanner
        isFallbackData={!isUnavailableData && (isGuideFallbackData || (traitsQuery.data.source === 'fallback' && !traitsQuery.isFetching))}
        isFetching={isGuideFetching || traitsQuery.isFetching}
        isUnavailableData={isUnavailableData}
        onRetry={() => {
          onGuideRetry()
          void traitsQuery.refetch()
        }}
        patchVersion={traitsQuery.data.patchVersion || patchVersion}
      />
      <section className={styles.traitGrid} ref={traitGridRef}>
        {visibleTraits.length === 0 && <EmptyState />}
        {visibleTraits.map((traitGuide) => {
          const traitDisplay = getTraitDisplay(traitGuide)
          const specialUnits = traitGuide.specialUnits ?? []
          const isHighlighted = isGuideHighlighted('traits', traitGuide, highlightedGuide)
          const aiRef = createGameGuideAiRef('TRAIT', traitGuide.name, traitGuide.targetKey)

          return (
            <article
              {...getGuideHighlightAttrs(isHighlighted, styles.traitCard, styles.guideHighlighted)}
              key={getTraitCardKey(traitGuide)}
            >
              <div className={styles.traitTop}>
                <TraitHexBadge
                  count={traitGuide.count}
                  iconUrl={traitGuide.iconUrl}
                  name={traitGuide.name}
                  showCount={false}
                  tone={traitGuide.tone}
                />
                <div className={styles.traitTitle}>
                  <h2>{traitGuide.name}</h2>
                  <span>{traitGuide.type}</span>
                  {traitGuide.variant && (
                    <strong className={styles.traitVariant}>{traitGuide.variant} 별자리</strong>
                  )}
                </div>
                <div className={styles.levelTrackWrap}>
                  <span>효과 단계</span>
                  <div className={styles.levelTrack} aria-label={`${traitGuide.name} 효과 단계`}>
                    {traitGuide.levels.map((level) => (
                      <b
                        aria-label={`${formatTraitLevel(level)} 효과 단계`}
                        key={level}
                      >
                        {formatTraitLevel(level)}
                      </b>
                    ))}
                  </div>
                </div>
                {aiRef && (
                  <button
                    aria-label={`${traitGuide.name} AI 질문`}
                    className={styles.gameGuideAiCardButton}
                    onClick={() => onGameGuideAiAsk(aiRef)}
                    title="AI에게 물어보기"
                    type="button"
                  >
                    <Bot size={14} />
                  </button>
                )}
              </div>
              {traitDisplay.summary && (
                <div className={styles.traitSummaryPanel}>
                  <span>{getTraitSummaryLabel(traitGuide)}</span>
                  <p>{traitDisplay.summary}</p>
                </div>
              )}
              {traitDisplay.tierEffects.length > 0 && (
                <div className={styles.traitEffectList} aria-label={`${traitGuide.name} 단계별 효과`}>
                  <div className={styles.traitEffectHeader}>
                    <strong>단계별 효과</strong>
                    <span>{traitDisplay.tierEffects.length}단계</span>
                  </div>
                  {traitDisplay.tierEffects.map((tierEffect) => (
                    <div
                      aria-label={`${formatTraitLevel(tierEffect.level)} 단계 ${tierEffect.description}`}
                      className={styles.traitEffectRow}
                      key={`${tierEffect.level}-${tierEffect.description}`}
                    >
                      <span className={styles.traitEffectLevel}>{formatTraitLevel(tierEffect.level)}</span>
                      <span>{tierEffect.description}</span>
                    </div>
                  ))}
                </div>
              )}
              <div className={styles.traitSection}>
                <div className={styles.traitSectionHeader}>
                  <strong>소속 챔피언</strong>
                  <span>{getChampionSectionMeta(traitGuide.champions.length)}</span>
                </div>
                <div className={styles.championLine}>
                  {traitGuide.champions.length > 0
                    ? traitGuide.champions.map((championRef) => (
                      <LinkedChampionMini champion={championRef} key={championRef.name} onSelect={onChampionSelect} />
                    ))
                    : <span className={styles.traitEmptyMeta}>챔피언 조합보다 특수 조건으로 활성화되는 시너지입니다.</span>
                  }
                </div>
              </div>
              {specialUnits.length > 0 && (
                <div className={styles.traitSection}>
                  <div className={styles.traitSectionHeader}>
                    <strong>소환 유닛</strong>
                    <span>{specialUnits.length}개</span>
                  </div>
                  <div className={styles.specialUnitLine} aria-label={`${traitGuide.name} 소환 유닛`}>
                    {specialUnits.map((specialUnit) => (
                      <span className={styles.specialUnitMini} key={specialUnit.name}>
                        <GuideAssetImage
                          alt={specialUnit.name}
                          fallbackLabel={specialUnit.name}
                          imageUrl={specialUnit.imageUrl}
                        />
                        <span>{specialUnit.name}</span>
                        {specialUnit.note && <small>{specialUnit.note}</small>}
                      </span>
                    ))}
                  </div>
                </div>
              )}
              {traitGuide.tips.length > 0 && (
                <div className={styles.traitSection}>
                  <div className={styles.traitSectionHeader}>
                    <strong>운영 팁</strong>
                    <span>{traitGuide.tips.length}개</span>
                  </div>
                  <div className={styles.tipLine}>
                    {traitGuide.tips.map((tip) => (
                      <span key={tip}>{tip}</span>
                    ))}
                  </div>
                </div>
              )}
            </article>
          )
        })}
      </section>
      <GuidePagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />
    </>
  )
}

export default TraitGuideView
