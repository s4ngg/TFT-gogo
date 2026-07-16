import { useMemo } from 'react'
import type {
  GuideTab,
  GuideTabItems,
} from '../../../api/guide'
import type { GameGuideAiPathfinderRef } from '../../../api/gameGuideAiPathfinderApi'
import {
  createGameGuideAiRef,
  GUIDE_TYPE_BY_TAB,
} from '../utils/gameGuideAiRefs'

const MAX_CANDIDATE_REFS = 20

export function createGameGuideAiCandidateRefs(
  activeTab: GuideTab,
  visibleItems: GuideTabItems[GuideTab][number][],
): GameGuideAiPathfinderRef[] {
  const guideType = GUIDE_TYPE_BY_TAB[activeTab]
  const seen = new Set<string>()
  const candidateRefs: GameGuideAiPathfinderRef[] = []

  for (const guideEntry of visibleItems) {
    if (candidateRefs.length >= MAX_CANDIDATE_REFS) break

    const ref = createGameGuideAiRef(guideType, guideEntry.name, guideEntry.targetKey)
    if (!ref) continue

    const uniqueKey = `${ref.guideType}:${ref.targetKey}`
    if (seen.has(uniqueKey)) continue

    seen.add(uniqueKey)
    candidateRefs.push(ref)
  }

  return candidateRefs
}

export function useGameGuideAiCandidateRefs(
  activeTab: GuideTab,
  visibleItems: GuideTabItems[GuideTab][number][],
): GameGuideAiPathfinderRef[] {
  return useMemo(
    () => createGameGuideAiCandidateRefs(activeTab, visibleItems),
    [activeTab, visibleItems],
  )
}
