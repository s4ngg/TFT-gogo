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

export function buildGuideTargetKey(guideType: GuideEntryType, name: string) {
  const normalizedName = name.trim().replace(/\s+/g, '_')
  return `${guideType}:${normalizedName}`.slice(0, 120)
}

export function createGameGuideAiRef(
  guideType: GuideEntryType,
  name: string,
  targetKey?: string,
): GameGuideAiPathfinderRef {
  const normalizedName = name.trim()

  return {
    guideType,
    name: normalizedName,
    targetKey: targetKey?.trim() || buildGuideTargetKey(guideType, normalizedName),
  }
}
