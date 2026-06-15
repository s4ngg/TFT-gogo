const GUIDE_HASH_TAG_PATTERN = /^\{[0-9a-f]{6,}\}$/i

export function sanitizeGuideText(value: string) {
  return value
    .replace(/<br\s*\/?>|<\/p>|<\/li>|<\/div>/gi, ' ')
    .replace(/<[^>]+>/g, '')
    .replace(/@[^@]+@/g, '')
    .replace(/%[A-Za-z_:][A-Za-z0-9_:.-]*%/g, '')
    .replace(/(\d)\s+%/g, '$1%')
    .replace(/(^|[^\d])%/g, '$1')
    .replace(/\(\s*\)/g, '')
    .replace(/\s+([.,!?])/g, '$1')
    .replace(/\s+/g, ' ')
    .trim()
}

export function isDisplayableGuideTag(value: string) {
  const text = sanitizeGuideText(value)

  return text.length > 0 && !GUIDE_HASH_TAG_PATTERN.test(text)
}

export function formatGuideRankMetric(value: string) {
  const text = value.trim()

  return text && text !== '-' ? `#${text}` : '-'
}
