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

const RANK_FILTER = 'MASTER_PLUS' as const

function AiRecommendedDecks({ deckReasons }: AiRecommendedDecksProps) {
  const navigate = useNavigate()
  const { data: metaDeckResponse, isPending, isError } = useMetaSnapshot(RANK_FILTER)
  const metaDecks = metaDeckResponse?.decks ?? []

  const recommendedDecks = deckReasons
    .map((reason) => ({
      reason,
      deck: metaDecks.find((d) => d.rank === reason.deckRank),
    }))
    .filter((entry): entry is { reason: AiRecommendDeckReason; deck: NonNullable<typeof entry.deck> } =>
      entry.deck !== undefined,
    )

  if (isPending) {
    return (
      <section className={styles.panel}>
        <div className={styles.panelHead}><Sparkles size={17} /><h2>AI 맞춤 덱 추천</h2></div>
        <p className={`${styles.statusMessage} ${styles.loading}`}>메타 덱 데이터 로딩 중...</p>
      </section>
    )
  }

  if (isError) {
    return (
      <section className={styles.panel}>
        <div className={styles.panelHead}><Sparkles size={17} /><h2>AI 맞춤 덱 추천</h2></div>
        <p className={`${styles.statusMessage} ${styles.error}`}>메타 덱 데이터를 불러오지 못했습니다.</p>
      </section>
    )
  }

  if (recommendedDecks.length === 0) {
    return (
      <section className={styles.panel}>
        <div className={styles.panelHead}><Sparkles size={17} /><h2>AI 맞춤 덱 추천</h2></div>
        <p className={`${styles.statusMessage} ${styles.empty}`}>추천 덱 데이터를 찾을 수 없습니다.</p>
      </section>
    )
  }

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <Sparkles size={17} />
        <h2>AI 맞춤 덱 추천</h2>
        <span className={styles.panelSub}>내 플레이 스타일 + 현재 메타 기반</span>
      </div>
      <div className={styles.aiDeckList}>
        {recommendedDecks.map(({ deck, reason }, i) => (
          <button
            key={`${deck.rank}-${i}`}
            type="button"
            className={`${styles.aiDeckCard} ${reason.isPatchTrend ? styles.aiDeckCardPatch : ''}`}
            onClick={() => navigate(`/decks/${RANK_FILTER}/${deck.rank}`)}
          >
            <div className={styles.aiDeckTop}>
              <span className={styles.aiDeckBadge}>추천 #{i + 1}</span>
              <TierBadge value={deck.grade} />
              <span className={styles.aiDeckName}>{deck.name}</span>
              {reason.isPatchTrend && (
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
            <div className={`${styles.aiReason} ${reason.isPatchTrend ? styles.aiReasonPatch : ''}`}>
              {reason.isPatchTrend ? <TrendingUp size={13} /> : <Bot size={13} />}
              <span>{reason.reason}</span>
            </div>
          </button>
        ))}
      </div>
      <p className={styles.aiDisclaimer}>
        * AI 추천은 개인 전적 데이터 + 마스터+ 메타 데이터를 기반으로 생성됩니다.
      </p>
    </section>
  )
}

export default AiRecommendedDecks
