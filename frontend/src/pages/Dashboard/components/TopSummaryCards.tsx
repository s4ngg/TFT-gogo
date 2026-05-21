import { ClipboardList, RotateCcw, Search } from 'lucide-react'
import { useState } from 'react'
import useSummonerStore, { type SummonerRank } from '../../../store/useSummonerStore'
import styles from '../Dashboard.module.css'

const TIER_COLORS: Record<string, string> = {
  challenger:   '#F0C040',
  grandmaster:  '#CF3434',
  master:       '#A855F7',
  diamond:      '#4A90D9',
  emerald:      '#2ECC71',
  platinum:     '#1ABC9C',
  gold:         '#F1C40F',
  silver:       '#BDC3C7',
  bronze:       '#CD7F32',
  iron:         '#8E8E8E',
}

const POPULAR_SEARCHES = ['정동글#KR1', '새벽의달#KR', '응의자#KR1', 'TFT잘하고싶다#1234']

// TODO: 실제 Riot API 연동 후 교체
function mockSearchSummoner(name: string, tag: string): SummonerRank {
  return { name, tag, tier: '플래티넘 II', tierKey: 'platinum', lp: 45, wins: 123, losses: 98 }
}

function PatchMetaCard() {
  return (
    <section className={`${styles.panel} ${styles.patchCard}`}>
      <div className={styles.patchEmblemArt} aria-hidden="true" />
      <div className={styles.patchCopy}>
        <h2>17.3 추천 메타</h2>
        <p>5월 20일 업데이트</p>
      </div>
      <button type="button">
        <ClipboardList size={19} />
        패치 노트 보기
      </button>
    </section>
  )
}

function SummonerPanel() {
  const [query, setQuery] = useState('')
  const { summoner, recentSearches, setSummoner, clearSummoner, addRecentSearch } = useSummonerStore()

  function handleSearch(input: string) {
    const trimmed = input.trim()
    if (!trimmed) return
    const [name = trimmed, tag = 'KR1'] = trimmed.split('#')
    const full = trimmed.includes('#') ? trimmed : `${name}#${tag}`
    addRecentSearch(full)
    setSummoner(mockSearchSummoner(name, tag))
    setQuery('')
  }

  if (summoner) {
    const winRate = Math.round((summoner.wins / (summoner.wins + summoner.losses)) * 100)
    const color = TIER_COLORS[summoner.tierKey] ?? '#b5bdc5'
    return (
      <section className={`${styles.panel} ${styles.summonerResult}`}>
        <div className={styles.summonerEmblem} style={{ '--tier-color': color } as React.CSSProperties} />
        <div className={styles.summonerMeta}>
          <p className={styles.summonerName}>
            {summoner.name}<span className={styles.summonerTag}>#{summoner.tag}</span>
          </p>
          <p className={styles.summonerTier} style={{ color }}>
            {summoner.tier}<span className={styles.summonerLp}>{summoner.lp} LP</span>
          </p>
        </div>
        <div className={styles.summonerStats}>
          <div className={styles.statWinLoss}>
            <span>{summoner.wins}승</span>
            <span>{summoner.losses}패</span>
          </div>
          <div
            className={styles.statWinRate}
            style={{ color: winRate >= 55 ? '#1abc9c' : '#b5bdc5' }}
          >
            {winRate}%
          </div>
        </div>
        <button type="button" className={styles.resetSearchBtn} onClick={clearSummoner}>
          <RotateCcw size={13} />
          다시 검색
        </button>
      </section>
    )
  }

  const tagList = recentSearches.length > 0 ? recentSearches : POPULAR_SEARCHES
  const tagLabel = recentSearches.length > 0 ? '최근 검색' : '인기 검색'

  return (
    <section className={`${styles.panel} ${styles.searchPanel}`}>
      <h1>소환사 전적 검색</h1>
      <p>소환사명, 태그#KR 등을 입력하세요</p>
      <form
        className={styles.searchBox}
        onSubmit={(e) => { e.preventDefault(); handleSearch(query) }}
      >
        <input
          aria-label="소환사명 검색"
          placeholder="소환사명#태그 입력"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit" aria-label="검색">
          <Search size={28} />
        </button>
      </form>
      <div className={styles.searchTags}>
        <span>{tagLabel}</span>
        {tagList.map((tag) => (
          <button key={tag} type="button" onClick={() => handleSearch(tag)}>
            {tag}
          </button>
        ))}
      </div>
    </section>
  )
}

function Mascot() {
  return <div className={styles.apiMascotArt} aria-hidden="true" />
}

function RiotApiStatusCard() {
  return (
    <section className={`${styles.panel} ${styles.apiPanel}`}>
      <div className={styles.apiText}>
        <h2>
          <span />
          Riot API 연동
        </h2>
        <p>마지막 갱신: 1분 전</p>
        <div className={styles.apiStats}>
          <div>
            <small>현재 접속자</small>
            <strong>184,236</strong>
          </div>
          <div>
            <small>대기열</small>
            <strong>없음</strong>
          </div>
        </div>
      </div>
      <Mascot />
    </section>
  )
}

function TopSummaryCards() {
  return (
    <>
      <PatchMetaCard />
      <SummonerPanel />
      <RiotApiStatusCard />
    </>
  )
}

export default TopSummaryCards
