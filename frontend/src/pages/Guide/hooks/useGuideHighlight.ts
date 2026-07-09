import { useCallback, useEffect, useRef, useState } from 'react'
import type { GuideTab } from '../../../api/guide'
import type { HighlightedGuide } from '../utils/guideHighlight'

// Keep this timeout aligned with --guide-highlight-pulse-duration (1.3s) * 2 in variables.css.
const GUIDE_HIGHLIGHT_DURATION_MS = 2600

interface UseGuideHighlightParams {
  onJump: (tab: GuideTab, query: string, label?: string) => void
}

export function useGuideHighlight({ onJump }: UseGuideHighlightParams) {
  const highlightTimeoutRef = useRef<number | null>(null)
  const [highlightedGuide, setHighlightedGuide] = useState<HighlightedGuide | null>(null)

  useEffect(() => () => {
    if (highlightTimeoutRef.current !== null) {
      window.clearTimeout(highlightTimeoutRef.current)
    }
  }, [])

  const handleGuideJump = useCallback((tab: GuideTab, query: string, label = query, targetKey?: string) => {
    const nextHighlightedGuide = { label, query, tab, targetKey }

    onJump(tab, query, label)
    setHighlightedGuide(nextHighlightedGuide)
    if (highlightTimeoutRef.current !== null) {
      window.clearTimeout(highlightTimeoutRef.current)
    }
    highlightTimeoutRef.current = window.setTimeout(() => {
      setHighlightedGuide((current) => (
        current?.tab === nextHighlightedGuide.tab && current.query === nextHighlightedGuide.query
          ? null
          : current
      ))
    }, GUIDE_HIGHLIGHT_DURATION_MS)
  }, [onJump])

  return {
    handleGuideJump,
    highlightedGuide,
  }
}
