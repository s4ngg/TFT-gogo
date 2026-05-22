import { Bot, ChevronRight, Link2, Sparkles, TrendingUp, Trophy, User } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import TierBadge from '../../components/common/TierBadge'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import { useMetaSnapshot } from '../../hooks/useMetaSnapshot'
import useSummonerStore from '../../store/useSummonerStore'
import styles from './AiRecommend.module.css'

/* ── Mock 개인 데이터 (실제 구현 시 Riot API로 교체) ── */
const MOCK_MY_STATS = {
  recentGames: 20,
  avgPlace: '4.1',
  top4Rate: '58.0%',
  winRate: '22.5%',
}

const MOCK_MY_DECKS = [
  { name: '선봉대 벡스', games: 7, avgPlace: '3.4', top4Rate: '71%', grade: 'S' as const },
  { name: '정령족 코르키 백류', games: 5, avgPlace: '3.9', top4Rate: '64%', grade: 'A+' as const },
  { name: '8요새 럼블', games: 4, avgPlace: '4.5', top4Rate: '52%', grade: 'A' as const },
]

const MOCK_AUGMENTS = [
  { name: '강철의 의지', avgPlace: '2.9', games: 4, icon: '🛡️' },
  { name: '정의의 손길+', avgPlace: '3.2', games: 3, icon: '⚔️' },
  { name: '용의 불꽃', avgPlace: '3.5', games: 5, icon: '🔥' },
  { name: '별의 수호자', avgPlace: '4.8', games: 3, icon: '✨' },
  { name: '전사의 용기', avgPlace: '5.1', games: 4, icon: '🗡️' },
]

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
function StatCard({ label, value, color }: { label: string; value: string; color?: string }) {
  return (
    <div className={styles.statCard}>
      <small>{label}</small>
      <strong style={color ? { color } : undefined}>{value}</strong>
    </div>
  )
}

/* ── 내가 자주 쓴 덱 ── */
function MyDecks() {
  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <User size={17} />
        <h2>내가 자주 쓴 덱 TOP 3</h2>
        <span className={styles.panelSub}>최근 20게임 기준</span>
      </div>
      <div className={styles.myDeckList}>
        {MOCK_MY_DECKS.map((d, i) => (
          <div key={d.name} className={styles.myDeckRow}>
            <span className={styles.myDeckNum}>{i + 1}</span>
            <TierBadge value={d.grade} />
            <span className={styles.myDeckName}>{d.name}</span>
            <div className={styles.myDeckStats}>
              <span>{d.games}게임</span>
              <span className={styles.myAvg}>평균 {d.avgPlace}등</span>
              <span className={styles.myTop4}>TOP4 {d.top4Rate}</span>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

/* ── 증강 분석 ── */
function AugmentAnalysis() {
  const best = MOCK_AUGMENTS[0]
  const worst = MOCK_AUGMENTS[MOCK_AUGMENTS.length - 1]

  return (
    <section className={styles.panel}>
      <div className={styles.panelHead}>
        <TrendingUp size={17} />
        <h2>증강 성적 분석</h2>
        <span className={styles.panelSub}>평균 등수 낮을수록 좋음</span>
      </div>
      <div className={styles.augmentList}>
        {MOCK_AUGMENTS.map((aug, i) => {
          const avgNum = parseFloat(aug.avgPlace)
          const isBest = aug.name === best.name
          const isWorst = aug.name === worst.name
          return (
            <div key={aug.name} className={styles.augRow}>
              <span className={styles.augRank}>{i + 1}</span>
              <span className={styles.augIcon}>{aug.icon}</span>
              <span className={styles.augName}>{aug.name}</span>
              <div className={styles.augBar}>
                <div
                  className={styles.augBarFill}
                  style={{
                    width: `${(avgNum / 8) * 100}%`,
                    background: isBest ? '#04ede0' : isWorst ? '#f87171' : '#a78bfa',
                  }}
                />
              </div>
              <span
                className={styles.augPlace}
                style={{ color: isBest ? '#04ede0' : isWorst ? '#f87171' : '#c0c8d0' }}
              >
                {aug.avgPlace}등
              </span>
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
function AiRecommendedDecks() {
  const { data: metaDecks = [] } = useMetaSnapshot()
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
          const reasons = [
            '내가 자주 쓰는 챔피언 포함',
            '현재 메타 최상위 티어',
            '증강 궁합 최적',
          ]
          return (
            <div key={deck.rank} className={styles.aiDeckCard}>
              <div className={styles.aiDeckTop}>
                <span className={styles.aiDeckBadge}>추천 #{i + 1}</span>
                <TierBadge value={deck.grade} />
                <span className={styles.aiDeckName}>{deck.name}</span>
              </div>
              <div className={styles.aiDeckTraits}>
                {deck.traits.slice(0, 4).map((t) => (
                  <TraitHexBadge key={t.name} count={t.count} iconUrl={t.iconUrl} name={t.name} tone={t.tone} />
                ))}
              </div>
              <div className={styles.aiDeckStats}>
                <span>승률 <b className={styles.green}>{deck.winRate}</b></span>
                <span>평균 등수 <b className={styles.purple}>{deck.avgPlace}</b></span>
                <span>픽률 <b className={styles.gold}>{deck.pickRate}</b></span>
              </div>
              <div className={styles.aiReason}>
                <Bot size={13} />
                <span>{reasons[i]}</span>
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
                <h2>최근 {MOCK_MY_STATS.recentGames}게임 요약</h2>
              </div>
              <div className={styles.summaryStats}>
                <StatCard label="평균 등수" value={`${MOCK_MY_STATS.avgPlace}등`} color="#a78bfa" />
                <StatCard label="TOP 4율" value={MOCK_MY_STATS.top4Rate} color="#04ede0" />
                <StatCard label="1등 비율" value={MOCK_MY_STATS.winRate} color="#f9c860" />
                <StatCard label="분석 게임" value={`${MOCK_MY_STATS.recentGames}게임`} />
              </div>
            </section>

            <div className={styles.twoCol}>
              <MyDecks />
              <AugmentAnalysis />
            </div>

            <AiRecommendedDecks />
          </div>
        )}
      </div>
    </AppLayout>
  )
}

export default AiRecommend
