import { Bot, Trophy } from 'lucide-react'
import { AppLayout } from '../../components/layout'
import { useAiRecommendQuery } from '../../hooks/useAiRecommendQuery'
import useSummonerStore from '../../store/useSummonerStore'
import { mockAiRecommendation } from '../../mocks/aiRecommendMock'
import type { AiRecommendStats } from '../../api/aiRecommendApi'
import { tftTierEmblemUrl } from '../../api/communityDragonAssets'
import ConnectPrompt from './components/ConnectPrompt'
import StatCard from './components/StatCard'
import DeckPerformance from './components/DeckPerformance'
import AiRecommendedDecks from './components/AiRecommendedDecks'
import styles from './AiRecommend.module.css'

function AiRecommend() {
  const summoner = useSummonerStore((s) => s.summoner)
  const recommendationQuery = useAiRecommendQuery(
    summoner
      ? { gameName: summoner.name, tagLine: summoner.tag, recentGameCount: 20 }
      : null,
  )
  const recommendation = recommendationQuery.data ?? mockAiRecommendation
  const stats: AiRecommendStats = recommendation.stats

  return (
    <AppLayout>
      <div className={styles.page}>
        <div className={styles.pageHeader}>
          <div>
            <h1><Bot size={22} /> AI 덱 추천</h1>
            <p>내 전적을 분석해 현재 메타에 맞는 덱을 추천해 드립니다</p>
          </div>
          {summoner && (
            <div className={styles.summonerBadge}>
              <span className={styles.summonerDot} />
              <span>{summoner.name}</span>
              <span className={styles.summonerTag}>#{summoner.tag}</span>
            </div>
          )}
        </div>

        {!summoner ? (
          <ConnectPrompt />
        ) : (
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
                  {summoner?.tier && (
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
        )}
      </div>
    </AppLayout>
  )
}

export default AiRecommend
