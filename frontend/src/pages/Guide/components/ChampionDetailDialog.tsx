import { useEffect, useId, useRef } from 'react'
import { Star, X } from 'lucide-react'
import type { ChampionGuide } from '../../../api/guide'
import { ItemIconStrip } from './GuideShared'
import styles from '../Guide.module.css'

interface ChampionDetailDialogProps {
  champion: ChampionGuide
  isFavorite: boolean
  onClose: () => void
  onFavoriteToggle: (championName: string) => void
  onItemSelect: (itemName: string) => void
}

function ChampionDetailDialog({
  champion,
  isFavorite,
  onClose,
  onFavoriteToggle,
  onItemSelect,
}: ChampionDetailDialogProps) {
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const titleId = useId()
  const traitsId = useId()

  useEffect(() => {
    closeButtonRef.current?.focus()

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  return (
    <div className={styles.dialogBackdrop} role="presentation" onClick={onClose}>
      <section
        aria-describedby={traitsId}
        aria-labelledby={titleId}
        aria-modal="true"
        className={styles.championDialog}
        onClick={(event) => event.stopPropagation()}
        role="dialog"
      >
        <button
          aria-label={`${champion.name} 상세 정보 닫기`}
          className={styles.dialogCloseButton}
          onClick={onClose}
          ref={closeButtonRef}
          type="button"
        >
          <X size={17} />
        </button>
        <div className={styles.dialogHero}>
          <img src={champion.imageUrl} alt={champion.name} />
          <div>
            <strong id={titleId}>{champion.name}</strong>
            <span>{champion.role}</span>
            <p id={traitsId}>{champion.traits.join(' / ')}</p>
          </div>
        </div>
        <div className={styles.dialogItems}>
          <b>3신기</b>
          <ItemIconStrip items={champion.bestItems} onItemSelect={onItemSelect} />
        </div>
        <dl className={styles.dialogStats}>
          <div><dt>체력</dt><dd>{champion.stats.hp}</dd></div>
          <div><dt>공격력</dt><dd>{champion.stats.ad}</dd></div>
          <div><dt>공속</dt><dd>{champion.stats.attackSpeed}</dd></div>
          <div><dt>마나</dt><dd>{champion.stats.mana}</dd></div>
          <div><dt>방어</dt><dd>{champion.stats.armor}</dd></div>
          <div><dt>마저</dt><dd>{champion.stats.mr}</dd></div>
        </dl>
        <p className={styles.dialogPosition}>권장 배치: {champion.position}</p>
        <button
          aria-pressed={isFavorite}
          className={`${styles.dialogFavoriteButton} ${isFavorite ? styles.favoriteActive : ''}`}
          onClick={() => onFavoriteToggle(champion.name)}
          type="button"
        >
          <Star size={15} />
          {isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가'}
        </button>
      </section>
    </div>
  )
}

export default ChampionDetailDialog
