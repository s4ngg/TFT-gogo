import { ArrowLeft, BarChart2, Trophy } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import ChampionCard from '../../components/common/ChampionCard'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import styles from './DeckDetail.module.css'

function DeckDetail() {
  const { deckId } = useParams<{ deckId: string }>()
  const navigate = useNavigate()
  const { data: metaDecks = [], isLoading } = useMetaSnapshot()

  const deck = metaDecks.find((d) => String(d.rank) === deckId)

  if (isLoading) {
    return (
      <AppLayout>
        <div className={styles.page}>
          <p className={styles.loading}>불러오는 중...</p>
        </div>
      </AppLayout>
    )
  }

  if (!deck) {
    return (
      <AppLayout>
        <div className={styles.page}>
          <button type="button" className={styles.backBtn} onClick={() => navigate('/decks')}>
            <ArrowLeft size={16} /> 덱모음으로
          </button>
          <p className={styles.notFound}>덱 정보를 찾을 수 없어요.</p>
        </div>
      </AppLayout>
    )
  }

  return (
    <AppLayout>
      <div className={styles.page}>

        {/* 뒤로가기 */}
        <button type="button" className={styles.backBtn} onClick={() => navigate('/decks')}>
          <ArrowLeft size={16} /> 덱모음으로
        </button>

        {/* 헤더 */}
        <div className={styles.header}>
          <TierBadge value={deck.grade} />
          <h1 className={styles.deckName}>{deck.name}</h1>
          <span className={styles.rankLabel}>메타 #{deck.rank}</span>
        </div>

        {/* 스탯 */}
        <div className={styles.statsRow}>
          <div className={styles.statItem}>
            <small>승률</small>
            <strong className={styles.green}>{deck.winRate}</strong>
          </div>
          <div className={styles.statDivider} />
          <div className={styles.statItem}>
            <small>TOP 4</small>
            <strong className={styles.cyan}>{deck.top4}</strong>
          </div>
          <div className={styles.statDivider} />
          <div className={styles.statItem}>
            <small>평균 등수</small>
            <strong className={styles.purple}>{deck.avgPlace}등</strong>
          </div>
          <div className={styles.statDivider} />
          <div className={styles.statItem}>
            <small>픽률</small>
            <strong className={styles.gold}>{deck.pickRate}</strong>
          </div>
        </div>

        {/* 시너지 */}
        <section className={styles.panel}>
          <div className={styles.panelHead}>
            <Trophy size={16} />
            <h2>시너지 구성</h2>
          </div>
          <div className={styles.traitList}>
            {deck.traits.map((t) => (
              <div key={t.name} className={styles.traitItem}>
                <TraitHexBadge count={t.count} iconUrl={t.iconUrl} name={t.name} tone={t.tone} />
                <span className={styles.traitName}>{t.name}</span>
                <span className={styles.traitCount}>{t.count}조각</span>
              </div>
            ))}
          </div>
        </section>

        {/* 챔피언 구성 */}
        <section className={styles.panel}>
          <div className={styles.panelHead}>
            <BarChart2 size={16} />
            <h2>챔피언 구성</h2>
            <span className={styles.panelSub}>추천 아이템 포함</span>
          </div>
          <div className={styles.championGrid}>
            {deck.champions.map((c, i) => (
              <div key={c.name} className={styles.championItem}>
                <ChampionCard
                  imageUrl={c.imageUrl}
                  items={c.items}
                  label={c.name}
                  stars={c.stars}
                  toneIndex={i}
                />
                <span className={styles.championName}>{c.name}</span>
              </div>
            ))}
          </div>
        </section>

      </div>
    </AppLayout>
  )
}

export default DeckDetail
