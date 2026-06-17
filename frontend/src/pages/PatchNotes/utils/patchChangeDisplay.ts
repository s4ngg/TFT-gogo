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

function getTitleDelimiterIndex(summary: string) {
  const commaIndex = summary.indexOf(',')
  if (commaIndex > 0) return commaIndex

  const colonIndex = summary.indexOf(':')
  if (colonIndex > 0) return colonIndex

  return -1
}

function getTitleFromSummary(summary: string) {
  const normalizedSummary = normalizePatchChangeArrow(summary)
  const delimiterIndex = getTitleDelimiterIndex(normalizedSummary)

  if (delimiterIndex > 0) {
    return normalizedSummary.slice(0, delimiterIndex).trim()
  }

  return normalizedSummary || '패치 변경사항'
}

export function getPatchChangeTitle(change: PatchChange) {
  const target = change.target.trim()
  const summary = change.summary.trim()

  if (!target) return getTitleFromSummary(summary)
  if (GENERIC_TARGET_NAMES.has(target) && summary) return getTitleFromSummary(summary)
  return target
}

export function getPatchChangeDetailSummary(change: PatchChange, title: string) {
  const summary = normalizePatchChangeArrow(change.summary)
  const normalizedTitle = normalizePatchChangeArrow(title)

  if (!summary || summary === normalizedTitle) return ''

  if (!summary.startsWith(normalizedTitle)) return summary

  const remainingSummary = summary.slice(normalizedTitle.length)
  const firstCharacter = remainingSummary[0]

  if (firstCharacter === ',' || firstCharacter === ':' || firstCharacter === ' ') {
    return remainingSummary.slice(1).trim()
  }

  return summary
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

export function shouldShowPatchChangeValueLine(change: PatchChange) {
  const before = change.before.trim()
  const after = change.after.trim()

  if (!before && !after) return false
  return !summaryIncludesValueChange(change.summary, before, after)
}

function getPatchChangeGroupKey(title: string) {
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
