import type { GuideTab, RecentGuide } from '../../../api/guide'

export interface GuideHighlightCandidate {
  name: string
  targetKey?: string | null
}

function normalizeGuideHighlightValue(value?: string | null) {
  return value?.trim().toLowerCase() ?? ''
}

export function isGuideHighlighted(
  tab: GuideTab,
  guide: GuideHighlightCandidate,
  highlightedGuide?: RecentGuide | null,
) {
  if (!highlightedGuide || highlightedGuide.tab !== tab) {
    return false
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
