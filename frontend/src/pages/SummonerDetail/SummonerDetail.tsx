import { ChevronDown, ChevronUp, RefreshCcw, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { communityDragonProfileIconUrl, itemsFromUrls, tftChampSquareUrl, tftTierEmblemUrl } from '../../api/communityDragonAssets'
import { AppLayout } from '../../components/layout'
import TraitHexBadge from '../../components/common/TraitHexBadge'
import ChampionCard from '../../components/common/ChampionCard'
import useSummonerStore from '../../store/useSummonerStore'
import { useSummonerProfile } from '../../hooks/useSummonerProfile'
import { useMatchHistory } from '../../hooks/useMatchHistory'
import type { MatchTraitResponse, GameType } from '../../api/summonerApi'
import { refreshSummoner } from '../../api/summonerApi'
import ProfileSkeleton from './components/ProfileSkeleton'
import RecentSummary from './components/RecentSummary'
import MatchDetailPanel from './components/MatchDetailPanel'
import EmptyState from './components/EmptyState'
import RateLimitState from './components/RateLimitState'
import { placementTone, timeAgo, formatDate } from './utils/summonerUtils'
import styles from './SummonerDetail.module.css'

interface HttpError { response?: { status?: number; headers?: Record<string, string> } }

const TIER_KO: Record<string, string> = {
  IRON: '아이언', BRONZE: '브론즈', SILVER: '실버', GOLD: '골드',
  PLATINUM: '플래티넘', EMERALD: '에메랄드', DIAMOND: '다이아몬드',
  MASTER: '마스터', GRANDMASTER: '그랜드마스터', CHALLENGER: '챌린저',
}

type GameTypeFilter = 'ALL' | GameType
const GAME_TYPE_FILTERS: GameTypeFilter[] = ['ALL', 'RANKED', 'NORMAL']
const REFRESH_COOLDOWN = 60
const REFRESH_LS_KEY = 'tft_last_refresh'

function getInitialCooldown(name: string, tag: string): number {
  try {
    const raw = localStorage.getItem(REFRESH_LS_KEY)
    if (!raw) return 0
    const { name: n, tag: t, at } = JSON.parse(raw)
    if (n !== name || t !== tag) return 0
    const elapsed = Math.floor((Date.now() - at) / 1000)
    return elapsed < REFRESH_COOLDOWN ? REFRESH_COOLDOWN - elapsed : 0
  } catch { return 0 }
}

/* ── 메인 ── */
function SummonerDetail() {
  const { gameName, tagLine } = useParams<{ gameName: string; tagLine: string }>()
  const name = decodeURIComponent(gameName ?? '')
  const tag = tagLine ?? 'KR1'
  const [query, setQuery] = useState('')
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [gameTypeFilter, setGameTypeFilter] = useState<GameTypeFilter>('ALL')
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [refreshRateLimitSeconds, setRefreshRateLimitSeconds] = useState(0)
  const [cooldownSeconds, setCooldownSeconds] = useState(() => getInitialCooldown(name, tag))
  const [refreshError, setRefreshError] = useState<string | null>(null)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const setSummoner = useSummonerStore((s) => s.setSummoner)

  const { data: profile, isError: profileIsError, error: profileErr, isLoading: profileLoading } = useSummonerProfile(name, tag)
  const profileRateLimited = profileIsError && (profileErr as Error)?.message === 'RATE_LIMITED'
  const profileNotFound = profileIsError && (profileErr as Error)?.message === 'NOT_FOUND'
  const profileServerError = profileIsError && !profileRateLimited && !profileNotFound
  const profileRetryAfter = profileRateLimited ? 120 : 0
  const isRateLimited = profileRateLimited || refreshRateLimitSeconds > 0
  const retryAfterSeconds = profileRateLimited ? profileRetryAfter : refreshRateLimitSeconds
  const {
    data: matchData,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isError: matchIsError,
    error: matchError,
  } = useMatchHistory(profile?.puuid ?? '')
  const matchRateLimited = matchIsError && (matchError as HttpError)?.response?.status === 429
  const matches = matchData?.pages.flat() ?? []

  const tierKo = TIER_KO[profile?.tier ?? ''] ?? profile?.tier ?? '-'
  const total = profile ? (profile.wins + profile.losses) : 0
  const winRate = total > 0 ? Math.round((profile!.wins / total) * 100) : 0

  const topTraits = (() => {
    const map = new Map<string, { traitId: string; name: string; iconUrl: string; tone: MatchTraitResponse['tone']; count: number; games: number; totalPlace: number }>()
    for (const m of matches) {
      const seen = new Set<string>()
      for (const tr of m.traits) {
        if (tr.traitId.toLowerCase().includes('unique')) continue
        if (seen.has(tr.traitId)) continue
        seen.add(tr.traitId)
        const entry = map.get(tr.traitId)
        if (entry) {
          entry.games++; entry.totalPlace += m.placement
          if (tr.count > entry.count) { entry.count = tr.count; entry.tone = tr.tone }
        }
        else map.set(tr.traitId, { traitId: tr.traitId, name: tr.name, iconUrl: tr.iconUrl, tone: tr.tone, count: tr.count, games: 1, totalPlace: m.placement })
      }
    }
    return [...map.values()]
      .sort((a, b) => b.games - a.games).slice(0, 5)
      .map(t => ({ ...t, avgPlace: Math.round(t.totalPlace / t.games * 10) / 10 }))
  })()

  const topChampions = (() => {
    const map = new Map<string, { characterId: string; name: string; imageUrl: string; games: number; totalPlace: number }>()
    for (const m of matches) {
      const seen = new Set<string>()
      for (const u of m.units) {
        if (seen.has(u.characterId)) continue
        seen.add(u.characterId)
        const entry = map.get(u.characterId)
        if (entry) { entry.games++; entry.totalPlace += m.placement }
        else map.set(u.characterId, {
          characterId: u.characterId,
          name: u.characterId.replace(/^TFT\d+_/i, ''),
          imageUrl: u.imageUrl || tftChampSquareUrl(u.characterId),
          games: 1,
          totalPlace: m.placement,
        })
      }
    }
    return [...map.values()]
      .sort((a, b) => b.games - a.games).slice(0, 5)
      .map(c => ({ ...c, avgPlace: Math.round(c.totalPlace / c.games * 10) / 10 }))
  })()

  const filteredMatches = gameTypeFilter === 'ALL'
    ? matches
    : matches.filter((m) => m.gameType === gameTypeFilter)

  useEffect(() => {
    if (refreshRateLimitSeconds <= 0 && cooldownSeconds <= 0) return
    const timer = setInterval(() => {
      setRefreshRateLimitSeconds((prev) => (prev <= 1 ? 0 : prev - 1))
      setCooldownSeconds((prev) => (prev <= 1 ? 0 : prev - 1))
    }, 1000)
    return () => clearInterval(timer)
  }, [refreshRateLimitSeconds > 0 || cooldownSeconds > 0])

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

  async function handleRefresh() {
    if (!name || !tag || isRefreshing) return
    setIsRefreshing(true)
    setRefreshError(null)
    try {
      await refreshSummoner(name, tag)
      await queryClient.invalidateQueries({ queryKey: ['summoner', 'profile', name, tag] })
      await queryClient.invalidateQueries({ queryKey: ['summoner', 'matches', profile?.puuid] })
      localStorage.setItem(REFRESH_LS_KEY, JSON.stringify({ name, tag, at: Date.now() }))
      setCooldownSeconds(REFRESH_COOLDOWN)
    } catch (err: unknown) {
      const status = (err as HttpError)?.response?.status
      if (status === 429) {
        const retryAfter = (err as HttpError)?.response?.headers?.['retry-after']
        setRefreshRateLimitSeconds(Math.max(1, Number(retryAfter ?? 120) || 120))
      } else if (status === 404) {
        setRefreshError('소환사 정보를 찾을 수 없습니다.')
      } else {
        setRefreshError('갱신 중 오류가 발생했습니다.')
      }
    } finally {
      setIsRefreshing(false)
    }
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = query.trim()
    if (!trimmed) return
    const [n = trimmed, tg = 'KR1'] = trimmed.split('#').map((s) => s.trim())
    navigate(`/summoner/${encodeURIComponent(n)}/${tg}`)
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

        {profileLoading ? (
          <ProfileSkeleton />
        ) : isRateLimited ? (
          <RateLimitState retryAfterSeconds={retryAfterSeconds} />
        ) : profileNotFound ? (
          <EmptyState name={name} tag={tag} />
        ) : profileServerError ? (
          <p className={styles.emptyState}>서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.</p>
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
                </p>
              </div>
              <div className={styles.profileRight}>
                <button
                  type="button"
                  className={styles.updateBtn}
                  onClick={handleRefresh}
                  disabled={isRefreshing || cooldownSeconds > 0}
                >
                  <RefreshCcw size={16} className={isRefreshing ? styles.spin : ''} />
                  {isRefreshing ? '갱신 중...' : cooldownSeconds > 0 ? `${cooldownSeconds}초 후 가능` : '갱신됨'}
                </button>
                {refreshError && <p className={styles.refreshError}>{refreshError}</p>}
              </div>
            </section>

            {/* 많이 플레이한 시너지 / 챔피언 */}
            {matches.length > 0 && (
              <div className={styles.statGrid}>
                <section className={styles.statSection}>
                  <h2 className={styles.statSectionTitle}>많이 플레이한 시너지</h2>
                  <div className={styles.topTraitList}>
                    {topTraits.map((tr, i) => (
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
                    {topChampions.map((champ, i) => (
                      <div key={champ.characterId} className={styles.topChampRow}>
                        <span className={styles.topRank}>{i + 1}</span>
                        <div className={styles.champThumbWrap}>
                          <img className={styles.champThumb} src={champ.imageUrl} alt={champ.name} />
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
              <h2>{filteredMatches.length > 0 ? `최근 ${filteredMatches.length}게임` : '매치 히스토리'}</h2>
              <RecentSummary matches={filteredMatches} />

              {/* 게임 유형 필터 */}
              {matches.length > 0 && (
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
              )}

              <div className={styles.matchList}>
                {filteredMatches.length === 0 ? (
                  <p className={styles.matchEmptyState}>
                    {matches.length === 0
                      ? '아직 플레이한 기록이 없습니다'
                      : '선택한 게임 유형의 전적이 없습니다'}
                  </p>
                ) : filteredMatches.map((match) => {
                  const isOpen = expandedId === match.matchId
                  return (
                    <div key={match.matchId} className={styles.matchItem}>
                      <article
                        className={`${styles.matchRow} ${placementTone(match.placement, styles)} ${isOpen ? styles.matchRowOpen : ''}`}
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
                          <p className={styles.timeAgo}>{formatDate(match.gameDateTime)} · {timeAgo(match.gameDateTime)}</p>
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
              {matchRateLimited && (
                <p className={styles.matchError}>
                  전적 갱신에 실패했습니다. 잠시 후 다시 시도해주세요.
                </p>
              )}
              {hasNextPage && (
                <button
                  type="button"
                  className={styles.loadMoreBtn}
                  onClick={() => fetchNextPage()}
                  disabled={isFetchingNextPage}
                >
                  {isFetchingNextPage ? '불러오는 중...' : '더 보기'}
                </button>
              )}
            </section>
          </>
        )}
      </div>
    </AppLayout>
  )
}

export default SummonerDetail
