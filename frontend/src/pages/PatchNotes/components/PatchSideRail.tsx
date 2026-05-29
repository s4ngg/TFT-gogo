import { CheckCircle2, History } from 'lucide-react'
import type { PatchNoteDetail } from '../../../api/patchNotes'
import { PATCH_FALLBACK_IMAGE } from '../patchNotesImages'
import styles from '../PatchNotes.module.css'

interface PatchSideRailProps {
  patchHistory: PatchNoteDetail[]
  selectedPatch: PatchNoteDetail
  selectedPatchVersion: string
  onPatchSelect: (version: string) => void
}

function PatchSideRail({
  onPatchSelect,
  patchHistory,
  selectedPatch,
  selectedPatchVersion,
}: PatchSideRailProps) {
  return (
    <aside className={styles.sideRail}>
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

      <section className={styles.historyPanel}>
        <div className={styles.historyHeader}>
          <span className={styles.sectionLabel}>히스토리</span>
          <History size={17} />
        </div>
        <h2>이전 패치</h2>
        <div className={styles.historyList}>
          {patchHistory.map((patch) => (
            <button
              key={patch.version}
              type="button"
              className={`${patch.status === '현재' ? styles.currentPatch : ''} ${
                selectedPatchVersion === patch.version ? styles.selectedHistory : ''
              }`}
              onClick={() => onPatchSelect(patch.version)}
              aria-pressed={selectedPatchVersion === patch.version}
            >
              <span className={styles.historyThumb}>
                <img
                  src={patch.imageUrl}
                  alt=""
                  onError={(event) => {
                    event.currentTarget.onerror = null
                    event.currentTarget.src = PATCH_FALLBACK_IMAGE
                  }}
                />
              </span>
              <div>
                <strong>{patch.version}</strong>
                <span>{patch.date}</span>
              </div>
              <p>{patch.title}</p>
              <small>{patch.focus}</small>
            </button>
          ))}
        </div>
      </section>
    </aside>
  )
}

export default PatchSideRail
