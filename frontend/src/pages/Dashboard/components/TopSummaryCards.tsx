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

const QUICK_TAGS = ['정동글#KR1', '새벽의달#KR', '응의자#KR1', 'TFT잘하고싶다#1234']

function SummonerSearchCard() {
  const [query, setQuery] = useState('')
  const navigate = useNavigate()

  function handleSearch(input: string) {
    const trimmed = input.trim()
    if (!trimmed) return
    const [name = trimmed, tag = 'KR1'] = trimmed.split('#').map((s) => s.trim())
    navigate(`/summoner/${encodeURIComponent(name)}/${tag}`)
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
        <span>인기 검색</span>
        {QUICK_TAGS.map((tag) => (
          <button key={tag} type="button" onClick={() => handleSearch(tag)}>
            {tag}
          </button>
        ))}
      </div>
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
