import { ChevronDown, ChevronUp, Coins, RefreshCcw, Search, Swords } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { communityDragonProfileIconUrl, itemsFromUrls, tftChampSquareUrl, tftTierEmblemUrl, tftTraitIconUrl } from '../../api/communityDragonAssets'
import { AppLayout } from '../../components/layout'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import ChampionCard from '../../components/common/ChampionCard'
import useSummonerStore from '../../store/useSummonerStore'
import { useSummonerProfile } from '../../hooks/useSummonerProfile'
import { useMatchHistory } from '../../hooks/useMatchHistory'
import { getMatchHistory } from '../../api/summonerApi'
import type { MatchSummaryResponse, GameType } from '../../api/summonerApi'
import { parseSummonerInput, summonerPath } from '../../utils/summonerSearch'
import styles from './SummonerDetail.module.css'

const TIER_KO: Record<string, string> = {
  IRON: '아이언', BRONZE: '브론즈', SILVER: '실버', GOLD: '골드',
  PLATINUM: '플래티넘', EMERALD: '에메랄드', DIAMOND: '다이아몬드',
  MASTER: '마스터', GRANDMASTER: '그랜드마스터', CHALLENGER: '챌린저',
}

type GameTypeFilter = 'ALL' | GameType
const GAME_TYPE_FILTERS: GameTypeFilter[] = ['ALL', 'RANKED', 'NORMAL']

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

function detailRankClass(n: number) {
  if (n === 1) return styles.detailRankGold
  if (n === 2) return styles.detailRankSilver
  if (n === 3) return styles.detailRankBronze
  return ''
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
        <span>챔피언</span><span>킬</span><span>잔여골드</span>
      </div>
      {match.participants.map((p) => {
        const isMe = p.puuid === myPuuid
        return (
          <div key={p.puuid} className={`${styles.matchDetailRow} ${isMe ? styles.myMatchDetailRow : ''}`}>
            <span className={`${styles.detailRank} ${detailRankClass(p.placement)}`}>{p.placement}위</span>
            <div className={styles.detailPlayer}>
              <span className={styles.detailName}>{p.riotIdGameName}</span>
              <span className={styles.detailTag}>#{p.riotIdTagline}</span>
            </div>
            <span className={styles.detailStage}>{p.stage}</span>
            <div className={styles.detailTraits}>
              {p.traits.slice(0, 3).map((tr) => (
                <TraitHexBadge
                  key={tr.traitId}
                  count={tr.count}
                  iconUrl={tftTraitIconUrl(tr.traitId)}
                  name={tr.name}
                  tone={tr.tone}
                />
              ))}
            </div>
            <div className={styles.detailUnits}>
              {p.units.map((unit, i) => (
                <ChampionCard
                  key={`${unit.characterId}-${i}`}
                  imageUrl={unit.imageUrl || tftChampSquareUrl(unit.characterId)}
                  stars={unit.stars}
                  label=""
                  items={itemsFromUrls(unit.itemImageUrls)}
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

/* ── 소환사 없음 ── */
function EmptyState({ name, tag }: { name: string; tag: string }) {
  return (
    <div className={styles.emptyState}>
      <p className={styles.emptyTitle}>소환사를 찾을 수 없습니다</p>
      <p className={styles.emptyDesc}>
        <strong>{name}#{tag}</strong>에 해당하는 소환사가 존재하지 않거나 한국 서버에 등록되지 않았습니다.
        소환사명과 태그를 다시 확인해 주세요.
      </p>
    </div>
  )
}

/* ── 메인 ── */
function SummonerDetail() {
  const { gameName, tagLine } = useParams<{ gameName: string; tagLine: string }>()
  const [query, setQuery] = useState('')
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [allMatches, setAllMatches] = useState<MatchSummaryResponse[]>([])
  const [nextStart, setNextStart] = useState(30)
  const [hasMore, setHasMore] = useState(false)
  const [isLoadingMore, setIsLoadingMore] = useState(false)
  const [gameTypeFilter, setGameTypeFilter] = useState<GameTypeFilter>('ALL')
  const navigate = useNavigate()
  const setSummoner = useSummonerStore((s) => s.setSummoner)

  const name = gameName ?? ''
  const tag = tagLine ?? ''

  const { data: profile, isError: profileNotFound } = useSummonerProfile(name, tag)
  const {
    data: initialMatches = [],
    refetch: refetchMatches,
    isRefetching: isMatchesRefetching,
  } = useMatchHistory(name, tag)

  useEffect(() => {
    setAllMatches(initialMatches)
    setHasMore(initialMatches.length === 30)
    setNextStart(30)
  }, [initialMatches])

  async function handleLoadMore() {
    setIsLoadingMore(true)
    const more = await getMatchHistory(name, tag, nextStart)
    setAllMatches((prev) => [...prev, ...more])
    setHasMore(more.length === 30)
    setNextStart((prev) => prev + 30)
    setIsLoadingMore(false)
  }

  const tierKo = TIER_KO[profile?.tier ?? ''] ?? profile?.tier ?? '-'
  const total = profile ? (profile.wins + profile.losses) : 0
  const winRate = total > 0 ? Math.round((profile!.wins / total) * 100) : 0

  const filteredMatches = gameTypeFilter === 'ALL'
    ? allMatches
    : allMatches.filter((m) => m.gameType === gameTypeFilter)

  useEffect(() => {
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
  }, [profile, name, tag, tierKo, setSummoner])

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    const parsed = parseSummonerInput(query)
    if (!parsed) return
    navigate(summonerPath(parsed.gameName, parsed.tagLine))
    setQuery('')
  }

  function handleFilterChange(filter: GameTypeFilter) {
    setGameTypeFilter(filter)
    setExpandedId(null)
  }

  return (
    <AppLayout>
      <div className={styles.page}>
        <form className={styles.topSearch} onSubmit={handleSearch}>
          <input placeholder="소환사명#태그 검색" value={query} onChange={(e) => setQuery(e.target.value)} />
          <button type="submit" aria-label="검색"><Search size={20} /></button>
        </form>

        {profileNotFound ? (
          <EmptyState name={name} tag={tag} />
        ) : (
          <>
            {/* 프로필 카드 */}
            <section className={`${styles.profileCard} ${!profile?.tier ? styles.profileCardNoEmblem : ''}`}>
              <div className={styles.profileIconWrap}>
                <img
                  className={styles.profileIcon}
                  src={communityDragonProfileIconUrl(profile?.profileIconId ?? 1)}
                  alt="프로필 아이콘"
                />
                <span className={styles.profileLevel}>{profile?.summonerLevel ?? '-'}</span>
              </div>
              {profile?.tier && (
                <div
                  className={styles.emblem}
                  style={{ backgroundImage: `url(${tftTierEmblemUrl(profile.tier)})` }}
                />
              )}
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
                <button
                  type="button"
                  className={styles.updateBtn}
                  onClick={() => refetchMatches()}
                  disabled={isMatchesRefetching}
                >
                  <RefreshCcw size={16} className={isMatchesRefetching ? styles.spin : ''} />
                  {isMatchesRefetching ? '갱신 중...' : '전적 업데이트'}
                </button>
              </div>
            </section>

            {/* 많이 플레이한 시너지 / 챔피언 */}
            {profile && (
              <div className={styles.statGrid}>
                <section className={styles.statSection}>
                  <h2 className={styles.statSectionTitle}>많이 플레이한 시너지</h2>
                  <div className={styles.topTraitList}>
                    {(profile.topTraits ?? []).map((tr, i) => (
                      <div key={tr.traitId} className={styles.topTraitRow}>
                        <span className={styles.topRank}>{i + 1}</span>
                        <TraitHexBadge count={tr.count} iconUrl={tftTraitIconUrl(tr.traitId)} name={tr.name} tone={tr.tone} />
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
                    {(profile.topChampions ?? []).map((champ, i) => (
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
              <h2>최근 {Math.min(30, filteredMatches.length)}게임</h2>
              <RecentSummary matches={filteredMatches} />

              {/* 게임 유형 필터 */}
              <div className={styles.filterBar}>
                {GAME_TYPE_FILTERS.map((type) => (
                  <button
                    key={type}
                    type="button"
                    className={`${styles.filterBtn} ${gameTypeFilter === type ? styles.filterBtnActive : ''}`}
                    onClick={() => handleFilterChange(type)}
                  >
                    {type === 'ALL' ? '전체' : type === 'RANKED' ? '랭크' : '일반'}
                  </button>
                ))}
              </div>

              <div className={styles.matchList}>
                {filteredMatches.map((match) => {
                  const isOpen = expandedId === match.matchId
                  return (
                    <div key={match.matchId} className={styles.matchItem}>
                      <article
                        className={`${styles.matchRow} ${placementTone(match.placement)} ${isOpen ? styles.matchRowOpen : ''}`}
                        role="button"
                        tabIndex={0}
                        onClick={() => setExpandedId(isOpen ? null : match.matchId)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault()
                            setExpandedId(isOpen ? null : match.matchId)
                          }
                        }}
                      >
                        <div className={styles.placementBadge}>
                          <span>{match.placement}위</span>
                        </div>
                        <div className={styles.matchMeta}>
                          <p className={styles.deckName}>
                            <span className={`${styles.gameTypeBadge} ${match.gameType === 'RANKED' ? styles.gameTypeBadgeRanked : styles.gameTypeBadgeNormal}`}>
                              {match.gameType === 'RANKED' ? '랭크' : '일반'}
                            </span>
                            {match.compositionName}
                          </p>
                          <p className={styles.timeAgo}>{timeAgo(match.gameDateTime)}</p>
                        </div>
                        <div className={styles.unitList}>
                          {match.units.map((unit, i) => (
                            <ChampionCard
                              key={`${unit.characterId}-${i}`}
                              imageUrl={unit.imageUrl || tftChampSquareUrl(unit.characterId)}
                              stars={unit.stars}
                              label={unit.characterId}
                              items={itemsFromUrls(unit.itemImageUrls)}
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
                        <MatchDetailPanel match={match} myPuuid={profile?.puuid ?? ''} />
                      )}
                    </div>
                  )
                })}
              </div>
              {hasMore && (
                <button
                  type="button"
                  className={styles.loadMoreBtn}
                  onClick={handleLoadMore}
                  disabled={isLoadingMore}
                >
                  {isLoadingMore ? '불러오는 중...' : '30개 더 보기'}
                </button>
              )}
            </section>
          </>
        )}
      </div>
    </AppLayout>
  )
}

function costColor(cost: number) {
  const map: Record<number, string> = { 1: '#808080', 2: '#3cb371', 3: '#4169e1', 4: '#9932cc', 5: '#ffd700' }
  return map[cost] ?? '#808080'
}

export default SummonerDetail
