import { useNavigate } from 'react-router-dom'
import ChampionCard from '../../../components/common/ChampionCard'
import TierBadge from '../../../components/common/TierBadge'
import { getChampionName, getTraitName } from '../../../api/cdragonLocale'
import type { TFTLocale } from '../../../api/cdragonLocale'
import type { MetaDeck, RankFilter } from '../../Dashboard/dashboardData'
import { deckDisplayName, shopChampions, resolveItems } from '../utils/deckListUtils'
import styles from '../Decks.module.css'

const MAX_VISIBLE_TRAITS = 4

const TONE_CLASS_MAP: Record<string, string> = {
  gold: styles.toneGold,
  silver: styles.toneSilver,
  bronze: styles.toneBronze,
}

interface DeckRowProps {
  deck: MetaDeck
  showTier?: boolean
  showRank?: boolean
  locale: TFTLocale | undefined
  rankFilter: RankFilter
}

function DeckRow({ deck, showTier = true, showRank = true, locale, rankFilter }: DeckRowProps) {
  const navigate = useNavigate()

  function handleRowClick() { navigate(`/decks/${rankFilter}/${deck.rank}`) }

  return (
    <tr className={styles.deckRow} onClick={handleRowClick}>
      {showRank && (
        <td>
          <strong className={styles.rank} data-top={deck.rank <= 3 ? deck.rank : undefined}>
            {deck.rank}
          </strong>
        </td>
      )}
      {showTier && <td><TierBadge value={deck.grade} /></td>}
      <td className={styles.nameCol}>
        <span className={styles.deckName}>{deckDisplayName(deck, locale)}</span>
        <span className={styles.traits}>
          {deck.traits.slice(0, MAX_VISIBLE_TRAITS).map((t, i) => (
            <span
              key={`${t.name}-${t.count}-${i}`}
              className={`${styles.traitTag} ${TONE_CLASS_MAP[t.tone] ?? ''}`}
            >
              {t.count} {getTraitName(t.name, locale)}
            </span>
          ))}
        </span>
      </td>
      <td className={styles.champCol}>
        <span className={styles.champions}>
          {shopChampions(deck).slice(0, 9).map((c, i) => (
            <ChampionCard
              key={`${c.name}-${i}`}
              imageUrl={c.imageUrl}
              items={resolveItems(c, locale)}
              label={getChampionName(c.imageUrl, locale, c.name)}
              stars={c.stars}
              cost={c.cost}
            />
          ))}
        </span>
      </td>
      <td className={styles.top4}>{deck.top4}</td>
      <td className={styles.avgPlace}><span className={styles.avgHash}>#</span>{deck.avgPlace}</td>
      <td className={styles.pickRate}>{deck.pickRate}</td>
    </tr>
  )
}

export default DeckRow
