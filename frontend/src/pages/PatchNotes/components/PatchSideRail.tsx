import { CheckCircle2, ChevronDown, ChevronRight, History } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import type { PatchNoteDetail } from '../../../api/patchNotes'
import styles from '../PatchNotes.module.css'

interface PatchSideRailProps {
  patchHistory: PatchNoteDetail[]
  selectedPatch: PatchNoteDetail
  selectedPatchVersion: string
  onPatchSelect: (version: string) => void
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

function PatchSideRail({
  onPatchSelect,
  patchHistory,
  selectedPatch,
  selectedPatchVersion,
}: PatchSideRailProps) {
  const seasonGroups = useMemo(() => buildSeasonGroups(patchHistory), [patchHistory])
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
        <ul>
          {selectedPatch.highlights.map((highlight) => (
            <li key={highlight}>
              <CheckCircle2 size={16} />
              <span>{highlight}</span>
            </li>
          ))}
        </ul>
      </section>
    </aside>
  )
}

export default PatchSideRail
