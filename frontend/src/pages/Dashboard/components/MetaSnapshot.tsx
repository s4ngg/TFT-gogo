import { ChevronRight, Clock3 } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import ChampionCard from '../../../components/common/ChampionCard'
import TierBadge from '../../../components/common/TierBadge'
import TraitHexBadge from '../../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../../hooks/useMetaSnapshot'
import type { ChampionSummary, TraitSummary } from '../dashboardData'
import styles from '../Dashboard.module.css'

interface TraitsProps {
  values: TraitSummary[]
}

function Traits({ values }: TraitsProps) {
  return (
    <div className={styles.traits}>
      {values.map((trait) => (
        <TraitHexBadge
          count={trait.count}
          iconUrl={trait.iconUrl}
          key={`${trait.name}-${trait.count}`}
          name={trait.name}
          tone={trait.tone}
        />
      ))}
    </div>
  )
}

interface ChampionsProps {
  champions: ChampionSummary[]
}

function Champions({ champions }: ChampionsProps) {
  return (
    <div className={styles.champions}>
      {champions.map((champion, index) => (
        <ChampionCard
          imageUrl={champion.imageUrl}
          items={champion.items}
          key={`${champion.name}-${index}`}
          label={champion.name}
          stars={champion.stars}
          toneIndex={index}
        />
      ))}
    </div>
  )
}

function MetaSnapshot() {
  const { data: metaDecks = [] } = useMetaSnapshot()
  const navigate = useNavigate()

  return (
    <section className={`${styles.panel} ${styles.metaPanel}`}>
      <div className={styles.panelHeading}>
        <div>
          <h2>추천 메타 스냅샷</h2>
          <span>
            <Clock3 size={17} />
            업데이트: 3분 전
          </span>
        </div>
        <button type="button" onClick={() => navigate('/decks')}>
          전체 보기
          <ChevronRight size={20} />
        </button>
      </div>

      <div className={styles.metaFilters}>
        <button type="button" className={styles.selectedFilter}>종합</button>
        <button type="button">상위권</button>
        <button type="button">마스터+</button>
        <span>승률</span>
        <span>TOP 4</span>
        <span>평균 등수</span>
      </div>

      <div className={styles.deckList}>
        {metaDecks.map((deck) => (
          <article className={styles.deckRow} key={deck.rank}>
            <strong className={styles.rankNumber}>{deck.rank}</strong>
            <TierBadge value={deck.grade} />
            <div className={styles.deckInfo}>
              <h3>{deck.name}</h3>
              <Traits values={deck.traits} />
            </div>
            <Champions champions={deck.champions} />
            <b className={styles.winRate}>{deck.winRate}</b>
            <b className={styles.top4}>{deck.top4}</b>
            <ChevronRight className={styles.rowArrow} size={24} />
          </article>
        ))}
      </div>
    </section>
  )
}

export default MetaSnapshot
