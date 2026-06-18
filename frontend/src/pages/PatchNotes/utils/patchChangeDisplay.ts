import {
  type ChangeType,
  type ChangeTypeFilter,
  type PatchChange,
} from '../../../api/patchNotes'

const GENERIC_TARGET_NAMES = new Set([
  '기타',
  '버그 수정',
  '변경사항',
  '시스템',
  '시너지',
  '시작 조우자',
  '아이템',
  '유닛',
  '증강',
  '증강체',
  '챔피언',
  '특성',
])

export interface PatchChangeGroup {
  title: string
  changes: PatchChange[]
}

export function normalizePatchChangeArrow(value: string) {
  return value
    .replace(/\s*⇒\s*/g, ' → ')
    .replace(/\s*->\s*/g, ' → ')
    .replace(/\s*→\s*/g, ' → ')
    .replace(/\s+/g, ' ')
    .trim()
}

function getTitleDelimiterIndex(value: string, shouldUseCommaDelimiter: boolean) {
  const commaIndex = value.indexOf(',')
  if (shouldUseCommaDelimiter && commaIndex > 0) return commaIndex

  const colonIndex = value.indexOf(':')
  if (colonIndex > 0) return colonIndex

  return -1
}

function getTitleFromText(value: string, shouldUseCommaDelimiter: boolean) {
  const normalizedValue = normalizePatchChangeArrow(value)
  const delimiterIndex = getTitleDelimiterIndex(normalizedValue, shouldUseCommaDelimiter)

  if (delimiterIndex > 0) {
    return normalizedValue.slice(0, delimiterIndex).trim()
  }

  return normalizedValue || '패치 변경사항'
}

function getDetailFromText(value: string, title: string) {
  const normalizedValue = normalizePatchChangeArrow(value)
  const normalizedTitle = normalizePatchChangeArrow(title)

  if (!normalizedValue || normalizedValue === normalizedTitle) return ''

  if (!normalizedValue.startsWith(normalizedTitle)) return normalizedValue

  const remainingValue = normalizedValue.slice(normalizedTitle.length)
  const firstCharacter = remainingValue[0]

  if (firstCharacter === ',' || firstCharacter === ':' || firstCharacter === ' ') {
    return remainingValue.slice(1).trim()
  }

  return normalizedValue
}

function hasValueChangeIndicator(value: string) {
  return normalizePatchChangeArrow(value).includes(' → ')
}

function getPreferredDetailSummary(summary: string, target: string, title: string) {
  const summaryDetail = getDetailFromText(summary, title)
  const targetDetail = getDetailFromText(target, title)

  if (!targetDetail) return summaryDetail
  if (!summaryDetail) return targetDetail

  const comparableSummaryDetail = normalizeComparableText(summaryDetail)
  const comparableTargetDetail = normalizeComparableText(targetDetail)

  if (comparableSummaryDetail.includes(comparableTargetDetail)) {
    return summaryDetail
  }

  if (hasValueChangeIndicator(targetDetail)) {
    return targetDetail
  }

  return summaryDetail
}

function getTitleFromSummary(summary: string) {
  return getTitleFromText(summary, true)
}

function getTitleFromTarget(target: string) {
  return getTitleFromText(target, false)
}

function normalizeComparableText(value: string) {
  return normalizePatchChangeArrow(value)
    .replace(/\s+/g, '')
    .toLowerCase()
}

function summaryIncludesValueChange(summary: string, before: string, after: string) {
  if (!before || !after) return false

  const comparableSummary = normalizeComparableText(summary)
  return comparableSummary.includes(normalizeComparableText(before))
    && comparableSummary.includes(normalizeComparableText(after))
}

function changeTextIncludesValueChange(change: PatchChange, before: string, after: string) {
  return [change.summary, change.target].some((value) => summaryIncludesValueChange(value, before, after))
}

export function getPatchChangeTitle(change: PatchChange) {
  const target = change.target.trim()
  const summary = change.summary.trim()

  if (!target) return getTitleFromSummary(summary)
  if (GENERIC_TARGET_NAMES.has(target) && summary) return getTitleFromSummary(summary)
  return getTitleFromTarget(target)
}

export function getPatchChangeDetailSummary(change: PatchChange, title: string) {
  return getPreferredDetailSummary(change.summary, change.target, title)
}

export function shouldShowPatchChangeValueLine(change: PatchChange) {
  const before = change.before.trim()
  const after = change.after.trim()

  if (!before && !after) return false
  return !changeTextIncludesValueChange(change, before, after)
}

export function getPatchChangeGroupKey(title: string) {
  return normalizePatchChangeArrow(title).toLowerCase()
}

export function groupPatchChangesByTitle(changes: PatchChange[]): PatchChangeGroup[] {
  const groups = new Map<string, PatchChangeGroup>()

  changes.forEach((change) => {
    const title = getPatchChangeTitle(change)
    const key = getPatchChangeGroupKey(title)
    const group = groups.get(key)

    if (group) {
      group.changes.push(change)
      return
    }

    groups.set(key, {
      title,
      changes: [change],
    })
  })

  return Array.from(groups.values())
}

export function getVisiblePatchChangeTypes(
  changes: PatchChange[],
  activeChangeType: ChangeTypeFilter,
): ChangeType[] {
  const changeTypes = Array.from(new Set(changes.map((change) => change.type)))

  if (activeChangeType !== '전체') {
    return changeTypes.filter((changeType) => changeType !== activeChangeType)
  }

  return changeTypes.length > 1 ? changeTypes : []
}
