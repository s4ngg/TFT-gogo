import { AlertTriangle, ChevronRight, Loader2, Search, UserRound } from 'lucide-react'
import { communityDragonProfileIconUrl } from '../../../api/communityDragonAssets'
import type { SummonerProfileResponse } from '../../../api/summonerApi'
import {
  formatSummonerTier,
  formatSummonerWinRate,
  profileToSummonerSearchTarget,
  type SummonerSearchTarget,
} from '../utils/summonerSearch'
import styles from '../Dashboard.module.css'

export type SummonerSearchResultStatus =
  | 'empty'
  | 'error'
  | 'idle'
  | 'loading'
  | 'notFound'
  | 'rateLimited'
  | 'success'

interface SummonerSearchResultCardProps {
  descriptionId?: string
  message?: string
  onOpenDetail: (target: SummonerSearchTarget) => void
  profile?: SummonerProfileResponse
  search: SummonerSearchTarget | null
  status: SummonerSearchResultStatus
}

function getStateCopy(status: Exclude<SummonerSearchResultStatus, 'success'>, message?: string) {
  if (status === 'loading') {
    return {
      description: 'Riot API 프록시로 프로필을 조회하고 있습니다.',
      title: '소환사 검색 중',
    }
  }

  if (status === 'empty') {
    return {
      description: message ?? '소환사명#태그 형식으로 다시 입력해주세요.',
      title: '검색어를 확인해주세요',
    }
  }

  if (status === 'notFound') {
    return {
      description: '입력한 소환사명과 태그를 다시 확인해주세요.',
      title: '소환사를 찾을 수 없습니다',
    }
  }

  if (status === 'rateLimited') {
    return {
      description: message ?? '잠시 후 다시 검색하면 결과를 확인할 수 있습니다.',
      title: '검색 요청이 많습니다',
    }
  }

  if (status === 'error') {
    return {
      description: '서버 응답을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.',
      title: '검색에 실패했습니다',
    }
  }

  return {
    description: '검색하면 결과 카드가 여기에 표시됩니다.',
    title: '홈에서 먼저 확인해보세요',
  }
}

function SummonerSearchResultCard({
  descriptionId,
  message,
  onOpenDetail,
  profile,
  search,
  status,
}: SummonerSearchResultCardProps) {
  if (status === 'success' && profile) {
    const target = profileToSummonerSearchTarget(profile)
    const tierLabel = formatSummonerTier(profile.tier, profile.rank)
    const winRate = formatSummonerWinRate(profile.wins, profile.losses)

    return (
      <article
        aria-label={`${target.normalized} 검색 결과`}
        aria-busy="false"
        className={`${styles.searchResultCard} ${styles.searchResultSuccess}`}
      >
        <img
          alt={`${profile.gameName} 프로필 아이콘`}
          className={styles.searchResultAvatar}
          src={communityDragonProfileIconUrl(profile.profileIconId)}
        />
        <div className={styles.searchResultContent}>
          <h2>
            {profile.gameName}
            <span>#{profile.tagLine}</span>
          </h2>
          <dl className={styles.searchResultStats}>
            <div>
              <dt>티어</dt>
              <dd>{tierLabel}</dd>
            </div>
            <div>
              <dt>LP</dt>
              <dd>{profile.leaguePoints} LP</dd>
            </div>
            <div>
              <dt>전적</dt>
              <dd>{profile.wins}승 {profile.losses}패</dd>
            </div>
            <div>
              <dt>승률</dt>
              <dd>{winRate}</dd>
            </div>
          </dl>
        </div>
        <button
          aria-label={`${target.normalized} 상세 전적 보기`}
          className={styles.searchResultCta}
          onClick={() => onOpenDetail(target)}
          type="button"
        >
          상세 보기
          <ChevronRight size={16} />
        </button>
      </article>
    )
  }

  const stateStatus = status === 'success' ? 'idle' : status
  const stateCopy = getStateCopy(stateStatus, message)
  const isErrorState = stateStatus === 'empty'
    || stateStatus === 'error'
    || stateStatus === 'notFound'
    || stateStatus === 'rateLimited'
  const Icon = stateStatus === 'loading'
    ? Loader2
    : isErrorState
      ? AlertTriangle
      : search
        ? UserRound
        : Search

  return (
    <div
      aria-busy={stateStatus === 'loading'}
      aria-live="polite"
      className={`${styles.searchResultCard} ${styles.searchResultState} ${
        isErrorState ? styles.searchResultError : ''
      }`}
      role="status"
    >
      <span className={styles.searchResultStateIcon} aria-hidden="true">
        <Icon className={stateStatus === 'loading' ? styles.searchResultSpin : undefined} size={19} />
      </span>
      <div className={styles.searchResultContent}>
        <h2>{stateCopy.title}</h2>
        <p id={descriptionId}>{stateCopy.description}</p>
      </div>
    </div>
  )
}

export default SummonerSearchResultCard
