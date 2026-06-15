import { Bot, Trophy } from 'lucide-react'
import { AppLayout } from '../../components/layout'
import { useAiRecommendQuery } from '../../hooks/useAiRecommendQuery'
import useAuthStore from '../../store/useAuthStore'
import useSummonerStore from '../../store/useSummonerStore'
import type { AiRecommendStats } from '../../api/aiRecommendApi'
import { tftTierEmblemUrl } from '../../api/communityDragonAssets'
import ConnectPrompt from './components/ConnectPrompt'
import StatCard from './components/StatCard'
import DeckPerformance from './components/DeckPerformance'
import AiRecommendedDecks from './components/AiRecommendedDecks'
import styles from './AiRecommend.module.css'

function AiRecommend() {
  const token = useAuthStore((s) => s.token)
  const summoner = useSummonerStore((s) => s.summoner)

  const recommendationQuery = useAiRecommendQuery(
    summoner ? { gameName: summoner.name, tagLine: summoner.tag } : null,
    token,
  )

  const renderContent = () => {
    if (!token) {
      return <ConnectPrompt variant="login" />
    }

    if (!summoner) {
      return <ConnectPrompt variant="summoner" />
    }

    if (recommendationQuery.isPending) {
      return <p className={`${styles.statusMessage} ${styles.loading}`}>AI 분석 중...</p>
    }

    if (recommendationQuery.isError) {
      return (
        <p className={`${styles.statusMessage} ${styles.error}`}>
          AI 추천을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
        </p>
      )
    }

    if (!recommendationQuery.data) {
      return (
        <p className={`${styles.statusMessage} ${styles.empty}`}>
          랭크 전적이 부족해 AI 추천을 생성할 수 없습니다. 랭크 게임을 더 플레이해 주세요.
        </p>
      )
    }

    const recommendation = recommendationQuery.data
    const stats: AiRecommendStats = recommendation.stats

    return (
      <div className={styles.content}>
        <section className={`${styles.panel} ${styles.summaryPanel}`}>
          <div className={styles.panelHead}>
            <Trophy size={17} />
            <h2>최근 {stats.recentGames}게임 요약</h2>
          </div>
          <div className={styles.summaryStats}>
            <StatCard label="평균 등수" value={`${stats.avgPlace}등`} tone="purple" />
            <StatCard label="TOP 4율" value={stats.top4Rate} tone="green" />
            <StatCard label="1등 비율" value={stats.winRate} tone="gold" />
            <StatCard label="분석 게임" value={`${stats.recentGames}게임`} />
          </div>
          {stats.recentPlacements && stats.recentPlacements.length > 0 && (
            <div className={styles.placementsRow}>
              <span className={styles.placementsLabel}>최근 등수</span>
              <div className={styles.placementsGrid}>
                {[stats.recentPlacements.slice(0, 10), stats.recentPlacements.slice(10, 20)].map((row, rowIdx) => (
                  <div key={rowIdx} className={styles.placementBadges}>
                    {row.map((place, i) => (
                      <span
                        key={i}
                        className={styles.placementBadge}
                        data-top4={place <= 4 ? 'true' : 'false'}
                        data-first={place === 1 ? 'true' : 'false'}
                      >
                        {place}
                      </span>
                    ))}
                  </div>
                ))}
              </div>
              {summoner.tier && (
                <div className={styles.tierInfo}>
                  <img
                    src={tftTierEmblemUrl(summoner.emblemKey)}
                    alt={summoner.tier}
                    className={styles.tierEmblem}
                  />
                  <div className={styles.tierText}>
                    <span className={styles.tierName}>{summoner.tier}</span>
                    <span className={styles.tierLp}>{summoner.lp} LP</span>
                  </div>
                </div>
              )}
            </div>
          )}
        </section>

        <DeckPerformance goodTraits={recommendation.goodTraits} badTraits={recommendation.badTraits} />
        <AiRecommendedDecks deckReasons={recommendation.deckReasons} />
      </div>
    )
  }

  return (
    <AppLayout>
      <div className={styles.page}>
        <div className={styles.pageHeader}>
          <div>
            <h1><Bot size={22} /> AI 덱 추천</h1>
            <p>내 전적을 분석해 현재 메타에 맞는 덱을 추천해 드립니다</p>
          </div>
          {summoner && token && (
            <div className={styles.summonerBadge}>
              <span className={styles.summonerDot} />
              <span>{summoner.name}</span>
              <span className={styles.summonerTag}>#{summoner.tag}</span>
            </div>
          )}
        </div>

        {renderContent()}
      </div>
    </AppLayout>
  )
}

export default AiRecommend
