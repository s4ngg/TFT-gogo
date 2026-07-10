import { ClipboardList, Search } from 'lucide-react'
import type { ReactNode } from 'react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useLatestPatchNote } from '../../Decks/hooks/useLatestPatchVersion'
import styles from '../Dashboard.module.css'

function PatchMetaCard() {
  const navigate = useNavigate()
  const latestPatch = useLatestPatchNote()

  return (
    <section className={`${styles.panel} ${styles.patchCard}`}>
      <div className={styles.patchEmblemArt} aria-hidden="true" />
      <div className={styles.patchCopy}>
        <h2>{latestPatch ? `${latestPatch.version} 추천 메타` : '추천 메타'}</h2>
        <p>{latestPatch ? `${latestPatch.date} 업데이트` : '패치 데이터를 불러오는 중입니다'}</p>
      </div>
      <button type="button" onClick={() => navigate('/patch-notes')}>
        <ClipboardList size={19} />
        패치 노트 보기
      </button>
    </section>
  )
}

interface SummonerSearchCardProps {
  isSearchInvalid: boolean
  onSearchSubmit: (input: string) => void
  recentSearches: string[]
  result: ReactNode
  searchDescriptionId?: string
}

function SummonerSearchCard({
  isSearchInvalid,
  onSearchSubmit,
  recentSearches,
  result,
  searchDescriptionId,
}: SummonerSearchCardProps) {
  const [query, setQuery] = useState('')

  function handleSearch(input: string) {
    onSearchSubmit(input)
  }

  return (
    <section className={`${styles.panel} ${styles.searchPanel}`}>
      <h1>소환사 전적 검색</h1>
      <p>소환사명, 태그#KR 등을 입력하세요</p>
      <form
        className={styles.searchBox}
        onSubmit={(e) => {
          e.preventDefault()
          handleSearch(query)
        }}
      >
        <input
          aria-describedby={searchDescriptionId}
          aria-invalid={isSearchInvalid}
          aria-label="소환사명 검색"
          placeholder="소환사명#태그 입력"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit" aria-label="검색">
          <Search size={28} />
        </button>
      </form>
      {recentSearches.length > 0 && (
        <div className={styles.searchTags}>
          <span>최근 검색</span>
          {recentSearches.map((tag) => (
            <button
              key={tag}
              type="button"
              onClick={() => {
                setQuery(tag)
                handleSearch(tag)
              }}
            >
              {tag}
            </button>
          ))}
        </div>
      )}
      {result}
    </section>
  )
}

interface TopSummaryCardsProps {
  isSummonerSearchInvalid: boolean
  onSummonerSearchSubmit: (input: string) => void
  recentSearches: string[]
  searchDescriptionId?: string
  searchResult: ReactNode
}

function TopSummaryCards({
  isSummonerSearchInvalid,
  onSummonerSearchSubmit,
  recentSearches,
  searchDescriptionId,
  searchResult,
}: TopSummaryCardsProps) {
  return (
    <>
      <PatchMetaCard />
      <SummonerSearchCard
        isSearchInvalid={isSummonerSearchInvalid}
        onSearchSubmit={onSummonerSearchSubmit}
        recentSearches={recentSearches}
        result={searchResult}
        searchDescriptionId={searchDescriptionId}
      />
    </>
  )
}

export default TopSummaryCards
