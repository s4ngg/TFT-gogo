import { Bot, ChevronRight, Sparkles, TrendingUp } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import ChampionCard from '../../../components/common/ChampionCard'
import TierBadge from '../../../components/common/TierBadge'
import TraitHexBadge from '../../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../../hooks/useMetaSnapshot'
import type { AiRecommendDeckReason } from '../../../api/aiRecommendApi'
import styles from '../AiRecommend.module.css'

interface AiRecommendedDecksProps {
  deckReasons: AiRecommendDeckReason[]
}

function AiRecommendedDecks({ deckReasons }: AiRecommendedDecksProps) {
  const navigate = useNavigate()
  const { data: metaDeckResponse } = useMetaSnapshot()
  const metaDecks = metaDeckResponse?.decks ?? []
  const topDecks = metaDecks.filter((d) => d.grade === 'S' || d.grade === 'A').slice(0, 3)

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <Sparkles size={17} />
        <h2>AI 맞춤 덱 추천</h2>
        <span className={styles.panelSub}>내 플레이 스타일 + 현재 메타 기반</span>
      </div>
      <div className={styles.aiDeckList}>
        {topDecks.map((deck, i) => {
          const meta = deckReasons.find((reason) => reason.deckRank === deck.rank)
            ?? { isPatchTrend: false, reason: '현재 메타 기반 추천' }
          return (
            <div
              key={deck.rank}
              className={`${styles.aiDeckCard} ${meta.isPatchTrend ? styles.aiDeckCardPatch : ''}`}
              onClick={() => navigate(`/decks/${deck.rank}`)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => e.key === 'Enter' && navigate(`/decks/${deck.rank}`)}
            >
              <div className={styles.aiDeckTop}>
                <span className={styles.aiDeckBadge}>추천 #{i + 1}</span>
                <TierBadge value={deck.grade} />
                <span className={styles.aiDeckName}>{deck.name}</span>
                {meta.isPatchTrend && (
                  <span className={styles.patchBadge}>
                    <TrendingUp size={12} /> 패치 후 상승 중
                  </span>
                )}
                <span className={styles.aiDeckArrow}><ChevronRight size={16} /></span>
              </div>
              <div className={styles.aiDeckTraits}>
                {deck.traits.slice(0, 4).map((t) => (
                  <TraitHexBadge key={t.name} count={t.count} iconUrl={t.iconUrl} name={t.name} tone={t.tone} />
                ))}
              </div>
              <div className={styles.aiDeckChampions}>
                {deck.champions.map((c, ci) => (
                  <ChampionCard key={c.name} imageUrl={c.imageUrl} items={c.items} label={c.name} stars={c.stars} toneIndex={ci} />
                ))}
              </div>
              <div className={styles.aiDeckStats}>
                <span>승률 <b className={styles.green}>{deck.winRate}</b></span>
                <span>평균 등수 <b className={styles.purple}>{deck.avgPlace}</b></span>
                <span>픽률 <b className={styles.gold}>{deck.pickRate}</b></span>
              </div>
              <div className={`${styles.aiReason} ${meta.isPatchTrend ? styles.aiReasonPatch : ''}`}>
                {meta.isPatchTrend ? <TrendingUp size={13} /> : <Bot size={13} />}
                <span>{meta.reason}</span>
              </div>
            </div>
          )
        })}
      </div>
      <p className={styles.aiDisclaimer}>
        * AI 추천은 개인 전적 데이터 + 현재 메타 데이터를 기반으로 생성됩니다.
        실제 Riot API 연동 후 더 정확한 분석이 제공됩니다.
      </p>
    </section>
  )
}

export default AiRecommendedDecks
