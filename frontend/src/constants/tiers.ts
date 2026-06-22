import type { RankedTierBadgeValue } from '../types/badges'

export const TIER_ORDER: RankedTierBadgeValue[] = ['S', 'A', 'B', 'C', 'D']

export const TIER_META: Record<RankedTierBadgeValue, { color: string; label: string }> = {
  S: { color: '#04f3e5', label: '최상위 픽 · 강력 추천' },
  A: { color: '#a78bfa', label: '중상위권 범용 덱' },
  B: { color: '#60a5fa', label: '중위권 상황 의존적' },
  C: { color: '#818cf8', label: '하위권 전문 운영 필요' },
  D: { color: '#6b7280', label: '비추천 · 낮은 안정성' },
}