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

export function useGameGuideAiCandidateRefs(
  activeTab: GuideTab,
  visibleItems: GuideTabItems[GuideTab][number][],
): GameGuideAiPathfinderRef[] {
  return useMemo(() => {
    const guideType = GUIDE_TYPE_BY_TAB[activeTab]
    const seen = new Set<string>()
    const candidateRefs: GameGuideAiPathfinderRef[] = []

    for (const guideEntry of visibleItems) {
      if (candidateRefs.length >= MAX_CANDIDATE_REFS) break

      const name = guideEntry.name.trim()
      if (!name) continue

      const targetKey = createGameGuideAiRef(guideType, name, guideEntry.targetKey).targetKey
      const uniqueKey = `${guideType}:${targetKey}`
      if (seen.has(uniqueKey)) continue

      seen.add(uniqueKey)
      candidateRefs.push({
        guideType,
        name,
        targetKey,
      })
    }

    return candidateRefs
  }, [activeTab, visibleItems])
}
