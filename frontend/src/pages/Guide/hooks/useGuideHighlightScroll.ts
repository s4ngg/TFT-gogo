import { useEffect, type RefObject } from 'react'
import type { GuideTab } from '../../../api/guide'
import type { HighlightedGuide } from '../utils/guideHighlight'

const GUIDE_HIGHLIGHT_SELECTOR = '[data-guide-highlighted="true"]'

export function useGuideHighlightScroll(
  containerRef: RefObject<HTMLElement>,
  tab: GuideTab,
  highlightedGuide: HighlightedGuide | null | undefined,
  watchKey: string,
) {
  useEffect(() => {
    if (!highlightedGuide || highlightedGuide.tab !== tab) {
      return
    }

    const highlightedElement = containerRef.current?.querySelector<HTMLElement>(GUIDE_HIGHLIGHT_SELECTOR)
    highlightedElement?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, [containerRef, highlightedGuide, tab, watchKey])
}
