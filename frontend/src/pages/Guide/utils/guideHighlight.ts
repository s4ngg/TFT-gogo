import type { GuideTab, RecentGuide } from '../../../api/guide'

export interface GuideHighlightCandidate {
  name: string
  targetKey?: string | null
}

export type HighlightedGuide = RecentGuide & {
  targetKey?: string | null
}

interface GuideHighlightAttrs {
  className: string
  'data-guide-highlighted'?: 'true'
}

function normalizeGuideHighlightValue(value?: string | null) {
  return value?.trim().toLowerCase() ?? ''
}

export function isGuideHighlighted(
  tab: GuideTab,
  guide: GuideHighlightCandidate,
  highlightedGuide?: HighlightedGuide | null,
) {
  if (!highlightedGuide || highlightedGuide.tab !== tab) {
    return false
  }

  const highlightedTargetKey = normalizeGuideHighlightValue(highlightedGuide.targetKey)
  const guideTargetKey = normalizeGuideHighlightValue(guide.targetKey)
  if (highlightedTargetKey) {
    return guideTargetKey === highlightedTargetKey
  }

  const highlightValues = [
    normalizeGuideHighlightValue(highlightedGuide.query),
    normalizeGuideHighlightValue(highlightedGuide.label),
  ].filter(Boolean)
  const guideValues = [
    normalizeGuideHighlightValue(guide.name),
    normalizeGuideHighlightValue(guide.targetKey),
  ].filter(Boolean)

  return highlightValues.some((highlightValue) => guideValues.includes(highlightValue))
}

export function getGuideHighlightWatchKey(guides: GuideHighlightCandidate[]) {
  return guides
    .map((guide) => `${guide.targetKey ?? ''}:${guide.name}`)
    .join('|')
}

export function getGuideHighlightAttrs(
  isHighlighted: boolean,
  baseClassName: string,
  highlightedClassName: string,
): GuideHighlightAttrs {
  return {
    className: `${baseClassName} ${isHighlighted ? highlightedClassName : ''}`.trim(),
    'data-guide-highlighted': isHighlighted ? 'true' : undefined,
  }
}
