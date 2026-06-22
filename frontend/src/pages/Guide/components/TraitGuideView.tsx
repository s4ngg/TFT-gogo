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
  GuidePagination,
  GuideStatusBanner,
  LinkedChampionMini,
} from './GuideShared'
import styles from '../Guide.module.css'

interface TraitGuideViewProps {
  fallbackData: GuideCatalog
  onChampionSelect: (championName: string) => void
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

function TraitGuideView({
  fallbackData,
  onChampionSelect,
  query,
}: TraitGuideViewProps) {
  const {
    currentPage,
    setCurrentPage,
  } = useGuideTabPagination({ resetKey: query })
  const traitsQuery = useGuideTabItems({
    fallbackData,
    params: {
      page: currentPage,
      pageSize: TRAIT_PAGE_SIZE,
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

  return (
    <>
      <GuideStatusBanner
        isFallbackData={traitsQuery.data.source === 'fallback' && !traitsQuery.isFetching}
        isFetching={traitsQuery.isFetching}
        onRetry={() => {
          void traitsQuery.refetch()
        }}
      />
      <section className={styles.traitGrid}>
        {visibleTraits.length === 0 && <EmptyState />}
        {visibleTraits.map((traitGuide) => {
          const traitDisplay = getTraitDisplay(traitGuide)

          return (
            <article className={styles.traitCard} key={traitGuide.name}>
              <div className={styles.traitTop}>
                <TraitHexBadge
                  count={traitGuide.count}
                  iconUrl={traitGuide.iconUrl}
                  name={traitGuide.name}
                  tone={traitGuide.tone}
                />
                <div className={styles.traitTitle}>
                  <h2>{traitGuide.name}</h2>
                  <span>{traitGuide.type}</span>
                </div>
                <div className={styles.levelTrack}>
                  {traitGuide.levels.map((level) => (
                    <b
                      className={level.replace(/\+$/, '') === String(traitGuide.count) ? styles.levelActive : ''}
                      key={level}
                    >
                      {level}
                    </b>
                  ))}
                </div>
              </div>
              {traitDisplay.summary && <p className={styles.traitSummary}>{traitDisplay.summary}</p>}
              {traitDisplay.tierEffects.length > 0 && (
                <div className={styles.traitEffectList} aria-label={`${traitGuide.name} 단계별 효과`}>
                  <div className={styles.traitEffectHeader}>
                    <strong>단계별 효과</strong>
                    <span>{traitDisplay.tierEffects.length}단계</span>
                  </div>
                  {traitDisplay.tierEffects.map((tierEffect) => (
                    <div
                      aria-label={`${tierEffect.level}단계 ${tierEffect.description}`}
                      className={styles.traitEffectRow}
                      key={`${tierEffect.level}-${tierEffect.description}`}
                    >
                      <span className={styles.traitEffectLevel}>{tierEffect.level}</span>
                      <span>{tierEffect.description}</span>
                    </div>
                  ))}
                </div>
              )}
              <div className={styles.championLine}>
                {traitGuide.champions.map((championRef) => (
                  <LinkedChampionMini champion={championRef} key={championRef.name} onSelect={onChampionSelect} />
                ))}
              </div>
              <div className={styles.tipLine}>
                {traitGuide.tips.map((tip) => (
                  <span key={tip}>{tip}</span>
                ))}
              </div>
            </article>
          )
        })}
      </section>
      <GuidePagination currentPage={safePage} onPageChange={setCurrentPage} totalPages={pageData.totalPages} />
    </>
  )
}

export default TraitGuideView
