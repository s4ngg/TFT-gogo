/**
 * 레벨별 배치 가능한 최대 코스트
 * Lv.5-6: 3코스트 이하 / Lv.7-8: 4코스트 이하 / Lv.9: 5코스트 이하
 */
export function costLimitForLevel(level: number): number {
  if (level <= 6) return 3
  if (level <= 8) return 4
  return 5
}
