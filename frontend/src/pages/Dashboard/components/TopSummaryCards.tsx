import { ClipboardList, Search } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from '../Dashboard.module.css'

function PatchMetaCard() {
  const navigate = useNavigate()

  return (
    <section className={`${styles.panel} ${styles.patchCard}`}>
      <div className={styles.patchEmblemArt} aria-hidden="true" />
      <div className={styles.patchCopy}>
        <h2>17.3 추천 메타</h2>
        <p>5월 20일 업데이트</p>
      </div>
      <button type="button" onClick={() => navigate('/patch-notes')}>
        <ClipboardList size={19} />
        패치 노트 보기
      </button>
    </section>
  )
}

const STORAGE_KEY = 'tft_recent_searches'

function getRecentSearches(): string[] {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
    if (!Array.isArray(parsed)) return []
    return parsed.filter((s): s is string => typeof s === 'string').slice(0, 5)
  } catch {
    return []
  }
}

function saveRecentSearch(input: string) {
  const prev = getRecentSearches()
  const next = [input, ...prev.filter((s) => s !== input)].slice(0, 5)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
}

function SummonerSearchCard() {
  const [query, setQuery] = useState('')
  const navigate = useNavigate()

  function handleSearch(input: string) {
    const trimmed = input.trim()
    if (!trimmed) return
    saveRecentSearch(trimmed)
    const parts = trimmed.split('#').map((s) => s.trim())
    const name = parts[0] || trimmed
    const tag = parts[1] || 'KR1'
    navigate(`/summoner/${encodeURIComponent(name)}/${encodeURIComponent(tag)}`)
  }

  const recentSearches = getRecentSearches()

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
              onClick={() => handleSearch(tag)}
            >
              {tag}
            </button>
          ))}
        </div>
      )}
    </section>
  )
}

function TopSummaryCards() {
  return (
    <>
      <PatchMetaCard />
      <SummonerSearchCard />
    </>
  )
}

export default TopSummaryCards
