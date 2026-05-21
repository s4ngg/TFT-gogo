import { RefreshCcw, Search } from 'lucide-react'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import styles from './Summoner.module.css'

const DUMMY_TIER = {
  tier: '다이아몬드',
  division: 'IV',
  lp: 45,
  wins: 123,
  losses: 98,
}

const DUMMY_MATCHES = [
  { id: 1, placement: 1, lpDelta: 20, timeAgo: '3분 전', deckName: '선봉대 벡스', augments: ['실버 티켓', '전사의 방패', '퀵실버'] },
  { id: 2, placement: 2, lpDelta: 12, timeAgo: '1시간 전', deckName: '6암흑의 별 진', augments: ['마법사의 모자', '아이오니아의 불꽃', '블루 버프'] },
  { id: 3, placement: 4, lpDelta: 5, timeAgo: '2시간 전', deckName: '정령족 코르키 백류', augments: ['망가진 시계', '수호자의 검', '원소 주입'] },
  { id: 4, placement: 6, lpDelta: -15, timeAgo: '3시간 전', deckName: '승격자 마스터 이', augments: ['마나 흡수', '에너지 전환', '강철 의지'] },
  { id: 5, placement: 7, lpDelta: -20, timeAgo: '4시간 전', deckName: '별들보미 블루 (메탕)', augments: ['강철 연구소', '피의 충동', '신비한 글리프'] },
  { id: 6, placement: 3, lpDelta: 8, timeAgo: '5시간 전', deckName: '8오소 웜풀', augments: ['황금 알계란', '시간 왜곡 포션', '지식 결정'] },
  { id: 7, placement: 5, lpDelta: -12, timeAgo: '어제', deckName: '4그림자 암살자', augments: ['수확의 낫', '마법 진단서', '신비의 힘'] },
  { id: 8, placement: 1, lpDelta: 22, timeAgo: '어제', deckName: '발명의 대가 하이머딩거', augments: ['세상의 파멸', '유리 대포', '적의 창'] },
]

function placementTone(n: number) {
  if (n === 1) return styles.gold
  if (n <= 4) return styles.top4
  return styles.bot4
}

function SummonerPage() {
  const { summonerName, tagLine } = useParams<{ summonerName: string; tagLine: string }>()
  const [query, setQuery] = useState('')
  const navigate = useNavigate()

  const name = decodeURIComponent(summonerName ?? '')
  const tag = tagLine ?? 'KR1'
  const winRate = Math.round(DUMMY_TIER.wins / (DUMMY_TIER.wins + DUMMY_TIER.losses) * 100)

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = query.trim()
    if (!trimmed) return
    const [n = trimmed, t = 'KR1'] = trimmed.split('#')
    navigate(`/summoner/${encodeURIComponent(n)}/${t}`)
    setQuery('')
  }

  return (
    <AppLayout>
      <div className={styles.page}>
        <form className={styles.topSearch} onSubmit={handleSearch}>
          <input
            placeholder="소환사명#태그 검색"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <button type="submit" aria-label="검색">
            <Search size={20} />
          </button>
        </form>

        <section className={styles.profileCard}>
          <div className={styles.emblem} />
          <div className={styles.profileInfo}>
            <h1>
              {name}
              <span className={styles.tag}>#{tag}</span>
            </h1>
            <p className={styles.tierLine}>
              {DUMMY_TIER.tier} {DUMMY_TIER.division} · {DUMMY_TIER.lp} LP
            </p>
            <p className={styles.recordLine}>
              <span>{DUMMY_TIER.wins}승 {DUMMY_TIER.losses}패</span>
              <span className={styles.winRateText}>승률 {winRate}%</span>
            </p>
          </div>
          <button type="button" className={styles.updateBtn}>
            <RefreshCcw size={16} />
            전적 업데이트
          </button>
        </section>

        <section className={styles.matchSection}>
          <h2>최근 전적</h2>
          <div className={styles.matchList}>
            {DUMMY_MATCHES.map((match) => (
              <article key={match.id} className={`${styles.matchRow} ${placementTone(match.placement)}`}>
                <div className={styles.placementBadge}>
                  <span>{match.placement}</span>
                  <small>{match.placement <= 4 ? 'TOP 4' : 'BOT'}</small>
                </div>
                <div className={styles.matchMeta}>
                  <p className={styles.deckName}>{match.deckName}</p>
                  <p className={styles.timeAgo}>{match.timeAgo}</p>
                </div>
                <div className={styles.augments}>
                  {match.augments.map((aug) => (
                    <div key={aug} className={styles.augIcon} title={aug} />
                  ))}
                </div>
                <div className={`${styles.lpDelta} ${match.lpDelta >= 0 ? styles.lpGain : styles.lpLoss}`}>
                  {match.lpDelta >= 0 ? '+' : ''}{match.lpDelta} LP
                </div>
              </article>
            ))}
          </div>
        </section>
      </div>
    </AppLayout>
  )
}

export default SummonerPage
