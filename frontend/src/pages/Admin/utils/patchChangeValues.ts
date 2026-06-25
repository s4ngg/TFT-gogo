export interface PatchChangeValueDisplayInput {
  afterValue: string | null
  beforeValue: string | null
  summary: string
}

function normalizeChangeText(value: string): string {
  return value
    .replace(/[⇒→]/g, '->')
    .replace(/\s+/g, '')
    .toLowerCase()
}

export function shouldShowPatchChangeValues(change: PatchChangeValueDisplayInput): boolean {
  const beforeValue = change.beforeValue?.trim() ?? ''
  const afterValue = change.afterValue?.trim() ?? ''

  if (!beforeValue && !afterValue) return false

  const normalizedSummary = normalizeChangeText(change.summary)
  const normalizedBeforeValue = normalizeChangeText(beforeValue)
  const normalizedAfterValue = normalizeChangeText(afterValue)

  const summaryIncludesBefore = normalizedBeforeValue !== '' && normalizedSummary.includes(normalizedBeforeValue)
  const summaryIncludesAfter = normalizedAfterValue !== '' && normalizedSummary.includes(normalizedAfterValue)

  if (summaryIncludesBefore && summaryIncludesAfter) return false

  return true
}
