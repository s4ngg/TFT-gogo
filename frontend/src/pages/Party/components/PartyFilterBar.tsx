import { type FormEvent } from 'react'
import { Search } from 'lucide-react'
import { partyFilters, type PartyFilter } from '../partyFilters'
import styles from '../Party.module.css'

interface PartyFilterBarProps {
  onFilterChange: (filter: PartyFilter) => void
  onSearchChange: (value: string) => void
  onSearchSubmit: () => void
  searchDraft: string
  selectedFilter: PartyFilter
}

function PartyFilterBar({
  onFilterChange,
  onSearchChange,
  onSearchSubmit,
  searchDraft,
  selectedFilter,
}: PartyFilterBarProps) {
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    onSearchSubmit()
  }

  return (
    <div className={styles.toolbar}>
      <form className={styles.searchBox} onSubmit={handleSubmit}>
        <Search size={18} />
        <input
          aria-label="파티 모집 검색"
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder="티어, 모드, 키워드 검색"
          value={searchDraft}
        />
        <button type="submit">검색</button>
      </form>
      <div className={styles.filterTabs} aria-label="파티원 찾기 필터">
        {partyFilters.map((filter) => (
          <button
            aria-pressed={selectedFilter === filter}
            className={selectedFilter === filter ? styles.selectedTab : undefined}
            key={filter}
            onClick={() => onFilterChange(filter)}
            type="button"
          >
            {filter}
          </button>
        ))}
      </div>
    </div>
  )
}

export default PartyFilterBar
