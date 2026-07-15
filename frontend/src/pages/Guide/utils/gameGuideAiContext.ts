import type { GameGuideAiPathfinderRef } from '../../../api/gameGuideAiPathfinderApi'
import type { GuideTab } from '../../../api/guide'

export interface GameGuideAiScopedValue<T> {
  scopeKey: string
  value: T
}

export function createGameGuideAiScopeKey(patchVersion: string, activeTab: GuideTab) {
  return `${patchVersion.trim()}:${activeTab}`
}

export function readGameGuideAiScopedValue<T>(
  state: GameGuideAiScopedValue<T>,
  currentScopeKey: string,
  emptyValue: T,
) {
  return state.scopeKey === currentScopeKey ? state.value : emptyValue
}

export function createGameGuideAiWidgetKey(
  scopeKey: string,
  selectedRefs: GameGuideAiPathfinderRef[],
) {
  const selectionKey = selectedRefs
    .map((ref) => `${ref.guideType}:${ref.targetKey}`)
    .join('|')
  return `${scopeKey}:${selectionKey}`
}
