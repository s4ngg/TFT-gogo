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

const COLLAPSIBLE_VALUE_LIST_MIN_LENGTH = 4
const DISPLAY_ARROW = '→'
const VALUE_CHANGE_ARROW_PATTERN = /\s*(?:→|⇒|->)\s*/u
const DETAIL_LABEL_ONLY_TEXTS = new Set([
  '버그수정',
  '복귀',
  '사용가능',
  '상향',
  '삭제',
  '수정',
  '신규',
  '제거',
  '제외',
  '조정',
  '추가',
  '하향',
])

interface StatusPattern {
  expression: RegExp
  label: string
  tone: PatchChangeStatusTone
}

const STATUS_FILLER_ADVERBS = '(?:정상적으로|정삭적으로|올바르게|제대로)'

const STATUS_PATTERNS: StatusPattern[] = [
  {
    expression: new RegExp(`^(.+?)(?:이|가)?\\s+(?:${STATUS_FILLER_ADVERBS}\\s+)?다시\\s+활성화됩니다\\.?$`, 'u'),
    label: '복귀',
    tone: 'enabled',
  },
  {
    expression: new RegExp(`^(.+?)(?:이|가)?\\s+(?:${STATUS_FILLER_ADVERBS}\\s+)?비활성화됩니다\\.?$`, 'u'),
    label: '제외',
    tone: 'disabled',
  },
  {
    expression: new RegExp(`^(.+?)(?:이|가)?\\s+(?:${STATUS_FILLER_ADVERBS}\\s+)?활성화됩니다\\.?$`, 'u'),
    label: '사용 가능',
    tone: 'enabled',
  },
  {
    expression: new RegExp(`^(.+?)(?:이|가)?\\s+(?:${STATUS_FILLER_ADVERBS}\\s+)?추가됩니다\\.?$`, 'u'),
    label: '신규',
    tone: 'added',
  },
  {
    expression: new RegExp(`^(.+?)(?:이|가)?\\s+(?:${STATUS_FILLER_ADVERBS}\\s+)?삭제됩니다\\.?$`, 'u'),
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

function isGenericTargetName(value: string) {
  const normalizedValue = normalizeDisplayText(value)
  const colonIndex = normalizedValue.indexOf(':')
  const heading = colonIndex > 0 ? normalizedValue.slice(0, colonIndex).trim() : normalizedValue

  return GENERIC_TARGET_NAMES.has(normalizedValue) || GENERIC_TARGET_NAMES.has(heading)
}

function removeTrailingRepeatedToken(value: string) {
  const words = value.split(/\s+/u).filter(Boolean)
  const trailingWord = words[words.length - 1]

  if (!trailingWord || words.length < 2) return value

  const prefixWords = words.slice(0, -1).flatMap((word) => word.split(/[:()[\],.]+/u)).filter(Boolean)

  if (!prefixWords.includes(trailingWord)) return value

  return words.slice(0, -1).join(' ')
}

function normalizeStatusTitle(value: string) {
  const title = normalizeDisplayTitle(value)
    .replace(new RegExp(`\\s+${STATUS_FILLER_ADVERBS}$`, 'u'), '')
    .trim()

  return removeTrailingRepeatedToken(title).trim()
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

interface BugFixTextParts {
  detail: string
  title: string
}

function getBugFixPartsFromText(value: string): BugFixTextParts | undefined {
  const normalizedValue = normalizeDisplayText(value)
  const match = normalizedValue.match(/^(?:버그\s*수정:\s*)?(.+?)\s*버그(?:를|가|는)?\s*수정(?:했습니다|되었습니다|됩니다|됨)?\.?\s*(.*)$/u)
  const title = normalizeStatusTitle(match?.[1] ?? '')

  if (!title) return undefined

  return {
    detail: normalizeDisplayText(match?.[2] ?? ''),
    title: title.endsWith('문제') ? `${title} 수정` : `${title} 문제 수정`,
  }
}

function getBugFixTitleFromText(value: string) {
  return getBugFixPartsFromText(value)?.title
}

function getBugFixDetailFromText(value: string) {
  return getBugFixPartsFromText(value)?.detail ?? ''
}

function getDelimiterIndexOutsideParentheses(value: string, delimiter: string) {
  let parenthesisDepth = 0

  for (let index = 0; index < value.length; index += 1) {
    const character = value[index]

    if (character === '(') {
      parenthesisDepth += 1
      continue
    }

    if (character === ')') {
      parenthesisDepth = Math.max(0, parenthesisDepth - 1)
      continue
    }

    if (parenthesisDepth === 0 && character === delimiter) return index
  }

  return -1
}

function getTitleDelimiterIndex(value: string, shouldUseCommaDelimiter: boolean) {
  const commaIndex = getDelimiterIndexOutsideParentheses(value, ',')
  const colonIndex = getDelimiterIndexOutsideParentheses(value, ':')

  if (
    shouldUseCommaDelimiter
    && commaIndex > 0
    && colonIndex > commaIndex
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

function splitValueChange(value: string): [string, string] | undefined {
  const parts = value.split(VALUE_CHANGE_ARROW_PATTERN)

  if (parts.length !== 2) return undefined

  const before = parts[0].trim()
  const after = parts[1].trim()

  return before && after ? [before, after] : undefined
}

function splitValueTokens(value: string) {
  return value
    .split('/')
    .map((token) => token.trim())
    .filter(Boolean)
}

function normalizeValueToken(value: string) {
  return value.replace(/\s+/g, '').toLowerCase()
}

function splitDetailLabel(value: string) {
  const colonIndex = value.indexOf(':')

  if (colonIndex <= 0) {
    return {
      body: value,
      label: '',
    }
  }

  return {
    body: value.slice(colonIndex + 1).trim(),
    label: value.slice(0, colonIndex + 1).trim(),
  }
}

function getCondensedValueListChangeLines(value: string) {
  const { body, label } = splitDetailLabel(value)
  const valueChange = splitValueChange(body)

  if (!valueChange) return undefined

  const [before, after] = valueChange
  const beforeTokens = splitValueTokens(before)
  const afterTokens = splitValueTokens(after)

  if (
    beforeTokens.length < COLLAPSIBLE_VALUE_LIST_MIN_LENGTH
    || beforeTokens.length !== afterTokens.length
  ) {
    return undefined
  }

  const changedPairs = beforeTokens
    .map((beforeToken, index) => ({
      after: afterTokens[index],
      before: beforeToken,
    }))
    .filter(({ before, after }) => normalizeValueToken(before) !== normalizeValueToken(after))

  if (changedPairs.length === 0) return undefined

  const beforeLine = changedPairs.map(({ before }) => before).join(' / ')
  const afterLine = `${DISPLAY_ARROW} ${changedPairs.map(({ after }) => after).join(' / ')}`

  return label ? [label, beforeLine, afterLine] : [beforeLine, afterLine]
}

function getPreferredDetailSummary(summary: string, target: string, title: string) {
  const summaryDetail = getDetailFromText(summary, title)
  const targetDetail = isGenericTargetName(target) ? '' : getDetailFromText(target, title)

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

function isLabelOnlyDetail(value: string) {
  if (!value) return false
  return DETAIL_LABEL_ONLY_TEXTS.has(normalizeComparableText(value))
}

function splitDetailSummaryLines(value: string) {
  const normalizedValue = normalizePatchChangeArrow(value)
  const condensedLines = getCondensedValueListChangeLines(normalizedValue)

  if (condensedLines) return condensedLines
  const arrowCount = normalizedValue.match(/ → /gu)?.length ?? 0

  if (arrowCount < 2) return normalizedValue ? [normalizedValue] : []

  return normalizedValue
    .split(/,\s+/u)
    .flatMap((line) => {
      const trimmedLine = line.trim()
      return getCondensedValueListChangeLines(trimmedLine) ?? [trimmedLine]
    })
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
  const bugFixTitle = getBugFixTitleFromText(summary) ?? getBugFixTitleFromText(target)

  if (statusDisplay) return statusDisplay.title
  if (bugFixTitle) return bugFixTitle

  if (!target) return getTitleFromSummary(summary)
  if (isGenericTargetName(target) && summary) return getTitleFromSummary(summary)
  return getTitleFromTarget(target)
}

export function getPatchChangeStatusDisplay(change: PatchChange) {
  return getStatusDisplayFromText(change.summary)
    ?? getStatusDisplayFromText(change.target)
}

export function getPatchChangeDetailSummary(change: PatchChange, title: string) {
  if (getPatchChangeStatusDisplay(change)) return ''
  if (getBugFixTitleFromText(change.summary) ?? getBugFixTitleFromText(change.target)) {
    return getBugFixDetailFromText(change.summary) || getBugFixDetailFromText(change.target)
  }
  const detailSummary = getPreferredDetailSummary(change.summary, change.target, title)
  return isLabelOnlyDetail(detailSummary) ? '' : detailSummary
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
