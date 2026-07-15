import type {
  GuideEntryType,
  GuideTab,
} from '../../../api/guide'
import type { GameGuideAiPathfinderRef } from '../../../api/gameGuideAiPathfinderApi'

export type GameGuideAiAskHandler = (ref: GameGuideAiPathfinderRef) => void

export const GUIDE_TYPE_BY_TAB: Record<GuideTab, GuideEntryType> = {
  augments: 'AUGMENT',
  champions: 'CHAMPION',
  items: 'ITEM',
  traits: 'TRAIT',
}

export function createGameGuideAiRef(
  guideType: GuideEntryType,
  name: string,
  targetKey?: string,
): GameGuideAiPathfinderRef | null {
  const normalizedName = name.trim()
  const normalizedTargetKey = targetKey?.trim()

  if (!normalizedName || !normalizedTargetKey) return null

  return {
    guideType,
    name: normalizedName,
    targetKey: normalizedTargetKey,
  }
}
