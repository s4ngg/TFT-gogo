import {
  type ChangeType,
  type PatchChange,
} from '../../../api/patchNotes'

export type PatchChangeStatusTone = 'added' | 'disabled' | 'enabled' | 'removed'

export interface PatchChangeStatusDisplay {
  label: string
  title: string
  tone: PatchChangeStatusTone
}

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

interface StatusPattern {
  expression: RegExp
  label: string
  tone: PatchChangeStatusTone
}

const STATUS_PATTERNS: StatusPattern[] = [
  {
    expression: /^(.+?)(?:이|가)\s+다시\s+활성화됩니다\.?$/u,
    label: '복귀',
    tone: 'enabled',
  },
  {
    expression: /^(.+?)\s+다시\s+활성화됩니다\.?$/u,
    label: '복귀',
    tone: 'enabled',
  },
  {
    expression: /^(.+?)(?:이|가)\s+비활성화됩니다\.?$/u,
    label: '제외',
    tone: 'disabled',
  },
  {
    expression: /^(.+?)\s+비활성화됩니다\.?$/u,
    label: '제외',
    tone: 'disabled',
  },
  {
    expression: /^(.+?)(?:이|가)\s+활성화됩니다\.?$/u,
    label: '사용 가능',
    tone: 'enabled',
  },
  {
    expression: /^(.+?)\s+활성화됩니다\.?$/u,
    label: '사용 가능',
    tone: 'enabled',
  },
  {
    expression: /^(.+?)(?:이|가)\s+추가됩니다\.?$/u,
    label: '신규',
    tone: 'added',
  },
  {
    expression: /^(.+?)\s+추가됩니다\.?$/u,
    label: '신규',
    tone: 'added',
  },
  {
    expression: /^(.+?)(?:이|가)\s+삭제됩니다\.?$/u,
    label: '제거',
    tone: 'removed',
  },
  {
    expression: /^(.+?)\s+삭제됩니다\.?$/u,
    label: '제거',
    tone: 'removed',
  },
]

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

function normalizeDisplayText(value: string) {
  return normalizePatchChangeArrow(value)
    .replace(/^(?:[\s,.;:，、·•\-–—"'“”‘’`]+|\(\d+\)\s*)+/u, '')
    .trim()
}

function normalizeDisplayTitle(value: string) {
  return normalizeDisplayText(value)
}

function normalizeStatusTitle(value: string) {
  return normalizeDisplayTitle(value)
    .replace(/\s+(?:정상적으로|올바르게|제대로)$/u, '')
    .replace(/(조우자 없음)\s+조우자(?:이|가)?$/u, '$1')
    .trim()
}

function getStatusDisplayFromText(value: string): PatchChangeStatusDisplay | undefined {
  const normalizedValue = normalizeDisplayText(value)

  for (const pattern of STATUS_PATTERNS) {
    const match = normalizedValue.match(pattern.expression)
    const title = normalizeStatusTitle(match?.[1] ?? '')

    if (title) {
      return {
        label: pattern.label,
        title,
        tone: pattern.tone,
      }
    }
  }

  return undefined
}

function getTitleDelimiterIndex(value: string, shouldUseCommaDelimiter: boolean) {
  const commaIndex = value.indexOf(',')
  const colonIndex = value.indexOf(':')

  if (
    shouldUseCommaDelimiter
    && commaIndex > 0
    && (colonIndex === -1 || commaIndex < colonIndex)
  ) {
    return commaIndex
  }

  if (colonIndex > 0) return colonIndex

  return -1
}

function shouldKeepTitleColon(title: string, value: string, delimiterIndex: number) {
  return value[delimiterIndex] === ':' && title.endsWith('스킬')
}

function getTitleFromText(value: string, shouldUseCommaDelimiter: boolean) {
  const normalizedValue = normalizeDisplayText(value)
  const delimiterIndex = getTitleDelimiterIndex(normalizedValue, shouldUseCommaDelimiter)

  if (delimiterIndex > 0) {
    const title = normalizedValue.slice(0, delimiterIndex).trim()
    return shouldKeepTitleColon(title, normalizedValue, delimiterIndex) ? title + ':' : title
  }

  return normalizedValue || '패치 변경사항'
}

function getDetailFromText(value: string, title: string) {
  const normalizedValue = normalizeDisplayText(value)
  const normalizedTitle = normalizeDisplayText(title)

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

function splitDetailSummaryLines(value: string) {
  const normalizedValue = normalizePatchChangeArrow(value)
  const arrowCount = normalizedValue.match(/ → /gu)?.length ?? 0

  if (arrowCount < 2) return normalizedValue ? [normalizedValue] : []

  return normalizedValue
    .split(/,\s+/u)
    .map((line) => line.trim())
    .filter(Boolean)
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
  const statusDisplay = getPatchChangeStatusDisplay(change)

  if (statusDisplay) return statusDisplay.title

  if (!target) return getTitleFromSummary(summary)
  if (GENERIC_TARGET_NAMES.has(target) && summary) return getTitleFromSummary(summary)
  return getTitleFromTarget(target)
}

export function getPatchChangeStatusDisplay(change: PatchChange) {
  return getStatusDisplayFromText(change.summary) ?? getStatusDisplayFromText(change.target)
}

export function getPatchChangeDetailSummary(change: PatchChange, title: string) {
  if (getPatchChangeStatusDisplay(change)) return ''
  return getPreferredDetailSummary(change.summary, change.target, title)
}

export function getPatchChangeDetailLines(change: PatchChange, title: string) {
  return splitDetailSummaryLines(getPatchChangeDetailSummary(change, title))
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

export function getVisiblePatchChangeStatuses(changes: PatchChange[]): PatchChangeStatusDisplay[] {
  const statuses = new Map<string, PatchChangeStatusDisplay>()

  changes.forEach((change) => {
    const status = getPatchChangeStatusDisplay(change)
    if (!status) return

    statuses.set(`${status.tone}:${status.label}`, status)
  })

  return Array.from(statuses.values())
}

export function getVisibleNewChangeTypes(changes: PatchChange[]): ChangeType[] {
  return changes.some((change) => change.type === '신규') ? ['신규'] : []
}
