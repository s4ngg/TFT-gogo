import { CalendarDays } from 'lucide-react'
import type { PatchNoteDetail } from '../../../api/patchNotes'
import { PATCH_FALLBACK_IMAGE } from '../patchNotesImages'
import styles from '../PatchNotes.module.css'

interface PatchHeroProps {
  selectedPatch: PatchNoteDetail
}

function PatchHero({ selectedPatch }: PatchHeroProps) {
  const sourceLabel = selectedPatch.importSource === 'RIOT_OFFICIAL' ? 'Riot 공식' : '패치 데이터'
  const releaseSummary = selectedPatch.summary || selectedPatch.focus || selectedPatch.description

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
          {selectedPatch.sourceUrl && (
            <a href={selectedPatch.sourceUrl} target="_blank" rel="noreferrer">
              {sourceLabel} 원문
            </a>
          )}
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
          <p>{releaseSummary}</p>
        </div>
      </aside>
    </header>
  )
}

export default PatchHero
