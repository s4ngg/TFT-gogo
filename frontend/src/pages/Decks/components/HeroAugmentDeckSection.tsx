import type { TFTLocale } from '../../../api/cdragonLocale'
import { useHeroAugmentDecks } from '../../../hooks/useHeroAugmentDecks'
import HeroAugmentDeckCard from './HeroAugmentDeckCard'
import styles from '../Decks.module.css'

interface HeroAugmentDeckSectionProps {
  locale: TFTLocale | undefined
}

function HeroAugmentDeckSection({ locale }: HeroAugmentDeckSectionProps) {
  const { data: decks = [], isLoading } = useHeroAugmentDecks()

  if (isLoading || decks.length === 0) return null

  return (
    <section className={styles.specialSection}>
      <div className={styles.specialHeader}>
        <span className={`${styles.specialBadge} ${styles.heroAugBadge}`}>
          영웅증강 덱
        </span>
        <div className={styles.specialHeaderText}>
          <h2>영웅증강 덱 모음</h2>
          <p>영웅 증강 효과를 극대화하는 덱 빌드 (관리자 큐레이션)</p>
        </div>
      </div>
      <div className={styles.haDeckGrid}>
        {decks.map((deck) => (
          <HeroAugmentDeckCard key={deck.id} deck={deck} locale={locale} />
        ))}
      </div>
    </section>
  )
}

export default HeroAugmentDeckSection
