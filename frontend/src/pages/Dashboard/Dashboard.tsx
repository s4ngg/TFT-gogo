import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppLayout } from '../../components/layout'
import { useSearchProfile } from '../../hooks/useSearchProfile'
import AiDeckRecommend from './components/AiDeckRecommend'
import LiveChat from './components/LiveChat'
import MetaSnapshot from './components/MetaSnapshot'
import PartyFinderCard from './components/PartyFinderCard'
import SummonerSearchResultCard, {
  type SummonerSearchResultStatus,
} from './components/SummonerSearchResultCard'
import TopSummaryCards from './components/TopSummaryCards'
import styles from './Dashboard.module.css'
import {
  formatSummonerDetailPath,
  getSummonerSearchRetryAfterSeconds,
  mapSummonerSearchError,
  parseSummonerSearchInput,
  profileToSummonerSearchTarget,
  readRecentSummonerSearches,
  saveRecentSummonerSearch,
  type SummonerSearchTarget,
} from './utils/summonerSearch'

function Dashboard() {
  const navigate = useNavigate()
  const [submittedSearch, setSubmittedSearch] = useState<SummonerSearchTarget | null>(null)
  const [searchInputMessage, setSearchInputMessage] = useState('')
  const [recentSearches, setRecentSearches] = useState<string[]>(() => readRecentSummonerSearches())
  const lastSavedSearchRef = useRef<string | null>(null)

  const profileQuery = useSearchProfile(submittedSearch?.gameName ?? '', submittedSearch?.tagLine ?? '')

  const searchStatus = useMemo<SummonerSearchResultStatus>(() => {
    if (searchInputMessage) {
      return 'empty'
    }

    if (!submittedSearch) {
      return 'idle'
    }

    if (profileQuery.isLoading || profileQuery.isFetching) {
      return 'loading'
    }

    if (profileQuery.isError) {
      return mapSummonerSearchError(profileQuery.error)
    }

    if (profileQuery.data) {
      return 'success'
    }

    return 'loading'
  }, [
    profileQuery.data,
    profileQuery.error,
    profileQuery.isError,
    profileQuery.isFetching,
    profileQuery.isLoading,
    searchInputMessage,
    submittedSearch,
  ])

  const searchResultMessage = useMemo(() => {
    if (searchStatus !== 'rateLimited') {
      return searchInputMessage
    }

    const retryAfterSeconds = getSummonerSearchRetryAfterSeconds(profileQuery.error) ?? 120

    return `${retryAfterSeconds}초 후 다시 검색할 수 있습니다.`
  }, [profileQuery.error, searchInputMessage, searchStatus])

  function handleSummonerSearchSubmit(input: string) {
    const parsedSearch = parseSummonerSearchInput(input)

    if (!parsedSearch.ok) {
      setSearchInputMessage(parsedSearch.message)
      setSubmittedSearch(null)
      return
    }

    setSearchInputMessage('')

    if (submittedSearch?.normalized === parsedSearch.value.normalized) {
      void profileQuery.refetch()
      return
    }

    setSubmittedSearch(parsedSearch.value)
  }

  function handleOpenSummonerDetail(target: SummonerSearchTarget) {
    navigate(formatSummonerDetailPath(target))
  }

  const searchDescriptionId = searchStatus === 'empty'
    ? 'dashboard-summoner-search-message'
    : undefined

  useEffect(() => {
    if (!profileQuery.data) {
      return
    }

    const searchTarget = profileToSummonerSearchTarget(profileQuery.data)

    if (lastSavedSearchRef.current === searchTarget.normalized) {
      return
    }

    lastSavedSearchRef.current = searchTarget.normalized
    setRecentSearches(saveRecentSummonerSearch(searchTarget.normalized))
  }, [profileQuery.data])

  return (
    <AppLayout sunTheme>
      <div className={styles.dashboardGrid}>
        <TopSummaryCards
          isSummonerSearchInvalid={searchStatus === 'empty'}
          onSummonerSearchSubmit={handleSummonerSearchSubmit}
          recentSearches={recentSearches}
          searchDescriptionId={searchDescriptionId}
          searchResult={(
            <SummonerSearchResultCard
              descriptionId={searchDescriptionId}
              message={searchResultMessage}
              onOpenDetail={handleOpenSummonerDetail}
              profile={profileQuery.data}
              search={submittedSearch}
              status={searchStatus}
            />
          )}
        />
        <MetaSnapshot />
        <aside className={styles.rightColumn}>
          <PartyFinderCard />
          <LiveChat />
          <AiDeckRecommend />
        </aside>
      </div>
    </AppLayout>
  )
}

export default Dashboard
