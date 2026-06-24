import { Filter, Search } from 'lucide-react'
import {
  PATCH_CATEGORIES,
  type PatchCategory,
  type PatchChangeStats,
} from '../../../api/patchNotes'
import styles from '../PatchNotes.module.css'

interface PatchChangeFiltersProps {
  activeCategory: PatchCategory
  query: string
  stats: PatchChangeStats
  onCategoryChange: (category: PatchCategory) => void
  onQueryChange: (query: string) => void
}

function getCategoryCount(category: PatchCategory, stats: PatchChangeStats) {
  return stats.categoryCounts[category] ?? 0
}

function PatchChangeFilters({
  activeCategory,
  onCategoryChange,
  onQueryChange,
  query,
  stats,
}: PatchChangeFiltersProps) {
  return (
    <>
      <div className={styles.toolBar}>
        <label className={styles.searchBox}>
          <Search size={16} />
          <input
            type="search"
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            placeholder="챔피언, 아이템, 키워드 검색"
          />
        </label>
        <div className={styles.filterLabel}>
          <Filter size={15} />
          카테고리
        </div>
      </div>

      <div className={styles.categoryTabs} aria-label="패치 변경 카테고리">
        {PATCH_CATEGORIES.map((category) => (
          <button
            key={category}
            type="button"
            className={activeCategory === category ? styles.activeTab : undefined}
            onClick={() => onCategoryChange(category)}
          >
            {category}
            <span>{getCategoryCount(category, stats)}</span>
          </button>
        ))}
      </div>
    </>
  )
}

export default PatchChangeFilters
