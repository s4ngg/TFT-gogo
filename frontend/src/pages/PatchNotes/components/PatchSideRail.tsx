import { CheckCircle2, ChevronDown, ChevronRight, History } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import {
  sanitizePatchHighlight,
  type PatchCategory,
  type PatchNoteDetail,
} from '../../../api/patchNotes'
import styles from '../PatchNotes.module.css'

interface PatchSideRailProps {
  patchHistory: PatchNoteDetail[]
  selectedPatch: PatchNoteDetail
  selectedPatchVersion: string
  onInsightSelect: (category: PatchCategory) => void
  onPatchSelect: (version: string) => void
}

interface InsightItem {
  category: PatchCategory
  label: string
}

function getPatchSeason(version: string) {
  const match = version.match(/^(\d{1,2})(?:[.-]\d+)/)
  return match ? match[1] : '기타'
}

function getDateSortValue(date: string) {
  const timestamp = Date.parse(date.replace(/\./g, '-'))
  return Number.isNaN(timestamp) ? 0 : timestamp
}

function buildSeasonGroups(patchHistory: PatchNoteDetail[]) {
  const seasonMap = new Map<string, PatchNoteDetail[]>()

  patchHistory.forEach((patch) => {
    const season = getPatchSeason(patch.version)
    const patches = seasonMap.get(season) ?? []
    patches.push(patch)
    seasonMap.set(season, patches)
  })

  return Array.from(seasonMap.entries())
    .map(([season, patches]) => {
      const dateMap = new Map<string, PatchNoteDetail[]>()
      patches.forEach((patch) => {
        const datePatches = dateMap.get(patch.date) ?? []
        datePatches.push(patch)
        dateMap.set(patch.date, datePatches)
      })

      return {
        season,
        patchCount: patches.length,
        dateGroups: Array.from(dateMap.entries())
          .map(([date, datePatches]) => ({
            date,
            patches: datePatches,
          }))
          .sort((left, right) => getDateSortValue(right.date) - getDateSortValue(left.date)),
      }
    })
    .sort((left, right) => {
      const leftSeason = Number(left.season)
      const rightSeason = Number(right.season)
      if (!Number.isFinite(leftSeason)) return 1
      if (!Number.isFinite(rightSeason)) return -1
      return rightSeason - leftSeason
    })
}

function getInsightLabel(highlight: string) {
  const trimmedHighlight = sanitizePatchHighlight(highlight)
  if (!trimmedHighlight) return ''

  if (/유닛.*\d단계|\d단계.*유닛/u.test(trimmedHighlight)) return '유닛 단계별 밸런스'
  if (trimmedHighlight.includes('특성')) return '특성 밸런스'
  if (trimmedHighlight.includes('증강')) return '증강 변경'
  if (trimmedHighlight.includes('아이템')) return '아이템 조정'
  if (trimmedHighlight.includes('버그 수정')) return '버그 수정'
  if (trimmedHighlight.includes('밸런스 변경')) return '밸런스 변경'

  return trimmedHighlight.replace(/\s*[:：]\s*$/u, '')
}

function getInsightCategory(highlight: string): PatchCategory {
  if (/챔피언|유닛/u.test(highlight)) return '챔피언'
  if (/시너지|특성/u.test(highlight)) return '시너지'
  if (/아이템|장비/u.test(highlight)) return '아이템'
  if (/증강/u.test(highlight)) return '증강체'
  if (/시스템|버그|조우자|오류|수정/u.test(highlight)) return '시스템'

  return '전체'
}

function buildInsightItems(highlights: string[]): InsightItem[] {
  const insightMap = new Map<string, InsightItem>()

  highlights.forEach((highlight) => {
    const label = getInsightLabel(highlight)
    if (!label || insightMap.has(label)) return

    insightMap.set(label, {
      category: getInsightCategory(`${highlight} ${label}`),
      label,
    })
  })

  return Array.from(insightMap.values()).slice(0, 4)
}

function PatchSideRail({
  onInsightSelect,
  onPatchSelect,
  patchHistory,
  selectedPatch,
  selectedPatchVersion,
}: PatchSideRailProps) {
  const seasonGroups = useMemo(() => buildSeasonGroups(patchHistory), [patchHistory])
  const insightItems = useMemo(() => buildInsightItems(selectedPatch.highlights), [selectedPatch.highlights])
  const summaryText = selectedPatch.summary || selectedPatch.description || selectedPatch.focus
  const selectedSeason = getPatchSeason(selectedPatchVersion)
  const [openSeasons, setOpenSeasons] = useState<Set<string>>(() => new Set([selectedSeason]))

  useEffect(() => {
    setOpenSeasons((current) => {
      if (current.has(selectedSeason)) return current

      const next = new Set(current)
      next.add(selectedSeason)
      return next
    })
  }, [selectedSeason])

  function toggleSeason(season: string) {
    setOpenSeasons((current) => {
      const next = new Set(current)
      if (next.has(season)) {
        next.delete(season)
      } else {
        next.add(season)
      }
      return next
    })
  }

  return (
    <aside className={styles.sideRail}>
      <section className={styles.historyPanel}>
        <div className={styles.historyHeader}>
          <span className={styles.sectionLabel}>시즌별 패치</span>
          <History size={17} />
        </div>
        <h2>패치 타임라인</h2>
        <div className={styles.seasonList}>
          {seasonGroups.map((group) => {
            const isOpen = openSeasons.has(group.season)
            const isActiveSeason = group.season === selectedSeason

            return (
              <div key={group.season} className={`${styles.seasonBlock} ${isActiveSeason ? styles.activeSeason : ''}`}>
                <button
                  type="button"
                  className={styles.seasonHeaderButton}
                  onClick={() => toggleSeason(group.season)}
                  aria-expanded={isOpen}
                >
                  <span className={styles.seasonTitle}>{group.season === '기타' ? '기타' : `시즌 ${group.season}`}</span>
                  <span className={styles.seasonCount}>{group.patchCount}개 패치</span>
                  {isOpen ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                </button>

                {isOpen && (
                  <div className={styles.seasonPatchList}>
                    {group.dateGroups.map((dateGroup) => (
                      <div key={dateGroup.date} className={styles.seasonDateGroup}>
                        <span className={styles.seasonDateLabel}>{dateGroup.date}</span>
                        {dateGroup.patches.map((patch) => {
                          const isSelected = selectedPatchVersion === patch.version

                          return (
                            <button
                              key={patch.version}
                              type="button"
                              className={`${styles.seasonPatchButton} ${isSelected ? styles.selectedSeasonPatch : ''}`}
                              onClick={() => onPatchSelect(patch.version)}
                              aria-pressed={isSelected}
                            >
                              <span>{patch.version}</span>
                              <strong>{patch.title}</strong>
                              {patch.status === '현재' && <small className={styles.currentPatchPill}>현재 패치</small>}
                            </button>
                          )
                        })}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </section>

      <section className={styles.insightPanel}>
        <span className={styles.sectionLabel}>요약</span>
        <h2>이번 패치 핵심</h2>
        {summaryText && <p className={styles.insightSummary}>{summaryText}</p>}

        {insightItems.length > 0 && (
          <div className={styles.insightSection}>
            <span className={styles.insightSectionTitle}>주요 변경</span>
            <ul>
              {insightItems.map((highlight) => (
                <li key={highlight.label}>
                  <button
                    type="button"
                    className={styles.insightButton}
                    onClick={() => onInsightSelect(highlight.category)}
                  >
                    <CheckCircle2 size={16} />
                    <span>{highlight.label}</span>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}

      </section>
    </aside>
  )
}

export default PatchSideRail
