import { CalendarDays } from 'lucide-react'
import type { PatchNoteDetail } from '../../../api/patchNotes'
import { PATCH_FALLBACK_IMAGE } from '../patchNotesImages'
import styles from '../PatchNotes.module.css'

interface PatchHeroProps {
  selectedPatch: PatchNoteDetail
}

function PatchHero({ selectedPatch }: PatchHeroProps) {
  return (
    <header className={styles.hero}>
      <div className={styles.heroCopy}>
        <span className={styles.kicker}>
          <CalendarDays size={16} />
          {selectedPatch.version} 패치 노트
        </span>
        <h1>{selectedPatch.title}</h1>
        <p>{selectedPatch.description}</p>
        <div className={styles.heroMeta}>
          <span>적용일 {selectedPatch.date}</span>
        </div>
      </div>

      <aside className={styles.releaseCard} aria-label="현재 패치 요약">
        <div className={styles.releaseArt} aria-hidden="true">
          <img
            src={selectedPatch.imageUrl}
            alt=""
            onError={(event) => {
              event.currentTarget.onerror = null
              event.currentTarget.src = PATCH_FALLBACK_IMAGE
            }}
          />
        </div>
        <div>
          <span className={styles.releaseLabel}>{selectedPatch.status === '현재' ? '현재 버전' : '선택한 버전'}</span>
          <strong>v{selectedPatch.version}</strong>
          <p>{selectedPatch.focus}</p>
        </div>
      </aside>
    </header>
  )
}

export default PatchHero
