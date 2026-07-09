import { useCallback, useEffect, useRef, useState } from 'react'
import type { GuideTab } from '../../../api/guide'
import type { HighlightedGuide } from '../utils/guideHighlight'

const GUIDE_HIGHLIGHT_DURATION_FALLBACK_MS = 2600
const GUIDE_HIGHLIGHT_PULSE_DURATION_VAR = '--guide-highlight-pulse-duration'
const GUIDE_HIGHLIGHT_PULSE_ITERATIONS_VAR = '--guide-highlight-pulse-iterations'

function parseCssTimeToMs(value: string): number | null {
  const trimmedValue = value.trim()
  const numericValue = Number.parseFloat(trimmedValue)

  if (!Number.isFinite(numericValue) || numericValue <= 0) {
    return null
  }

  if (trimmedValue.endsWith('ms')) {
    return numericValue
  }

  if (trimmedValue.endsWith('s')) {
    return numericValue * 1000
  }

  return null
}

function parsePositiveNumber(value: string): number | null {
  const numericValue = Number.parseFloat(value.trim())

  return Number.isFinite(numericValue) && numericValue > 0 ? numericValue : null
}

function readGuideHighlightDurationMs(): number {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return GUIDE_HIGHLIGHT_DURATION_FALLBACK_MS
  }

  const rootStyles = window.getComputedStyle(document.documentElement)
  const pulseDurationMs = parseCssTimeToMs(rootStyles.getPropertyValue(GUIDE_HIGHLIGHT_PULSE_DURATION_VAR))
  const pulseIterations = parsePositiveNumber(rootStyles.getPropertyValue(GUIDE_HIGHLIGHT_PULSE_ITERATIONS_VAR))

  if (pulseDurationMs === null || pulseIterations === null) {
    return GUIDE_HIGHLIGHT_DURATION_FALLBACK_MS
  }

  return pulseDurationMs * pulseIterations
}

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
    }, readGuideHighlightDurationMs())
  }, [onJump])

  return {
    handleGuideJump,
    highlightedGuide,
  }
}
