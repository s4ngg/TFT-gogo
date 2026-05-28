import { ChevronDown, ChevronUp, Coins, RefreshCcw, Search, Swords } from 'lucide-react'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { communityDragonProfileIconUrl } from '../../api/communityDragonAssets'
import { AppLayout } from '../../components/layout'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import ChampionCard from '../../components/common/ChampionCard'
import useSummonerStore from '../../store/useSummonerStore'
import { useSummonerProfile } from '../../hooks/useSummonerProfile'
import { useMatchHistory } from '../../hooks/useMatchHistory'
import type { MatchSummaryResponse, MatchParticipantResponse } from '../../api/summonerApi'
import styles from './SummonerDetail.module.css'

const TIER_KO: Record<string, string> = {
  IRON: '아이언', BRONZE: '브론즈', SILVER: '실버', GOLD: '골드',
  PLATINUM: '플래티넘', EMERALD: '에메랄드', DIAMOND: '다이아몬드',
  MASTER: '마스터', GRANDMASTER: '그랜드마스터', CHALLENGER: '챌린저',
}

function timeAgo(gameDateTime: number): string {
  const diffMs = Date.now() - gameDateTime
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '방금 전'
  if (diffMin < 60) return `${diffMin}분 전`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}시간 전`
  const diffDay = Math.floor(diffHour / 24)
  return diffDay === 1 ? '어제' : `${diffDay}일 전`
}

function placementTone(n: number) {
  if (n === 1) return styles.gold
  if (n <= 4) return styles.top4
  return styles.bot4
}

/* ── 순위 분포 바 ── */
function RankDistribution({ dist }: { dist: number[] }) {
  const max = Math.max(...dist)
  return (
    <div className={styles.rankDist}>
      {dist.map((count, i) => (
        <div key={i} className={styles.rankDistItem}>
          <div className={styles.rankDistBarWrap}>
            <div
              className={`${styles.rankDistBar} ${i < 4 ? styles.top4Bar : styles.bot4Bar}`}
              style={{ height: `${Math.max(4, (count / max) * 56)}px` }}
            />
          </div>
          <span className={styles.rankDistNum}>{i + 1}</span>
          <span className={styles.rankDistCount}>{count}</span>
        </div>
      ))}
    </div>
  )
}

/* ── 30게임 요약 ── */
function RecentSummary({ matches }: { matches: MatchSummaryResponse[] }) {
  const recent = matches.slice(0, 30)
  const top4 = recent.filter((m) => m.placement <= 4).length
  const avgPlace = recent.length > 0
    ? (recent.reduce((s, m) => s + m.placement, 0) / recent.length).toFixed(1)
    : '-'
  const top4Rate = recent.length > 0 ? ((top4 / recent.length) * 100).toFixed(1) : '0'
  const losses = recent.length - top4

  return (
    <section className={styles.summarySection}>
      <div className={styles.winRateDonut} style={{ '--pct': `${top4Rate}%` } as React.CSSProperties}>
        <div className={styles.winRateInner}>
          <strong className={styles.winRatePct}>{top4Rate}%</strong>
        </div>
      </div>
      <div className={styles.summaryStats}>
        <p className={styles.summaryStatLabel}>순방 확률</p>
        <p className={styles.summaryStatValue}>
          {top4}W {losses}L <span className={styles.summaryStatSub}>({top4Rate}%)</span>
        </p>
        <p className={styles.summaryStatLabel}>평균 순위</p>
        <p className={styles.summaryStatValue}>
          {avgPlace}<span className={styles.summaryStatTh}>th</span> / 8
        </p>
      </div>
    </section>
  )
}

/* ── 매치 상세 패널 ── */
function MatchDetailPanel({ match, myPuuid }: { match: MatchSummaryResponse; myPuuid: string }) {
  return (
    <div className={styles.matchDetailPanel}>
      <div className={styles.matchDetailHeader}>
        <span>#</span><span>소환사</span><span>스테이지</span><span>시너지</span>
        <span>증강</span><span>챔피언</span><span>킬</span><span>잔여골드</span>
      </div>
      {match.participants.map((p) => {
        const isMe = p.puuid === myPuuid
        return (
          <div key={p.puuid} className={`${styles.matchDetailRow} ${isMe ? styles.myMatchDetailRow : ''}`}>
            <span className={styles.detailRank}>{p.placement}위</span>
            <div className={styles.detailPlayer}>
              <span className={styles.detailName}>{p.riotIdGameName}</span>
              <span className={styles.detailTag}>#{p.riotIdTagline}</span>
            </div>
            <span className={styles.detailStage}>{p.stage}</span>
            <div className={styles.detailTraits}>
              {p.traits.slice(0, 3).map((tr) => (
                <div key={tr.traitId} className={styles.detailTraitBadge} title={tr.name}>
                  <img src={tr.iconUrl} alt={tr.name} className={styles.detailTraitIcon} />
                  <span>{tr.count}</span>
                </div>
              ))}
            </div>
            <div className={styles.detailAugments}>
              {p.augments.slice(0, 3).map((aug, i) => (
                <span key={i} className={styles.detailAugIcon}>{aug.replace('TFT17_Augment_', '').slice(0, 2)}</span>
              ))}
            </div>
            <div className={styles.detailUnits}>
              {p.units.map((unit) => (
                <ChampionCard
                  key={unit.characterId}
                  imageUrl={unit.imageUrl}
                  stars={unit.stars}
                  label=""
                  items={unit.itemNames.map((url) => ({
                    imageUrl: url,
                    name: url.split('/').pop()?.split('.')[0] ?? url,
                  }))}
                  toneIndex={0}
                />
              ))}
            </div>
            <span className={styles.detailKills}><Swords size={11} />{p.playersEliminated}</span>
            <span className={styles.detailGold}><Coins size={11} />{p.goldLeft}</span>
          </div>
        )
      })}
    </div>
  )
}

/* ── 메인 ── */
function SummonerDetail() {
  const { gameName, tagLine } = useParams<{ gameName: string; tagLine: string }>()
  const [query, setQuery] = useState('')
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [visibleCount, setVisibleCount] = useState(30)
  const navigate = useNavigate()
  const setSummoner = useSummonerStore((s) => s.setSummoner)

  const name = decodeURIComponent(gameName ?? '')
  const tag = tagLine ?? 'KR1'

  const { data: profile } = useSummonerProfile(name, tag)
  const { data: matches = [], refetch: refetchMatches } = useMatchHistory(name, tag)

  const tierKo = TIER_KO[profile?.tier ?? ''] ?? profile?.tier ?? '-'
  const winRate = profile
    ? Math.round((profile.wins / (profile.wins + profile.losses)) * 100)
    : 0

  // 소환사 스토어 갱신
  if (profile) {
    setSummoner({
      name,
      tag,
      tier: `${tierKo} ${profile.rank}`,
      lp: profile.leaguePoints,
      wins: profile.wins,
      losses: profile.losses,
      emblemKey: (profile.tier ?? '').toLowerCase(),
    })
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = query.trim()
    if (!trimmed) return
    const [n = trimmed, tg = 'KR1'] = trimmed.split('#')
    navigate(`/summoner/${encodeURIComponent(n)}/${tg}`)
    setQuery('')
  }

  return (
    <AppLayout>
      <div className={styles.page}>
        <form className={styles.topSearch} onSubmit={handleSearch}>
          <input placeholder="소환사명#태그 검색" value={query} onChange={(e) => setQuery(e.target.value)} />
          <button type="submit" aria-label="검색"><Search size={20} /></button>
        </form>

        {/* 프로필 카드 */}
        <section className={styles.profileCard}>
          <div className={styles.profileIconWrap}>
            <img
              className={styles.profileIcon}
              src={communityDragonProfileIconUrl(profile?.profileIconId ?? 1)}
              alt="프로필 아이콘"
            />
            <span className={styles.profileLevel}>{profile?.summonerLevel ?? '-'}</span>
          </div>
          <div className={styles.emblem} />
          <div className={styles.profileInfo}>
            <h1>{name}<span className={styles.tag}>#{tag}</span></h1>
            <p className={styles.tierLine}>
              {tierKo} {profile?.rank} · {profile?.leaguePoints ?? '-'} LP
            </p>
            <p className={styles.recordLine}>
              <span>{profile?.wins ?? '-'}승 {profile?.losses ?? '-'}패</span>
              <span className={styles.winRateText}>승률 {winRate}%</span>
              <span className={styles.avgPlaceText}>평균 {profile?.avgPlace ?? '-'}등</span>
              <span className={styles.top4Text}>TOP4 {profile?.top4Rate ?? '-'}%</span>
            </p>
          </div>
          <div className={styles.profileRight}>
            <button type="button" className={styles.updateBtn} onClick={() => refetchMatches()}>
              <RefreshCcw size={16} />전적 업데이트
            </button>
            {profile && <RankDistribution dist={profile.rankDistribution} />}
          </div>
        </section>

        {/* 많이 플레이한 시너지 / 챔피언 */}
        {profile && (
          <div className={styles.statGrid}>
            <section className={styles.statSection}>
              <h2 className={styles.statSectionTitle}>많이 플레이한 시너지</h2>
              <div className={styles.topTraitList}>
                {profile.topTraits.map((tr, i) => (
                  <div key={tr.traitId} className={styles.topTraitRow}>
                    <span className={styles.topRank}>{i + 1}</span>
                    <TraitHexBadge count={tr.count} iconUrl={tr.iconUrl} name={tr.name} tone={tr.tone} />
                    <span className={styles.topName}>{tr.name}</span>
                    <span className={styles.topGames}>{tr.games}게임</span>
                    <span className={styles.topAvg}>평균 {tr.avgPlace}등</span>
                  </div>
                ))}
              </div>
            </section>

            <section className={styles.statSection}>
              <h2 className={styles.statSectionTitle}>많이 플레이한 챔피언</h2>
              <div className={styles.topChampList}>
                {profile.topChampions.map((champ, i) => (
                  <div key={champ.characterId} className={styles.topChampRow}>
                    <span className={styles.topRank}>{i + 1}</span>
                    <div className={styles.champThumbWrap}>
                      <img className={styles.champThumb} src={champ.imageUrl} alt={champ.name} />
                      <span className={styles.champCost} style={{ background: costColor(champ.cost) }}>
                        {champ.cost}
                      </span>
                    </div>
                    <span className={styles.topName}>{champ.name}</span>
                    <span className={styles.topGames}>{champ.games}게임</span>
                    <span className={styles.topAvg}>평균 {champ.avgPlace}등</span>
                  </div>
                ))}
              </div>
            </section>
          </div>
        )}

        {/* 매치 히스토리 */}
        <section className={styles.matchSection}>
          <h2>최근 {Math.min(30, matches.length)}게임</h2>
          <RecentSummary matches={matches} />
          <div className={styles.matchList}>
            {matches.slice(0, visibleCount).map((match) => {
              const isOpen = expandedId === match.matchId
              return (
                <div key={match.matchId} className={styles.matchItem}>
                  <article
                    className={`${styles.matchRow} ${placementTone(match.placement)} ${isOpen ? styles.matchRowOpen : ''}`}
                    onClick={() => setExpandedId(isOpen ? null : match.matchId)}
                  >
                    <div className={styles.placementBadge}>
                      <span>{match.placement}위</span>
                    </div>
                    <div className={styles.matchMeta}>
                      <p className={styles.deckName}>{match.compositionName}</p>
                      <p className={styles.timeAgo}>{timeAgo(match.gameDateTime)}</p>
                    </div>
                    <div className={styles.unitList}>
                      {match.units.map((unit) => (
                        <ChampionCard
                          key={unit.characterId}
                          imageUrl={unit.imageUrl}
                          stars={unit.stars}
                          label={unit.characterId}
                          items={unit.itemNames.map((url) => ({
                            imageUrl: url,
                            name: url.split('/').pop()?.split('.')[0] ?? url,
                          }))}
                          toneIndex={0}
                        />
                      ))}
                    </div>
                    <div className={styles.matchChevron}>
                      {isOpen
                        ? <ChevronUp size={14} />
                        : <ChevronDown size={14} />}
                    </div>
                  </article>
                  {isOpen && (
                    <MatchDetailPanel match={match} myPuuid="mock-puuid-player-01" />
                  )}
                </div>
              )
            })}
          </div>
          {visibleCount < matches.length && (
            <button
              type="button"
              className={styles.loadMoreBtn}
              onClick={() => setVisibleCount((v) => v + 30)}
            >
              30개 더 보기 ({matches.length - visibleCount}개 남음)
            </button>
          )}
        </section>
      </div>
    </AppLayout>
  )
}

function costColor(cost: number) {
  const map: Record<number, string> = { 1: '#808080', 2: '#3cb371', 3: '#4169e1', 4: '#9932cc', 5: '#ffd700' }
  return map[cost] ?? '#808080'
}

export default SummonerDetail
