export function timeAgo(gameDateTime: number): string {
  const diffMs = Date.now() - gameDateTime
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '방금 전'
  if (diffMin < 60) return `${diffMin}분 전`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}시간 전`
  const diffDay = Math.floor(diffHour / 24)
  return diffDay === 1 ? '어제' : `${diffDay}일 전`
}

export function formatDate(ts: number): string {
  const d = new Date(ts)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}.${mm}.${dd}`
}

export function placementTone(n: number, styles: Record<string, string>): string {
  if (n === 1) return styles.gold
  if (n <= 4) return styles.top4
  return styles.bot4
}

export function detailRankClass(n: number, styles: Record<string, string>): string {
  if (n === 1) return styles.detailRankGold
  if (n === 2) return styles.detailRankSilver
  if (n === 3) return styles.detailRankBronze
  return ''
}
