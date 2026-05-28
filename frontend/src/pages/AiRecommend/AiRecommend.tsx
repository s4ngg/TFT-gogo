import { BarChart2, Bot, ChevronRight, Link2, Sparkles, ThumbsDown, ThumbsUp, TrendingUp, Trophy } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import ChampionCard from '../../components/common/ChampionCard'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import { useAiRecommendQuery } from '../../hooks/useAiRecommendQuery'
import useSummonerStore from '../../store/useSummonerStore'
import type { AiRecommendAugment, AiRecommendDeckReason, AiRecommendResponse, AiRecommendStats, AiRecommendTrait } from '../../api/aiRecommendApi'
import { mockAiRecommendation } from '../../mocks/aiRecommendMock'
import styles from './AiRecommend.module.css'

const MIN_TRAIT_GAMES = 5

/* ── 소환사 미연동 화면 ── */
function ConnectPrompt() {
  const navigate = useNavigate()

  return (
    <div className={styles.promptWrap}>
      <div className={styles.promptCard}>
        <div className={styles.promptIcon}>
          <Bot size={40} />
        </div>
        <h2>소환사 전적을 연동하면<br />개인 맞춤 AI 추천을 받을 수 있어요</h2>
        <p>
          최근 20게임을 분석해 내가 자주 쓰는 덱, 승률 높은 증강 조합,
          그리고 나에게 맞는 덱을 AI가 추천해 드립니다.
        </p>
        <div className={styles.promptFeatures}>
          <span><Trophy size={15} /> 내 자주 쓴 덱 분석</span>
          <span><TrendingUp size={15} /> 증강 성적 분석</span>
          <span><Sparkles size={15} /> AI 맞춤 덱 추천</span>
        </div>
        <button
          type="button"
          className={styles.connectBtn}
          onClick={() => navigate('/')}
        >
          <Link2 size={16} />
          홈에서 소환사 검색으로 연동하기
          <ChevronRight size={16} />
        </button>
      </div>
    </div>
  )
}

/* ── 통계 카드 ── */
function StatCard({ label, value, tone = 'default' }: { label: string; value: string; tone?: 'default' | 'purple' | 'green' | 'gold' }) {
  return (
    <div className={styles.statCard} data-tone={tone}>
      <small>{label}</small>
      <strong>{value}</strong>
    </div>
  )
}

/* ── 시너지별 승률 성적 ── */
function TraitPerformanceList({ traits, bad }: { traits: AiRecommendTrait[]; bad?: boolean }) {
  const filtered = traits.filter((t) => t.games >= MIN_TRAIT_GAMES)

  if (filtered.length === 0) {
    return <p className={styles.traitEmpty}>5판 이상 플레이한 시너지가 없어요</p>
  }

  return (
    <div className={styles.myDeckList}>
      {filtered.map((t, i) => (
        <div key={t.name} className={styles.traitRow}>
          <span className={styles.myDeckNum}>{i + 1}</span>
          <TraitHexBadge count={t.count} iconUrl={t.iconUrl} name={t.name} tone={t.tone} />
          <span className={styles.myDeckName}>{t.name}</span>
          <div className={styles.myDeckStats}>
            <span>{t.games}게임</span>
            <span className={styles.myAvg}>평균 {t.avgPlace}등</span>
            <span className={bad ? styles.myBadTop4 : styles.myTop4}>TOP4 {t.top4Rate}</span>
          </div>
        </div>
      ))}
    </div>
  )
}

function DeckPerformance({ goodTraits, badTraits }: { goodTraits: AiRecommendTrait[]; badTraits: AiRecommendTrait[] }) {
  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <BarChart2 size={17} />
        <h2>내 시너지별 승률 성적</h2>
        <span className={styles.panelSub}>최근 20게임 기준</span>
      </div>
      <div className={styles.deckPerfSplit}>
        <div className={styles.deckPerfCol}>
          <div className={`${styles.deckPerfColHead} ${styles.deckPerfGood}`}>
            <ThumbsUp size={14} /> 잘 맞는 시너지
          </div>
          <TraitPerformanceList traits={goodTraits} />
        </div>
        <div className={styles.deckPerfDivider} />
        <div className={styles.deckPerfCol}>
          <div className={`${styles.deckPerfColHead} ${styles.deckPerfBad}`}>
            <ThumbsDown size={14} /> 잘 안 맞는 시너지
          </div>
          <TraitPerformanceList traits={badTraits} bad />
        </div>
      </div>
    </section>
  )
}

/* ── 증강 분석 ── */
function getAugmentTone(augment: AiRecommendAugment, best: AiRecommendAugment, worst: AiRecommendAugment) {
  if (augment.name === best.name) return 'best'
  if (augment.name === worst.name) return 'worst'
  return 'default'
}

function AugmentAnalysis({ augments }: { augments: AiRecommendAugment[] }) {
  const best = augments[0]
  const worst = augments[augments.length - 1]

  if (!best || !worst) return null

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <TrendingUp size={17} />
        <h2>증강 성적 분석</h2>
        <span className={styles.panelSub}>평균 등수 낮을수록 좋음</span>
      </div>
      <div className={styles.augmentList}>
        {augments.map((aug, i) => {
          const avgNum = parseFloat(aug.avgPlace)
          const tone = getAugmentTone(aug, best, worst)
          return (
            <div key={aug.name} className={styles.augRow}>
              <span className={styles.augRank}>{i + 1}</span>
              <span className={styles.augIcon}>{aug.icon}</span>
              <span className={styles.augName}>{aug.name}</span>
              <progress className={styles.augProgress} data-tone={tone} value={avgNum} max={8} />
              <span className={styles.augPlace} data-tone={tone}>{aug.avgPlace}등</span>
              <span className={styles.augGames}>{aug.games}게임</span>
            </div>
          )
        })}
      </div>
      <p className={styles.augTip}>
        💡 <b>{best.name}</b> 증강을 먹었을 때 평균 {best.avgPlace}등으로 가장 좋은 성적을 냈어요!
      </p>
    </section>
  )
}

/* ── AI 추천 덱 ── */
function AiRecommendedDecks({ deckReasons }: { deckReasons: AiRecommendDeckReason[] }) {
  const navigate = useNavigate()
  const { data: metaDeckResponse } = useMetaSnapshot()
  const metaDecks = metaDeckResponse?.decks ?? []
  const topDecks = metaDecks.filter((d) => d.grade === 'S' || d.grade === 'A+').slice(0, 3)

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

/* ── 메인 ── */
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
            {/* 최근 전적 요약 */}
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
            </section>

            <DeckPerformance goodTraits={recommendation.goodTraits} badTraits={recommendation.badTraits} />
            <AugmentAnalysis augments={recommendation.augments} />

            <AiRecommendedDecks deckReasons={recommendation.deckReasons} />
          </div>
        )}
      </div>
    </AppLayout>
  )
}

export default AiRecommend
