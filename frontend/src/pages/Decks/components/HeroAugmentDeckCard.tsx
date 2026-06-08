import { Zap } from 'lucide-react'
import ChampionCard from '../../../components/common/ChampionCard'
import TierBadge from '../../../components/common/TierBadge'
import { getChampionName } from '../../../api/cdragonLocale'
import type { TFTLocale } from '../../../api/cdragonLocale'
import type { HeroAugmentDeckItem } from '../../../api/adminApi'
import type { TierBadgeValue } from '../../../types/badges'
import { parseHeroAugments, parseChampions } from '../utils/deckListUtils'
import styles from '../Decks.module.css'

interface HeroAugmentDeckCardProps {
  deck: HeroAugmentDeckItem
  locale: TFTLocale | undefined
}

function HeroAugmentDeckCard({ deck, locale }: HeroAugmentDeckCardProps) {
  const augments = parseHeroAugments(deck.heroAugments)
  const champs = parseChampions(deck.champions)

  return (
    <div className={styles.haDeckCard}>
      <div className={styles.haDeckCardHead}>
        {deck.grade && <TierBadge value={deck.grade as TierBadgeValue} />}
        <span className={styles.haDeckName}>{deck.name}</span>
      </div>

      {champs.length > 0 && (
        <div className={styles.haDeckChamps}>
          {champs.slice(0, 9).map((c, i) => (
            <ChampionCard
              key={i}
              imageUrl={c.imageUrl}
              label={getChampionName(c.imageUrl, locale, c.name)}
            />
          ))}
        </div>
      )}

      {augments.length > 0 && (
        <div className={styles.haDeckAugments}>
          {augments.map((aug) => (
            <div key={`${aug.championId}-${aug.augmentName}`} className={styles.haDeckAugRow}>
              <Zap size={11} className={styles.haDeckAugIcon} />
              <span className={styles.haDeckAugChamp}>{aug.championName}</span>
              <span className={styles.haDeckAugName}>{aug.augmentName}</span>
            </div>
          ))}
        </div>
      )}

      {deck.description && (
        <p className={styles.haDeckDesc}>{deck.description}</p>
      )}
    </div>
  )
}

export default HeroAugmentDeckCard
