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

  if (normalizedBeforeValue !== '' && normalizedAfterValue !== '') {
    const normalizedChange = `${normalizedBeforeValue}->${normalizedAfterValue}`
    if (normalizedSummary.includes(normalizedChange)) return false
  }

  return true
}